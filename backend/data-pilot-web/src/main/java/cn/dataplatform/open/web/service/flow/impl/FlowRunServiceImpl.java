package cn.dataplatform.open.web.service.flow.impl;

import cn.dataplatform.open.common.component.OrikaMapper;
import cn.dataplatform.open.common.exception.ApiException;
import cn.dataplatform.open.common.vo.base.PageBase;
import cn.dataplatform.open.common.vo.base.PageRequest;
import cn.dataplatform.open.common.vo.base.PageResult;
import cn.dataplatform.open.web.config.Context;
import cn.dataplatform.open.web.service.flow.FlowRunService;
import cn.dataplatform.open.web.service.task.TaskParamsHelper;
import cn.dataplatform.open.web.service.task.runner.TaskRunContext;
import cn.dataplatform.open.web.service.task.runner.TaskRunResult;
import cn.dataplatform.open.web.service.task.runner.TaskRunner;
import cn.dataplatform.open.web.service.task.runner.TaskRunnerFactory;
import cn.dataplatform.open.web.store.entity.DataFlow;
import cn.dataplatform.open.web.store.entity.FlowInstance;
import cn.dataplatform.open.web.store.entity.TaskInstance;
import cn.dataplatform.open.web.store.mapper.DataFlowMapper;
import cn.dataplatform.open.web.store.mapper.FlowInstanceMapper;
import cn.dataplatform.open.web.store.mapper.TaskInstanceMapper;
import cn.dataplatform.open.web.vo.data.flow.*;
import cn.dataplatform.open.web.vo.data.task.TaskInstanceDetailResponse;
import cn.dataplatform.open.web.vo.workspace.WorkspaceData;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * 任务流执行实现：Kahn 拓扑排序，按层级串行、层内并行执行节点
 *
 * @author dingqianwen
 */
@Slf4j
@Service
public class FlowRunServiceImpl implements FlowRunService {

    @Resource
    private DataFlowMapper dataFlowMapper;
    @Resource
    private FlowInstanceMapper flowInstanceMapper;
    @Resource
    private TaskInstanceMapper taskInstanceMapper;
    @Resource
    private TaskRunnerFactory taskRunnerFactory;
    @Resource
    private TaskParamsHelper taskParamsHelper;
    @Resource
    private OrikaMapper orikaMapper;
    @Resource(name = "dpTaskExecutor")
    private ThreadPoolTaskExecutor dpTaskExecutor;

    @Override
    public Long run(Long flowId, String triggerType, String failureStrategy) {
        DataFlow flow = this.dataFlowMapper.selectById(flowId);
        if (flow == null) {
            throw new ApiException("任务流不存在");
        }
        TaskFlowDesign design = JSON.parseObject(flow.getDesign(), TaskFlowDesign.class);
        if (design == null || CollUtil.isEmpty(design.getNodes())) {
            throw new ApiException("任务流节点不能为空");
        }
        FlowInstance instance = new FlowInstance();
        instance.setFlowId(flow.getId());
        instance.setFlowCode(flow.getCode());
        instance.setWorkspaceCode(flow.getWorkspaceCode());
        instance.setTriggerType(triggerType);
        instance.setFailureStrategy(StrUtil.isBlank(failureStrategy) ? "CONTINUE" : failureStrategy);
        instance.setStatus("RUNNING");
        instance.setStartTime(LocalDateTime.now());
        instance.setCreateTime(LocalDateTime.now());
        this.flowInstanceMapper.insert(instance);

        Long instanceId = instance.getId();
        this.dpTaskExecutor.submit(() -> this.execute(instanceId));
        return instanceId;
    }

    private void execute(Long flowInstanceId) {
        FlowInstance flowInstance = this.flowInstanceMapper.selectById(flowInstanceId);
        long start = System.currentTimeMillis();
        String finalStatus = "SUCCESS";
        String errorMsg = null;
        try {
            DataFlow flow = this.dataFlowMapper.selectById(flowInstance.getFlowId());
            TaskFlowDesign design = JSON.parseObject(flow.getDesign(), TaskFlowDesign.class);
            List<List<TaskFlowDesign.FlowNode>> levels = this.taskParamsHelper.topoSort(design);
            String failureStrategy = flowInstance.getFailureStrategy();
            boolean failed = false;
            for (List<TaskFlowDesign.FlowNode> level : levels) {
                if (failed) {
                    for (TaskFlowDesign.FlowNode node : level) {
                        this.markSkip(flowInstance, node, flow.getWorkspaceCode());
                    }
                    continue;
                }
                failed = this.runLevel(flowInstance, level, flow.getWorkspaceCode(), failureStrategy);
            }
            if (failed) {
                finalStatus = "FAIL";
                errorMsg = "存在执行失败的节点";
            }
        } catch (Exception e) {
            log.error("任务流执行失败, flowInstanceId:{}", flowInstanceId, e);
            finalStatus = "FAIL";
            errorMsg = this.rootMessage(e);
        } finally {
            FlowInstance update = new FlowInstance();
            update.setId(flowInstance.getId());
            update.setStatus(finalStatus);
            update.setErrorMsg(errorMsg);
            update.setDurationMs(System.currentTimeMillis() - start);
            update.setEndTime(LocalDateTime.now());
            this.flowInstanceMapper.updateById(update);
        }
    }

    /**
     * 执行一层节点（层内并行），返回该层是否存在失败节点。
     * 失败策略 END 时，一旦某节点失败即终止其余仍在执行的节点。
     */
    private boolean runLevel(FlowInstance flowInstance, List<TaskFlowDesign.FlowNode> level, String workspaceCode, String failureStrategy) {
        ExecutorService pool = Executors.newFixedThreadPool(Math.min(Math.max(level.size(), 1), 8));
        boolean end = "END".equalsIgnoreCase(failureStrategy);
        try {
            if (!end) {
                List<Future<String>> futures = new ArrayList<>();
                for (TaskFlowDesign.FlowNode node : level) {
                    futures.add(pool.submit(() -> this.runNode(flowInstance, node, workspaceCode)));
                }
                boolean failed = false;
                for (Future<String> future : futures) {
                    try {
                        if ("FAIL".equals(future.get())) {
                            failed = true;
                        }
                    } catch (Exception e) {
                        log.error("节点执行异常", e);
                        failed = true;
                    }
                }
                return failed;
            }

            // END：按完成顺序处理，一旦失败即终止其余节点
            ExecutorCompletionService<String> ecs = new ExecutorCompletionService<>(pool);
            List<Future<String>> futures = new ArrayList<>();
            for (TaskFlowDesign.FlowNode node : level) {
                futures.add(ecs.submit(() -> this.runNode(flowInstance, node, workspaceCode)));
            }
            boolean failed = false;
            for (int i = 0; i < futures.size(); i++) {
                try {
                    Future<String> done = ecs.take();
                    if ("FAIL".equals(done.get())) {
                        failed = true;
                    }
                } catch (Exception e) {
                    log.error("节点执行异常", e);
                    failed = true;
                }
                if (failed) {
                    this.cancelRemaining(futures);
                    break;
                }
            }
            return failed;
        } finally {
            pool.shutdownNow();
        }
    }

    private void cancelRemaining(List<Future<String>> futures) {
        for (Future<String> future : futures) {
            future.cancel(true);
        }
    }

    /**
     * 执行单个节点并回写 task_instance，返回 SUCCESS/FAIL
     */
    private String runNode(FlowInstance flowInstance, TaskFlowDesign.FlowNode node, String workspaceCode) {
        TaskFlowNode props = node.getProperties() == null ? new TaskFlowNode() : node.getProperties();
        TaskInstance instance = new TaskInstance();
        instance.setFlowInstanceId(flowInstance.getId());
        instance.setNodeId(node.getId());
        instance.setNodeName(props.getName());
        instance.setWorkspaceCode(workspaceCode);
        instance.setTaskType(node.getType());
        instance.setTriggerType(flowInstance.getTriggerType());
        instance.setStatus("RUNNING");
        instance.setTaskParams(this.taskParamsHelper.serialize(node.getType(), props));
        instance.setLogPath("/tmp/dp-sync/" + UUID.randomUUID().toString().replace("-", "") + ".log");
        instance.setStartTime(LocalDateTime.now());
        instance.setCreateTime(LocalDateTime.now());
        this.taskInstanceMapper.insert(instance);

        long start = System.currentTimeMillis();
        TaskInstance update = new TaskInstance();
        update.setId(instance.getId());
        String status;
        try {
            TaskRunContext context = new TaskRunContext();
            context.setTaskType(node.getType());
            context.setTaskParams(instance.getTaskParams());
            context.setTimeout(props.getTimeout() == null ? 30 : props.getTimeout());
            context.setWorkspaceCode(workspaceCode);
            context.setLogPath(instance.getLogPath());
            TaskRunner runner = this.taskRunnerFactory.get(node.getType());
            TaskRunResult result = runner.run(context);
            update.setStatus(result.getStatus());
            update.setLogContent(result.getLogContent());
            update.setErrorMsg(result.getErrorMsg());
            update.setRowCount(result.getRowCount());
            update.setResult(this.buildResultJson(result));
            status = result.getStatus();
        } catch (Exception e) {
            if (Thread.currentThread().isInterrupted()) {
                update.setStatus("SKIP");
                update.setErrorMsg("任务被终止");
                status = "SKIP";
            } else {
                log.error("节点执行失败, flowInstanceId:{}, nodeId:{}", flowInstance.getId(), node.getId(), e);
                update.setStatus("FAIL");
                update.setErrorMsg(this.rootMessage(e));
                status = "FAIL";
            }
        } finally {
            update.setDurationMs(System.currentTimeMillis() - start);
            update.setEndTime(LocalDateTime.now());
            this.taskInstanceMapper.updateById(update);
        }
        return status;
    }

    private void markSkip(FlowInstance flowInstance, TaskFlowDesign.FlowNode node, String workspaceCode) {
        TaskFlowNode props = node.getProperties() == null ? new TaskFlowNode() : node.getProperties();
        TaskInstance instance = new TaskInstance();
        instance.setFlowInstanceId(flowInstance.getId());
        instance.setNodeId(node.getId());
        instance.setNodeName(props.getName());
        instance.setWorkspaceCode(workspaceCode);
        instance.setTaskType(node.getType());
        instance.setTriggerType(flowInstance.getTriggerType());
        instance.setStatus("SKIP");
        instance.setTaskParams(this.taskParamsHelper.serialize(node.getType(), props));
        instance.setStartTime(LocalDateTime.now());
        instance.setEndTime(LocalDateTime.now());
        instance.setCreateTime(LocalDateTime.now());
        this.taskInstanceMapper.insert(instance);
    }

    @Override
    public PageResult<FlowInstanceListResponse> instanceList(PageRequest<FlowInstanceListRequest> pageRequest) {
        WorkspaceData workspace = Context.getWorkspace();
        PageBase page = pageRequest.getPage();
        FlowInstanceListRequest query = Optional.ofNullable(pageRequest.getQuery()).orElse(new FlowInstanceListRequest());

        List<Long> keywordFlowIds = null;
        if (StrUtil.isNotBlank(query.getKeyword())) {
            keywordFlowIds = this.dataFlowMapper.selectList(new LambdaQueryWrapper<DataFlow>()
                            .select(DataFlow::getId)
                            .and(q -> q.like(DataFlow::getName, query.getKeyword())
                                    .or()
                                    .like(DataFlow::getCode, query.getKeyword()))
                            .eq(DataFlow::getWorkspaceCode, workspace.getCode()))
                    .stream().map(DataFlow::getId).collect(Collectors.toList());
            if (keywordFlowIds.isEmpty()) {
                PageResult<FlowInstanceListResponse> empty = new PageResult<>();
                empty.setData(CollUtil.newArrayList(), page.getCurrent(), page.getSize(), 0L);
                return empty;
            }
        }

        Page<FlowInstance> instancePage = this.flowInstanceMapper.selectPage(
                new Page<>(page.getCurrent(), page.getSize()),
                new LambdaQueryWrapper<FlowInstance>()
                        .eq(query.getFlowId() != null, FlowInstance::getFlowId, query.getFlowId())
                        .in(keywordFlowIds != null, FlowInstance::getFlowId, keywordFlowIds)
                        .eq(StrUtil.isNotBlank(query.getStatus()), FlowInstance::getStatus, query.getStatus())
                        .eq(StrUtil.isNotBlank(query.getTriggerType()), FlowInstance::getTriggerType, query.getTriggerType())
                        .eq(FlowInstance::getWorkspaceCode, workspace.getCode())
                        .orderByDesc(FlowInstance::getId));
        PageResult<FlowInstanceListResponse> pageResult = new PageResult<>();
        List<FlowInstance> records = instancePage.getRecords();
        if (CollUtil.isEmpty(records)) {
            pageResult.setData(CollUtil.newArrayList(), page.getCurrent(), page.getSize(), instancePage.getTotal());
            return pageResult;
        }
        Set<Long> flowIds = records.stream().map(FlowInstance::getFlowId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> flowNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(flowIds)) {
            this.dataFlowMapper.selectList(new LambdaQueryWrapper<DataFlow>()
                            .select(DataFlow::getId, DataFlow::getName)
                            .in(DataFlow::getId, flowIds))
                    .forEach(f -> flowNameMap.put(f.getId(), f.getName()));
        }
        List<FlowInstanceListResponse> collect = records.stream().map(m -> {
            FlowInstanceListResponse response = new FlowInstanceListResponse();
            this.orikaMapper.map(m, response);
            response.setFlowName(flowNameMap.get(m.getFlowId()));
            return response;
        }).collect(Collectors.toList());
        pageResult.setData(collect, instancePage.getCurrent(), instancePage.getSize(), instancePage.getTotal());
        return pageResult;
    }

    @Override
    public FlowInstanceDetailResponse instanceDetail(Long id) {
        FlowInstance flowInstance = this.flowInstanceMapper.selectById(id);
        if (flowInstance == null) {
            return null;
        }
        FlowInstanceDetailResponse response = new FlowInstanceDetailResponse();
        this.orikaMapper.map(flowInstance, response);
        DataFlow flow = this.dataFlowMapper.selectById(flowInstance.getFlowId());
        if (flow != null) {
            response.setFlowName(flow.getName());
        }
        List<TaskInstance> nodes = this.taskInstanceMapper.selectList(new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getFlowInstanceId, id)
                .orderByAsc(TaskInstance::getId));
        response.setNodes(nodes.stream().map(this::toNodeDetail).collect(Collectors.toList()));
        return response;
    }

    private TaskInstanceDetailResponse toNodeDetail(TaskInstance instance) {
        TaskInstanceDetailResponse response = new TaskInstanceDetailResponse();
        this.orikaMapper.map(instance, response);
        // 运行中/结束后实时读取子进程日志文件（覆盖数据库里的最终 logContent）
        if (StrUtil.isNotBlank(instance.getLogPath())) {
            Path logPath = Paths.get(instance.getLogPath());
            if (Files.exists(logPath)) {
                try {
                    response.setLogContent(Files.readString(logPath, StandardCharsets.UTF_8));
                } catch (IOException e) {
                    // 读取失败则保留数据库中的 logContent
                }
            }
        }
        if (StrUtil.isNotBlank(instance.getResult())) {
            JSONObject obj = JSON.parseObject(instance.getResult());
            response.setColumns(obj.getList("columns", String.class));
            List<List<String>> rows = new ArrayList<>();
            JSONArray rowsArr = obj.getJSONArray("rows");
            if (rowsArr != null) {
                for (Object rowObj : rowsArr) {
                    JSONArray rowArr = (JSONArray) rowObj;
                    List<String> row = new ArrayList<>();
                    for (Object cell : rowArr) {
                        row.add(cell == null ? null : cell.toString());
                    }
                    rows.add(row);
                }
            }
            response.setRows(rows);
            response.setTruncated(obj.getBoolean("truncated"));
        }
        return response;
    }

    private String buildResultJson(TaskRunResult result) {
        if (result.getColumns() == null) {
            return null;
        }
        Map<String, Object> preview = new HashMap<>();
        preview.put("columns", result.getColumns());
        preview.put("rows", result.getRows());
        preview.put("truncated", result.isTruncated());
        return JSON.toJSONString(preview);
    }

    private String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
