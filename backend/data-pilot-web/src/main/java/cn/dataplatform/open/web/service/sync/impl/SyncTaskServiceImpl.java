package cn.dataplatform.open.web.service.sync.impl;

import cn.dataplatform.open.common.component.OrikaMapper;
import cn.dataplatform.open.common.enums.Status;
import cn.dataplatform.open.common.exception.ApiException;
import cn.dataplatform.open.common.vo.base.PageBase;
import cn.dataplatform.open.common.vo.base.PageRequest;
import cn.dataplatform.open.common.vo.base.PageResult;
import cn.dataplatform.open.web.config.Context;
import cn.dataplatform.open.web.service.PasswordEncAndDecService;
import cn.dataplatform.open.web.service.datasource.DataSourceService;
import cn.dataplatform.open.web.service.sync.SyncEngineExecutor;
import cn.dataplatform.open.web.service.sync.SyncTaskService;
import cn.dataplatform.open.web.service.sync.engine.SyncEngine;
import cn.dataplatform.open.web.service.sync.engine.SyncEngineContext;
import cn.dataplatform.open.web.service.sync.engine.SyncEngineFactory;
import cn.dataplatform.open.web.store.entity.DataSource;
import cn.dataplatform.open.web.store.entity.SyncTask;
import cn.dataplatform.open.web.store.entity.SyncTaskLog;
import cn.dataplatform.open.web.store.mapper.SyncTaskLogMapper;
import cn.dataplatform.open.web.store.mapper.SyncTaskMapper;
import cn.dataplatform.open.web.vo.data.sync.*;
import cn.dataplatform.open.web.vo.workspace.WorkspaceData;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据集成-同步任务实现
 *
 * @author jinmu
 */
@Slf4j
@Service
public class SyncTaskServiceImpl extends ServiceImpl<SyncTaskMapper, SyncTask> implements SyncTaskService {

    @Resource
    private OrikaMapper orikaMapper;
    @Resource
    private DataSourceService dataSourceService;
    @Resource
    private PasswordEncAndDecService passwordEncAndDecService;
    @Resource
    private SyncEngineFactory syncEngineFactory;
    @Resource
    private SyncEngineExecutor syncEngineExecutor;
    @Resource
    private SyncTaskLogMapper syncTaskLogMapper;
    @Resource(name = "syncTaskExecutor")
    private ThreadPoolTaskExecutor syncTaskExecutor;

    @Override
    public PageResult<SyncTaskListResponse> list(PageRequest<SyncTaskListRequest> pageRequest) {
        WorkspaceData workspace = Context.getWorkspace();
        PageBase page = pageRequest.getPage();
        SyncTaskListRequest query = Optional.ofNullable(pageRequest.getQuery()).orElse(new SyncTaskListRequest());
        Page<SyncTask> taskPage = this.lambdaQuery()
                .and(StrUtil.isNotBlank(query.getKeyword()), q -> q
                        .like(SyncTask::getName, query.getKeyword())
                        .or()
                        .like(SyncTask::getCode, query.getKeyword()))
                .eq(StrUtil.isNotBlank(query.getEngine()), SyncTask::getEngine, query.getEngine())
                .eq(StrUtil.isNotBlank(query.getStatus()), SyncTask::getStatus, query.getStatus())
                .eq(SyncTask::getWorkspaceCode, workspace.getCode())
                .orderByDesc(SyncTask::getUpdateTime)
                .page(new Page<>(page.getCurrent(), page.getSize()));
        PageResult<SyncTaskListResponse> pageResult = new PageResult<>();
        List<SyncTask> records = taskPage.getRecords();
        if (CollUtil.isEmpty(records)) {
            pageResult.setData(CollUtil.newArrayList(), page.getCurrent(), page.getSize(), 0L);
            return pageResult;
        }
        Set<String> dsCodes = new HashSet<>();
        records.forEach(t -> {
            dsCodes.add(t.getSourceDataSourceCode());
            dsCodes.add(t.getTargetDataSourceCode());
        });
        Map<String, String> dsNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(dsCodes)) {
            this.dataSourceService.lambdaQuery()
                    .select(DataSource::getCode, DataSource::getName)
                    .eq(DataSource::getWorkspaceCode, workspace.getCode())
                    .in(DataSource::getCode, dsCodes)
                    .list()
                    .forEach(ds -> dsNameMap.put(ds.getCode(), ds.getName()));
        }
        List<SyncTaskListResponse> collect = records.stream().map(m -> {
            SyncTaskListResponse response = new SyncTaskListResponse();
            this.orikaMapper.map(m, response);
            response.setSourceDataSourceName(dsNameMap.get(m.getSourceDataSourceCode()));
            response.setTargetDataSourceName(dsNameMap.get(m.getTargetDataSourceCode()));
            return response;
        }).collect(Collectors.toList());
        pageResult.setData(collect, taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        return pageResult;
    }

    @Override
    public Long add(SyncTaskAddRequest request) {
        WorkspaceData workspace = Context.getWorkspace();
        if (this.lambdaQuery().eq(SyncTask::getName, request.getName())
                .eq(SyncTask::getWorkspaceCode, workspace.getCode())
                .exists()) {
            throw new ApiException("同步任务名称已存在");
        }
        this.validateEngine(request.getEngine());
        this.resolveDataSource(request.getSourceDataSourceCode(), workspace.getCode());
        this.resolveDataSource(request.getTargetDataSourceCode(), workspace.getCode());

        SyncTask task = new SyncTask();
        this.orikaMapper.map(request, task);
        task.setCode(UUID.fastUUID().toString(true));
        task.setWorkspaceCode(workspace.getCode());
        task.setCreateUserId(Context.getUser().getId());
        task.setFieldMapping(this.serializeFieldMapping(request.getFieldMapping()));
        if (StrUtil.isBlank(task.getStatus())) {
            task.setStatus(Status.ENABLE.name());
        }
        this.save(task);
        return task.getId();
    }

    @Override
    public Boolean update(SyncTaskUpdateRequest request) {
        WorkspaceData workspace = Context.getWorkspace();
        if (this.lambdaQuery().eq(SyncTask::getName, request.getName())
                .ne(SyncTask::getId, request.getId())
                .eq(SyncTask::getWorkspaceCode, workspace.getCode())
                .exists()) {
            throw new ApiException("同步任务名称已存在");
        }
        SyncTask task = this.getById(request.getId());
        if (task == null) {
            throw new ApiException("同步任务不存在");
        }
        this.validateEngine(request.getEngine());
        this.resolveDataSource(request.getSourceDataSourceCode(), workspace.getCode());
        this.resolveDataSource(request.getTargetDataSourceCode(), workspace.getCode());

        this.orikaMapper.map(request, task);
        task.setFieldMapping(this.serializeFieldMapping(request.getFieldMapping()));
        this.updateById(task);
        return true;
    }

    @Override
    public SyncTaskDetailResponse detail(Long id) {
        SyncTask task = this.getById(id);
        if (task == null) {
            return null;
        }
        SyncTaskDetailResponse response = new SyncTaskDetailResponse();
        String fieldMapping = task.getFieldMapping();
        // 避免 Orika 把 String 映射到 List<SyncFieldMapping> 产生类型冲突
        task.setFieldMapping(null);
        this.orikaMapper.map(task, response);
        if (StrUtil.isNotBlank(fieldMapping)) {
            response.setFieldMapping(JSON.parseArray(fieldMapping, SyncFieldMapping.class));
        }
        return response;
    }

    @Override
    public Boolean delete(Long id) {
        SyncTask task = this.getById(id);
        if (task == null) {
            return false;
        }
        this.removeById(id);
        return true;
    }

    @Override
    public SyncConfigResponse generateConfig(Long id) {
        SyncTask task = this.getById(id);
        if (task == null) {
            throw new ApiException("同步任务不存在");
        }
        return this.buildConfigResponse(task);
    }

    @Override
    public Long run(Long id) {
        SyncTask task = this.getById(id);
        if (task == null) {
            throw new ApiException("同步任务不存在");
        }
        SyncEngine engine = this.syncEngineFactory.get(task.getEngine());
        SyncEngineContext ctx = this.buildContext(task);
        String config = engine.buildConfig(ctx);

        SyncTaskLog taskLog = new SyncTaskLog();
        taskLog.setTaskId(task.getId());
        taskLog.setTaskCode(task.getCode());
        taskLog.setWorkspaceCode(task.getWorkspaceCode());
        taskLog.setEngine(task.getEngine());
        taskLog.setTriggerType("MANUAL");
        taskLog.setStatus("RUNNING");
        taskLog.setConfigContent(config);
        taskLog.setStartTime(LocalDateTime.now());
        taskLog.setCreateTime(LocalDateTime.now());
        this.syncTaskLogMapper.insert(taskLog);

        Long logId = taskLog.getId();
        this.syncTaskExecutor.submit(() -> this.executeAsync(logId, engine, config));
        return logId;
    }

    @Override
    public PageResult<SyncTaskLogListResponse> logList(PageRequest<SyncTaskLogListRequest> pageRequest) {
        WorkspaceData workspace = Context.getWorkspace();
        PageBase page = pageRequest.getPage();
        SyncTaskLogListRequest query = Optional.ofNullable(pageRequest.getQuery()).orElse(new SyncTaskLogListRequest());
        Page<SyncTaskLog> logPage = this.syncTaskLogMapper.selectPage(
                new Page<>(page.getCurrent(), page.getSize()),
                new LambdaQueryWrapper<SyncTaskLog>()
                        .eq(query.getTaskId() != null, SyncTaskLog::getTaskId, query.getTaskId())
                        .eq(StrUtil.isNotBlank(query.getEngine()), SyncTaskLog::getEngine, query.getEngine())
                        .eq(StrUtil.isNotBlank(query.getStatus()), SyncTaskLog::getStatus, query.getStatus())
                        .eq(SyncTaskLog::getWorkspaceCode, workspace.getCode())
                        .orderByDesc(SyncTaskLog::getId));
        PageResult<SyncTaskLogListResponse> pageResult = new PageResult<>();
        List<SyncTaskLog> records = logPage.getRecords();
        if (CollUtil.isEmpty(records)) {
            pageResult.setData(CollUtil.newArrayList(), page.getCurrent(), page.getSize(), logPage.getTotal());
            return pageResult;
        }
        Set<Long> taskIds = records.stream()
                .map(SyncTaskLog::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> taskNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(taskIds)) {
            this.lambdaQuery()
                    .select(SyncTask::getId, SyncTask::getName)
                    .in(SyncTask::getId, taskIds)
                    .list()
                    .forEach(t -> taskNameMap.put(t.getId(), t.getName()));
        }
        List<SyncTaskLogListResponse> collect = records.stream().map(m -> {
            SyncTaskLogListResponse response = new SyncTaskLogListResponse();
            this.orikaMapper.map(m, response);
            response.setTaskName(taskNameMap.get(m.getTaskId()));
            return response;
        }).collect(Collectors.toList());
        pageResult.setData(collect, logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        return pageResult;
    }

    @Override
    public SyncTaskLogDetailResponse logDetail(Long id) {
        SyncTaskLog log = this.syncTaskLogMapper.selectById(id);
        if (log == null) {
            return null;
        }
        SyncTaskLogDetailResponse response = new SyncTaskLogDetailResponse();
        this.orikaMapper.map(log, response);
        return response;
    }

    private void executeAsync(Long logId, SyncEngine engine, String config) {
        long start = System.currentTimeMillis();
        SyncTaskLog update = new SyncTaskLog();
        update.setId(logId);
        try {
            SyncEngineExecutor.ExecuteResult result = this.syncEngineExecutor.execute(engine, config);
            update.setLogContent(result.getLog());
            update.setDurationMs(System.currentTimeMillis() - start);
            if (result.isSuccess()) {
                update.setStatus("SUCCESS");
            } else {
                update.setStatus("FAIL");
                update.setErrorMsg(result.getErrorMsg() != null ? result.getErrorMsg() : ("引擎退出码 " + result.getExitCode()));
            }
        } catch (Exception e) {
            log.error("同步任务执行失败, logId:{}", logId, e);
            update.setStatus("FAIL");
            update.setErrorMsg(this.rootMessage(e));
            update.setDurationMs(System.currentTimeMillis() - start);
        } finally {
            update.setEndTime(LocalDateTime.now());
            this.syncTaskLogMapper.updateById(update);
        }
    }

    private SyncConfigResponse buildConfigResponse(SyncTask task) {
        SyncEngine engine = this.syncEngineFactory.get(task.getEngine());
        SyncEngineContext ctx = this.buildContext(task);
        String config = engine.buildConfig(ctx);
        SyncConfigResponse response = new SyncConfigResponse();
        response.setEngine(task.getEngine());
        response.setConfigContent(config);
        response.setCommand(engine.buildCommand("<config-file>"));
        return response;
    }

    private SyncEngineContext buildContext(SyncTask task) {
        WorkspaceData workspace = Context.getWorkspace();
        DataSource source = this.resolveDataSource(task.getSourceDataSourceCode(), workspace.getCode());
        DataSource target = this.resolveDataSource(task.getTargetDataSourceCode(), workspace.getCode());

        SyncEngineContext ctx = new SyncEngineContext();
        ctx.setSource(this.toEndpoint(source, task.getSourceSchema(), task.getSourceTable()));
        ctx.setTarget(this.toEndpoint(target, task.getTargetSchema(), task.getTargetTable()));
        if (StrUtil.isNotBlank(task.getFieldMapping())) {
            ctx.setFieldMappings(JSON.parseArray(task.getFieldMapping(), SyncEngineContext.FieldMapping.class));
        }
        return ctx;
    }

    private SyncEngineContext.Endpoint toEndpoint(DataSource ds, String schema, String table) {
        SyncEngineContext.Endpoint endpoint = new SyncEngineContext.Endpoint();
        endpoint.setType(ds.getType());
        endpoint.setJdbcUrl(ds.getUrl());
        endpoint.setUsername(ds.getUsername());
        endpoint.setPassword(this.passwordEncAndDecService.decrypt(ds.getPassword()));
        endpoint.setDriver(ds.getDriver());
        endpoint.setSchema(schema);
        endpoint.setTable(table);
        endpoint.setFeNodes(ds.getFeNodes());
        endpoint.setBeNodes(ds.getBeNodes());
        return endpoint;
    }

    private DataSource resolveDataSource(String datasourceCode, String workspaceCode) {
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

    private void validateEngine(String engine) {
        this.syncEngineFactory.get(engine);
    }

    private String serializeFieldMapping(List<SyncFieldMapping> fieldMapping) {
        if (CollUtil.isEmpty(fieldMapping)) {
            return null;
        }
        return JSON.toJSONString(fieldMapping);
    }

    private String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
