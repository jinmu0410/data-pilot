package cn.datapilot.web.service.task;

import cn.datapilot.common.enums.Status;
import cn.datapilot.common.exception.ApiException;
import cn.datapilot.common.util.CronUtils;
import cn.datapilot.web.service.datasource.DataSourceService;
import cn.datapilot.web.service.task.model.ScriptTaskParams;
import cn.datapilot.web.service.task.model.SqlTaskParams;
import cn.datapilot.web.service.task.model.SyncTaskParams;
import cn.datapilot.web.store.entity.DataSource;
import cn.datapilot.web.vo.data.flow.TaskFlowDesign;
import cn.datapilot.web.vo.data.flow.TaskFlowNode;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 任务流节点参数与 DAG 公共逻辑（从 TaskServiceImpl 抽离，供任务流复用）
 *
 * @author jinmu
 */
@Component
public class TaskParamsHelper {

    @Resource
    private DataSourceService dataSourceService;

    /**
     * 按任务类型序列化节点参数为 JSON
     */
    public String serialize(String taskType, TaskFlowNode node) {
        TaskType type = TaskType.of(taskType);
        return switch (type) {
            case SQL -> {
                SqlTaskParams p = new SqlTaskParams();
                p.setDatasourceCode(node.getDatasourceCode());
                p.setSqlType(node.getSqlType());
                p.setSqlText(node.getSqlText());
                p.setPreSql(node.getPreSql());
                p.setPostSql(node.getPostSql());
                p.setSqlParams(node.getSqlParams());
                yield JSON.toJSONString(p);
            }
            case DATAX -> {
                SyncTaskParams p = new SyncTaskParams();
                p.setSourceDataSourceCode(node.getSourceDataSourceCode());
                p.setSourceSchema(node.getSourceSchema());
                p.setSourceTable(node.getSourceTable());
                p.setSqlText(node.getSqlText());
                p.setTargetDataSourceCode(node.getTargetDataSourceCode());
                p.setTargetSchema(node.getTargetSchema());
                p.setTargetTable(node.getTargetTable());
                p.setFieldMapping(node.getFieldMapping());
                p.setPreSql(node.getPreSql());
                p.setPostSql(node.getPostSql());
                p.setJobSpeedByte(node.getJobSpeedByte());
                p.setJobSpeedRecord(node.getJobSpeedRecord());
                p.setFetchSize(node.getFetchSize());
                p.setWriteMode(node.getWriteMode());
                p.setBatchSize(node.getBatchSize());
                p.setChannel(node.getChannel());
                yield JSON.toJSONString(p);
            }
            case SEATUNNEL -> {
                SyncTaskParams p = new SyncTaskParams();
                p.setSourceDataSourceCode(node.getSourceDataSourceCode());
                p.setSourceSchema(node.getSourceSchema());
                p.setSourceTable(node.getSourceTable());
                p.setTargetDataSourceCode(node.getTargetDataSourceCode());
                p.setTargetSchema(node.getTargetSchema());
                p.setTargetTable(node.getTargetTable());
                p.setFieldMapping(node.getFieldMapping());
                yield JSON.toJSONString(p);
            }
            case PYTHON, SHELL -> {
                ScriptTaskParams p = new ScriptTaskParams();
                p.setScript(node.getScript());
                yield JSON.toJSONString(p);
            }
        };
    }

    /**
     * 校验单个节点参数
     */
    public void validate(String taskType, TaskFlowNode node, String workspaceCode) {
        if (node == null) {
            throw new ApiException("节点参数不能为空");
        }
        TaskType type = TaskType.of(taskType);
        switch (type) {
            case SQL -> {
                if (StrUtil.isBlank(node.getDatasourceCode())) {
                    throw new ApiException("请选择数据源");
                }
                if (StrUtil.isBlank(node.getSqlText())) {
                    throw new ApiException("SQL 不能为空");
                }
                this.resolveDataSource(node.getDatasourceCode(), workspaceCode);
            }
            case DATAX -> {
                if (StrUtil.isBlank(node.getSourceDataSourceCode())) {
                    throw new ApiException("请选择数据源");
                }
                if (StrUtil.isBlank(node.getSqlText())) {
                    throw new ApiException("SQL 语句不能为空");
                }
                if (StrUtil.isBlank(node.getTargetDataSourceCode())) {
                    throw new ApiException("请选择目标库");
                }
                if (StrUtil.isBlank(node.getTargetTable())) {
                    throw new ApiException("目标表不能为空");
                }
                this.resolveDataSource(node.getSourceDataSourceCode(), workspaceCode);
                this.resolveDataSource(node.getTargetDataSourceCode(), workspaceCode);
            }
            case SEATUNNEL -> {
                if (StrUtil.isBlank(node.getSourceDataSourceCode())) {
                    throw new ApiException("请选择源数据源");
                }
                if (StrUtil.isBlank(node.getTargetDataSourceCode())) {
                    throw new ApiException("请选择目标数据源");
                }
                if (StrUtil.isBlank(node.getSourceTable())) {
                    throw new ApiException("源表不能为空");
                }
                if (StrUtil.isBlank(node.getTargetTable())) {
                    throw new ApiException("目标表不能为空");
                }
                this.resolveDataSource(node.getSourceDataSourceCode(), workspaceCode);
                this.resolveDataSource(node.getTargetDataSourceCode(), workspaceCode);
            }
            case PYTHON, SHELL -> {
                if (StrUtil.isBlank(node.getScript())) {
                    throw new ApiException("脚本内容不能为空");
                }
            }
        }
    }

    public void validateCron(String cron) {
        if (StrUtil.isNotBlank(cron) && !CronUtils.isValid(cron)) {
            throw new ApiException("cron 表达式不合法");
        }
    }

    public DataSource resolveDataSource(String datasourceCode, String workspaceCode) {
        DataSource dataSource = this.dataSourceService.lambdaQuery()
                .eq(DataSource::getCode, datasourceCode)
                .eq(DataSource::getWorkspaceCode, workspaceCode)
                .one();
        if (dataSource == null) {
            throw new ApiException("数据源不存在: " + datasourceCode);
        }
        if (!Objects.equals(dataSource.getStatus(), Status.ENABLE.name())) {
            throw new ApiException("数据源非启用状态: " + datasourceCode);
        }
        return dataSource;
    }

    /**
     * Kahn 拓扑排序，按层级返回节点；存在环则抛异常
     */
    public List<List<TaskFlowDesign.FlowNode>> topoSort(TaskFlowDesign design) {
        List<TaskFlowDesign.FlowNode> nodes = design.getNodes();
        if (CollUtil.isEmpty(nodes)) {
            return Collections.emptyList();
        }
        Map<String, TaskFlowDesign.FlowNode> nodeMap = nodes.stream()
                .filter(n -> StrUtil.isNotBlank(n.getId()))
                .collect(Collectors.toMap(TaskFlowDesign.FlowNode::getId, Function.identity(), (a, b) -> a));
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> adjacency = new HashMap<>();
        for (TaskFlowDesign.FlowNode node : nodes) {
            if (StrUtil.isBlank(node.getId())) {
                continue;
            }
            indegree.putIfAbsent(node.getId(), 0);
            adjacency.putIfAbsent(node.getId(), new ArrayList<>());
        }
        if (CollUtil.isNotEmpty(design.getEdges())) {
            for (TaskFlowDesign.FlowEdge edge : design.getEdges()) {
                if (StrUtil.isBlank(edge.getSourceNodeId()) || StrUtil.isBlank(edge.getTargetNodeId())) {
                    continue;
                }
                if (!nodeMap.containsKey(edge.getSourceNodeId()) || !nodeMap.containsKey(edge.getTargetNodeId())) {
                    continue;
                }
                adjacency.get(edge.getSourceNodeId()).add(edge.getTargetNodeId());
                indegree.merge(edge.getTargetNodeId(), 1, Integer::sum);
            }
        }
        Queue<String> queue = new ArrayDeque<>();
        indegree.forEach((id, degree) -> {
            if (degree == 0) {
                queue.add(id);
            }
        });
        List<List<TaskFlowDesign.FlowNode>> levels = new ArrayList<>();
        int visited = 0;
        while (!queue.isEmpty()) {
            List<TaskFlowDesign.FlowNode> level = new ArrayList<>();
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                String id = queue.poll();
                visited++;
                TaskFlowDesign.FlowNode node = nodeMap.get(id);
                if (node != null) {
                    level.add(node);
                }
                for (String next : adjacency.getOrDefault(id, Collections.emptyList())) {
                    int degree = indegree.merge(next, -1, Integer::sum);
                    if (degree == 0) {
                        queue.add(next);
                    }
                }
            }
            if (!level.isEmpty()) {
                levels.add(level);
            }
        }
        if (visited != nodeMap.size()) {
            throw new ApiException("任务流存在环");
        }
        return levels;
    }
}
