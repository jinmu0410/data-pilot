package cn.dataplatform.open.web.service.develop.impl;

import cn.dataplatform.open.common.component.OrikaMapper;
import cn.dataplatform.open.common.enums.Status;
import cn.dataplatform.open.common.exception.ApiException;
import cn.dataplatform.open.common.util.CronUtils;
import cn.dataplatform.open.common.vo.base.PageBase;
import cn.dataplatform.open.common.vo.base.PageRequest;
import cn.dataplatform.open.common.vo.base.PageResult;
import cn.dataplatform.open.web.config.Context;
import cn.dataplatform.open.web.service.datasource.DataSourceService;
import cn.dataplatform.open.web.service.develop.DevelopTaskService;
import cn.dataplatform.open.web.store.entity.DataSource;
import cn.dataplatform.open.web.store.entity.DevelopTask;
import cn.dataplatform.open.web.store.entity.DevelopTaskLog;
import cn.dataplatform.open.web.store.mapper.DevelopTaskLogMapper;
import cn.dataplatform.open.web.vo.data.develop.*;
import cn.dataplatform.open.web.vo.workspace.WorkspaceData;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 数据研发-SQL 任务实现
 *
 * @author dingqianwen
 * @date 2025/1/4
 * @since 1.0.0
 */
@Slf4j
@Service
public class DevelopTaskServiceImpl extends ServiceImpl<cn.dataplatform.open.web.store.mapper.DevelopTaskMapper, DevelopTask>
        implements DevelopTaskService {

    /**
     * 预览行数上限
     */
    private static final int PREVIEW_ROW_LIMIT = 200;

    @Resource
    private OrikaMapper orikaMapper;
    @Resource
    private DataSourceService dataSourceService;
    @Resource
    private DevelopTaskLogMapper developTaskLogMapper;

    @Override
    public PageResult<DevelopTaskListResponse> list(PageRequest<DevelopTaskListRequest> pageRequest) {
        WorkspaceData workspace = Context.getWorkspace();
        PageBase page = pageRequest.getPage();
        DevelopTaskListRequest query = Optional.ofNullable(pageRequest.getQuery()).orElse(new DevelopTaskListRequest());
        Page<DevelopTask> taskPage = this.lambdaQuery()
                .and(StrUtil.isNotBlank(query.getKeyword()), q -> q
                        .like(DevelopTask::getName, query.getKeyword())
                        .or()
                        .like(DevelopTask::getCode, query.getKeyword()))
                .eq(StrUtil.isNotBlank(query.getDatasourceCode()), DevelopTask::getDatasourceCode, query.getDatasourceCode())
                .eq(StrUtil.isNotBlank(query.getStatus()), DevelopTask::getStatus, query.getStatus())
                .eq(DevelopTask::getWorkspaceCode, workspace.getCode())
                .orderByDesc(DevelopTask::getUpdateTime)
                .page(new Page<>(page.getCurrent(), page.getSize()));
        PageResult<DevelopTaskListResponse> pageResult = new PageResult<>();
        List<DevelopTask> records = taskPage.getRecords();
        if (CollUtil.isEmpty(records)) {
            pageResult.setData(CollUtil.newArrayList(), page.getCurrent(), page.getSize(), 0L);
            return pageResult;
        }
        Set<String> dsCodes = records.stream()
                .map(DevelopTask::getDatasourceCode)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toSet());
        Map<String, String> dsNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(dsCodes)) {
            this.dataSourceService.lambdaQuery()
                    .select(DataSource::getCode, DataSource::getName)
                    .eq(DataSource::getWorkspaceCode, workspace.getCode())
                    .in(DataSource::getCode, dsCodes)
                    .list()
                    .forEach(ds -> dsNameMap.put(ds.getCode(), ds.getName()));
        }
        List<DevelopTaskListResponse> collect = records.stream().map(m -> {
            DevelopTaskListResponse response = new DevelopTaskListResponse();
            this.orikaMapper.map(m, response);
            response.setDatasourceName(dsNameMap.get(m.getDatasourceCode()));
            return response;
        }).collect(Collectors.toList());
        pageResult.setData(collect, taskPage.getCurrent(), taskPage.getSize(), taskPage.getTotal());
        return pageResult;
    }

    @Override
    public Long add(DevelopTaskAddRequest request) {
        WorkspaceData workspace = Context.getWorkspace();
        if (this.lambdaQuery().eq(DevelopTask::getName, request.getName())
                .eq(DevelopTask::getWorkspaceCode, workspace.getCode())
                .exists()) {
            throw new ApiException("任务名称已存在");
        }
        this.validateDatasource(request.getDatasourceCode(), workspace.getCode());
        this.validateCron(request.getCron());
        DevelopTask task = new DevelopTask();
        this.orikaMapper.map(request, task);
        task.setCode(UUID.fastUUID().toString(true));
        task.setWorkspaceCode(workspace.getCode());
        task.setCreateUserId(Context.getUser().getId());
        if (StrUtil.isBlank(task.getStatus())) {
            task.setStatus(Status.ENABLE.name());
        }
        if (task.getTimeout() == null) {
            task.setTimeout(30);
        }
        task.setNextExecTime(null);
        this.save(task);
        return task.getId();
    }

    @Override
    public Boolean update(DevelopTaskUpdateRequest request) {
        WorkspaceData workspace = Context.getWorkspace();
        if (this.lambdaQuery().eq(DevelopTask::getName, request.getName())
                .ne(DevelopTask::getId, request.getId())
                .eq(DevelopTask::getWorkspaceCode, workspace.getCode())
                .exists()) {
            throw new ApiException("任务名称已存在");
        }
        DevelopTask task = this.getById(request.getId());
        if (task == null) {
            throw new ApiException("任务不存在");
        }
        this.validateDatasource(request.getDatasourceCode(), workspace.getCode());
        this.validateCron(request.getCron());
        this.orikaMapper.map(request, task);
        task.setNextExecTime(null);
        this.updateById(task);
        return true;
    }

    @Override
    public DevelopTaskDetailResponse detail(Long id) {
        DevelopTask task = this.getById(id);
        if (task == null) {
            return null;
        }
        DevelopTaskDetailResponse response = new DevelopTaskDetailResponse();
        this.orikaMapper.map(task, response);
        return response;
    }

    @Override
    public Boolean delete(Long id) {
        DevelopTask task = this.getById(id);
        if (task == null) {
            return false;
        }
        this.removeById(id);
        return true;
    }

    @Override
    public DevelopRunResponse run(DevelopTaskRunRequest request) {
        WorkspaceData workspace = Context.getWorkspace();
        if (request.getId() != null) {
            DevelopTask task = this.getById(request.getId());
            if (task == null) {
                throw new ApiException("任务不存在");
            }
            return this.execute(task.getDatasourceCode(), task.getSqlText(), task.getTimeout(),
                    task.getId(), task.getCode(), task.getWorkspaceCode(), "MANUAL");
        }
        if (StrUtil.isBlank(request.getDatasourceCode())) {
            throw new ApiException("请选择数据源");
        }
        if (StrUtil.isBlank(request.getSqlText())) {
            throw new ApiException("SQL 不能为空");
        }
        return this.execute(request.getDatasourceCode(), request.getSqlText(), 30,
                null, null, workspace.getCode(), null);
    }

    @Override
    public PageResult<DevelopTaskLogListResponse> logList(PageRequest<DevelopTaskLogListRequest> pageRequest) {
        WorkspaceData workspace = Context.getWorkspace();
        PageBase page = pageRequest.getPage();
        DevelopTaskLogListRequest query = Optional.ofNullable(pageRequest.getQuery()).orElse(new DevelopTaskLogListRequest());

        List<Long> keywordTaskIds = null;
        if (StrUtil.isNotBlank(query.getKeyword())) {
            keywordTaskIds = this.lambdaQuery()
                    .select(DevelopTask::getId)
                    .like(DevelopTask::getName, query.getKeyword())
                    .eq(DevelopTask::getWorkspaceCode, workspace.getCode())
                    .list()
                    .stream()
                    .map(DevelopTask::getId)
                    .collect(Collectors.toList());
            if (keywordTaskIds.isEmpty()) {
                PageResult<DevelopTaskLogListResponse> empty = new PageResult<>();
                empty.setData(CollUtil.newArrayList(), page.getCurrent(), page.getSize(), 0L);
                return empty;
            }
        }

        Page<DevelopTaskLog> logPage = this.developTaskLogMapper.selectPage(
                new Page<>(page.getCurrent(), page.getSize()),
                new LambdaQueryWrapper<DevelopTaskLog>()
                        .eq(query.getTaskId() != null, DevelopTaskLog::getTaskId, query.getTaskId())
                        .in(keywordTaskIds != null, DevelopTaskLog::getTaskId, keywordTaskIds)
                        .eq(StrUtil.isNotBlank(query.getStatus()), DevelopTaskLog::getStatus, query.getStatus())
                        .eq(StrUtil.isNotBlank(query.getTriggerType()), DevelopTaskLog::getTriggerType, query.getTriggerType())
                        .eq(DevelopTaskLog::getWorkspaceCode, workspace.getCode())
                        .orderByDesc(DevelopTaskLog::getId));
        PageResult<DevelopTaskLogListResponse> pageResult = new PageResult<>();
        List<DevelopTaskLog> records = logPage.getRecords();
        if (CollUtil.isEmpty(records)) {
            pageResult.setData(CollUtil.newArrayList(), page.getCurrent(), page.getSize(), logPage.getTotal());
            return pageResult;
        }

        Set<Long> taskIds = records.stream()
                .map(DevelopTaskLog::getTaskId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> taskNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(taskIds)) {
            this.lambdaQuery()
                    .select(DevelopTask::getId, DevelopTask::getName)
                    .in(DevelopTask::getId, taskIds)
                    .list()
                    .forEach(t -> taskNameMap.put(t.getId(), t.getName()));
        }

        List<DevelopTaskLogListResponse> collect = records.stream().map(m -> {
            DevelopTaskLogListResponse response = new DevelopTaskLogListResponse();
            this.orikaMapper.map(m, response);
            response.setTaskName(taskNameMap.get(m.getTaskId()));
            return response;
        }).collect(Collectors.toList());
        pageResult.setData(collect, logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        return pageResult;
    }

    @Override
    public DevelopTaskLogDetailResponse logDetail(Long id) {
        DevelopTaskLog log = this.developTaskLogMapper.selectById(id);
        if (log == null) {
            return null;
        }
        DevelopTaskLogDetailResponse response = new DevelopTaskLogDetailResponse();
        this.orikaMapper.map(log, response);
        if (StrUtil.isNotBlank(log.getPreview())) {
            JSONObject obj = JSON.parseObject(log.getPreview());
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

    @Override
    public List<DevelopTask> listEnabledCronTasks() {
        return this.lambdaQuery()
                .eq(DevelopTask::getStatus, Status.ENABLE.name())
                .isNotNull(DevelopTask::getCron)
                .ne(DevelopTask::getCron, "")
                .list();
    }

    @Override
    public void advanceNextExecTime(DevelopTask task) {
        if (StrUtil.isBlank(task.getCron()) || !CronUtils.isValid(task.getCron())) {
            task.setNextExecTime(null);
        } else {
            List<ZonedDateTime> nexts = CronUtils.nextExecutionTime(task.getCron(), ZonedDateTime.now(), 1);
            task.setNextExecTime(nexts.isEmpty() ? null
                    : nexts.get(0).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
        }
        this.updateById(task);
    }

    @Override
    public DevelopRunResponse runTask(DevelopTask task, String triggerType) {
        return this.execute(task.getDatasourceCode(), task.getSqlText(), task.getTimeout(),
                task.getId(), task.getCode(), task.getWorkspaceCode(), triggerType);
    }

    /**
     * 执行 SQL 并（可选）写入运行记录
     */
    private DevelopRunResponse execute(String datasourceCode, String sqlText, Integer timeout,
                                       Long taskId, String taskCode, String workspaceCode, String triggerType) {
        DevelopRunResponse response = new DevelopRunResponse();
        long start = System.currentTimeMillis();
        DevelopTaskLog taskLog = null;
        if (taskId != null) {
            taskLog = new DevelopTaskLog();
            taskLog.setTaskId(taskId);
            taskLog.setTaskCode(taskCode);
            taskLog.setWorkspaceCode(workspaceCode);
            taskLog.setTriggerType(triggerType);
            taskLog.setSqlText(sqlText);
            taskLog.setStartTime(LocalDateTime.now());
            taskLog.setCreateTime(LocalDateTime.now());
        }
        try {
            DataSource dataSource = this.resolveDataSource(datasourceCode, workspaceCode);
            javax.sql.DataSource ds = this.dataSourceService.dataSourceConnect(dataSource, javax.sql.DataSource.class);
            int queryTimeout = timeout == null ? 30 : timeout;
            try (Connection connection = ds.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(queryTimeout);
                boolean isResultSet = statement.execute(sqlText);
                if (isResultSet) {
                    this.readResultSet(statement, response);
                } else {
                    int updateCount = statement.getUpdateCount();
                    response.setRowCount(updateCount >= 0 ? (long) updateCount : 0L);
                    response.setColumns(Collections.emptyList());
                    response.setRows(Collections.emptyList());
                    response.setTruncated(false);
                }
            }
            response.setStatus("SUCCESS");
        } catch (Exception e) {
            log.error("SQL 执行失败, taskId:{}, datasourceCode:{}, sql:{}", taskId, datasourceCode, sqlText, e);
            response.setStatus("FAIL");
            response.setError(this.rootMessage(e));
            response.setColumns(Collections.emptyList());
            response.setRows(Collections.emptyList());
            response.setRowCount(0L);
        }
        response.setDurationMs(System.currentTimeMillis() - start);
        if (taskLog != null) {
            taskLog.setStatus(response.getStatus());
            taskLog.setEndTime(LocalDateTime.now());
            taskLog.setDurationMs(response.getDurationMs());
            taskLog.setRowCount(response.getRowCount());
            taskLog.setErrorMsg(response.getError());
            taskLog.setPreview(this.buildPreviewJson(response));
            this.developTaskLogMapper.insert(taskLog);
            response.setLogId(taskLog.getId());
        }
        return response;
    }

    private void readResultSet(Statement statement, DevelopRunResponse response) throws SQLException {
        try (ResultSet rs = statement.getResultSet()) {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            List<String> columns = new ArrayList<>(columnCount);
            for (int i = 1; i <= columnCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }
            List<List<String>> rows = new ArrayList<>();
            boolean truncated = false;
            while (rs.next()) {
                if (rows.size() >= PREVIEW_ROW_LIMIT) {
                    truncated = true;
                    break;
                }
                List<String> row = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    row.add(this.convertValue(rs.getObject(i)));
                }
                rows.add(row);
            }
            response.setColumns(columns);
            response.setRows(rows);
            response.setRowCount((long) rows.size());
            response.setTruncated(truncated);
        }
    }

    private String convertValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp || value instanceof java.sql.Date || value instanceof Time
                || value instanceof java.time.LocalDateTime || value instanceof java.time.LocalDate
                || value instanceof java.time.LocalTime) {
            return value.toString();
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (value instanceof Blob blob) {
            try {
                return new String(blob.getBytes(1, (int) Math.min(blob.length(), 1024)), StandardCharsets.UTF_8);
            } catch (SQLException e) {
                return null;
            }
        }
        if (value instanceof Clob clob) {
            try {
                return clob.getSubString(1, (int) Math.min(clob.length(), 1024));
            } catch (SQLException e) {
                return null;
            }
        }
        return value.toString();
    }

    private String buildPreviewJson(DevelopRunResponse response) {
        Map<String, Object> preview = new HashMap<>();
        preview.put("columns", response.getColumns());
        preview.put("rows", response.getRows());
        preview.put("truncated", response.getTruncated());
        return JSON.toJSONString(preview);
    }

    private DataSource resolveDataSource(String datasourceCode, String workspaceCode) {
        DataSource dataSource = this.dataSourceService.lambdaQuery()
                .eq(DataSource::getCode, datasourceCode)
                .eq(DataSource::getWorkspaceCode, workspaceCode)
                .one();
        if (dataSource == null) {
            throw new ApiException("数据源不存在");
        }
        if (!Objects.equals(dataSource.getStatus(), Status.ENABLE.name())) {
            throw new ApiException("数据源非启用状态");
        }
        return dataSource;
    }

    private void validateDatasource(String datasourceCode, String workspaceCode) {
        this.resolveDataSource(datasourceCode, workspaceCode);
    }

    private void validateCron(String cron) {
        if (StrUtil.isNotBlank(cron) && !CronUtils.isValid(cron)) {
            throw new ApiException("cron 表达式不合法");
        }
    }

    private String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }

}
