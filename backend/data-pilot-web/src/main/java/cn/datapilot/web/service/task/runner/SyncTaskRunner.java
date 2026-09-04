package cn.datapilot.web.service.task.runner;

import cn.datapilot.common.enums.Status;
import cn.datapilot.common.exception.ApiException;
import cn.datapilot.web.service.PasswordEncAndDecService;
import cn.datapilot.web.service.datasource.DataSourceService;
import cn.datapilot.web.service.sync.SyncEngineExecutor;
import cn.datapilot.web.service.sync.engine.SyncEngine;
import cn.datapilot.web.service.sync.engine.SyncEngineContext;
import cn.datapilot.web.service.sync.engine.SyncEngineFactory;
import cn.datapilot.web.service.task.model.SyncTaskParams;
import cn.datapilot.web.store.entity.DataSource;
import cn.datapilot.web.vo.data.task.TaskConfigResponse;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;

/**
 * 数据同步任务执行器（DATAX/SEATUNNEL）
 *
 * @author jinmu
 */
@Slf4j
@Component
public class SyncTaskRunner implements TaskRunner {

    @Resource
    private SyncEngineFactory syncEngineFactory;
    @Resource
    private SyncEngineExecutor syncEngineExecutor;
    @Resource
    private DataSourceService dataSourceService;
    @Resource
    private PasswordEncAndDecService passwordEncAndDecService;

    @Override
    public Set<String> types() {
        return Set.of("DATAX", "SEATUNNEL");
    }

    @Override
    public TaskRunResult run(TaskRunContext context) {
        SyncTaskParams params = JSON.parseObject(context.getTaskParams(), SyncTaskParams.class);
        TaskRunResult result = new TaskRunResult();
        try {
            SyncEngine engine = this.syncEngineFactory.get(context.getTaskType());
            SyncEngineContext ctx = this.buildContext(context.getTaskType(), params, context.getWorkspaceCode());
            String config = engine.buildConfig(ctx);
            SyncEngineExecutor.ExecuteResult r = this.syncEngineExecutor.execute(engine, config, context.getLogPath());
            result.setLogContent(r.getLog());
            if (r.isSuccess()) {
                result.setStatus("SUCCESS");
            } else {
                result.setStatus("FAIL");
                result.setErrorMsg(r.getErrorMsg() != null ? r.getErrorMsg() : ("引擎退出码 " + r.getExitCode()));
            }
        } catch (Exception e) {
            if (Thread.currentThread().isInterrupted()) {
                result.setStatus("SKIP");
                result.setErrorMsg("任务被终止");
            } else {
                log.error("同步任务执行失败", e);
                result.setStatus("FAIL");
                result.setErrorMsg(this.rootMessage(e));
            }
        }
        return result;
    }

    /**
     * 生成引擎配置（供 generateConfig 使用）
     */
    public TaskConfigResponse buildConfig(String taskType, String taskParams, String workspaceCode) {
        SyncTaskParams params = JSON.parseObject(taskParams, SyncTaskParams.class);
        SyncEngine engine = this.syncEngineFactory.get(taskType);
        SyncEngineContext ctx = this.buildContext(taskType, params, workspaceCode);
        TaskConfigResponse response = new TaskConfigResponse();
        response.setEngine(taskType);
        response.setConfigContent(engine.buildConfig(ctx));
        response.setCommand(engine.buildCommand("<config-file>"));
        return response;
    }

    private SyncEngineContext buildContext(String taskType, SyncTaskParams params, String workspaceCode) {
        DataSource source = this.resolveDataSource(params.getSourceDataSourceCode(), workspaceCode);
        DataSource target = this.resolveDataSource(params.getTargetDataSourceCode(), workspaceCode);

        SyncEngineContext ctx = new SyncEngineContext();
        if ("DATAX".equalsIgnoreCase(taskType) && StrUtil.isNotBlank(params.getSqlText())) {
            // DataX SQL 语句模式（DolphinScheduler 风格）
            ctx.setSource(this.toEndpoint(source, null, null));
            ctx.setTarget(this.toEndpoint(target, null, params.getTargetTable()));
            ctx.setQuerySql(params.getSqlText());
            ctx.setPreSql(params.getPreSql());
            ctx.setPostSql(params.getPostSql());
            ctx.setJobSpeedByte(params.getJobSpeedByte());
            ctx.setJobSpeedRecord(params.getJobSpeedRecord());
            ctx.setFetchSize(params.getFetchSize());
            ctx.setWriteMode(params.getWriteMode());
            ctx.setBatchSize(params.getBatchSize());
            if (params.getChannel() != null) {
                ctx.setChannel(params.getChannel());
            }
        } else {
            // SeaTunnel / 表同步模式
            ctx.setSource(this.toEndpoint(source, params.getSourceSchema(), params.getSourceTable()));
            ctx.setTarget(this.toEndpoint(target, params.getTargetSchema(), params.getTargetTable()));
            if (params.getFieldMapping() != null && !params.getFieldMapping().isEmpty()) {
                ctx.setFieldMappings(params.getFieldMapping().stream().map(f -> {
                    SyncEngineContext.FieldMapping fm = new SyncEngineContext.FieldMapping();
                    fm.setSource(f.getSource());
                    fm.setTarget(f.getTarget());
                    return fm;
                }).toList());
            }
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

    private String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
