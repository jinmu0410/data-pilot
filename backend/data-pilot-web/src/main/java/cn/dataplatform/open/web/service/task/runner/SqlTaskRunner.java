package cn.dataplatform.open.web.service.task.runner;

import cn.dataplatform.open.common.enums.Status;
import cn.dataplatform.open.common.exception.ApiException;
import cn.dataplatform.open.web.service.datasource.DataSourceService;
import cn.dataplatform.open.web.service.task.model.SqlTaskParams;
import cn.dataplatform.open.web.store.entity.DataSource;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SQL 任务执行器
 *
 * @author jinmu
 */
@Slf4j
@Component
public class SqlTaskRunner implements TaskRunner {

    private static final int PREVIEW_ROW_LIMIT = 50000;

    @Resource
    private DataSourceService dataSourceService;

    @Override
    public Set<String> types() {
        return Set.of("SQL");
    }

    @Override
    public TaskRunResult run(TaskRunContext context) {
        SqlTaskParams params = JSON.parseObject(context.getTaskParams(), SqlTaskParams.class);
        TaskRunResult result = new TaskRunResult();
        try {
            DataSource dataSource = this.resolveDataSource(params.getDatasourceCode(), context.getWorkspaceCode());
            javax.sql.DataSource ds = this.dataSourceService.dataSourceConnect(dataSource, javax.sql.DataSource.class);
            int queryTimeout = context.getTimeout() == null ? 30 : context.getTimeout();
            Map<String, String> vars = this.parseSqlParams(params.getSqlParams());
            String sqlType = "NON_QUERY".equalsIgnoreCase(params.getSqlType()) ? "NON_QUERY" : "QUERY";
            try (Connection connection = ds.getConnection();
                 Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(queryTimeout);
                // 多取一行用于探测是否截断（结果仍只返回 PREVIEW_ROW_LIMIT 行）
                statement.setMaxRows(PREVIEW_ROW_LIMIT + 1);
                this.executeScript(statement, this.substitute(params.getPreSql(), vars), result, "NONE");
                this.executeScript(statement, this.substitute(params.getSqlText(), vars), result, sqlType);
                this.executeScript(statement, this.substitute(params.getPostSql(), vars), result, "NONE");
                if (result.getColumns() == null) {
                    result.setColumns(Collections.emptyList());
                    result.setRows(Collections.emptyList());
                    result.setTruncated(false);
                }
            }
            result.setStatus("SUCCESS");
        } catch (Exception e) {
            log.error("SQL 执行失败, datasourceCode:{}, sql:{}", params.getDatasourceCode(), params.getSqlText(), e);
            result.setStatus("FAIL");
            result.setErrorMsg(this.rootMessage(e));
            result.setColumns(Collections.emptyList());
            result.setRows(Collections.emptyList());
            result.setRowCount(0L);
        }
        return result;
    }

    /**
     * 执行一段 SQL 脚本（按 ; 拆分成多段依次执行）。
     * mode: NONE 前置/后置（不采集不计数）、QUERY 采集结果集、NON_QUERY 累计影响行数。
     */
    private void executeScript(Statement statement, String script, TaskRunResult result, String mode) throws SQLException {
        if (StrUtil.isBlank(script)) {
            return;
        }
        for (String stmt : this.splitSql(script)) {
            boolean isResultSet = statement.execute(stmt);
            if (isResultSet) {
                if ("QUERY".equals(mode)) {
                    this.readResultSet(statement, result);
                } else {
                    try (ResultSet ignored = statement.getResultSet()) {
                        // 不采集的结果集仅关闭释放
                    }
                }
            } else if ("NON_QUERY".equals(mode)) {
                int updateCount = statement.getUpdateCount();
                long current = result.getRowCount() == null ? 0L : result.getRowCount();
                result.setRowCount(current + (updateCount >= 0 ? updateCount : 0L));
            }
        }
    }

    private List<String> splitSql(String sql) {
        if (StrUtil.isBlank(sql)) {
            return Collections.emptyList();
        }
        return Arrays.stream(sql.split(";"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    private Map<String, String> parseSqlParams(String sqlParams) {
        Map<String, String> vars = new HashMap<>();
        if (StrUtil.isBlank(sqlParams)) {
            return vars;
        }
        for (String pair : sqlParams.split(";")) {
            if (StrUtil.isBlank(pair)) {
                continue;
            }
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            vars.put(pair.substring(0, idx).trim(), pair.substring(idx + 1).trim());
        }
        return vars;
    }

    private String substitute(String sql, Map<String, String> vars) {
        if (StrUtil.isBlank(sql) || vars.isEmpty()) {
            return sql;
        }
        String result = sql;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue() == null ? "" : entry.getValue());
        }
        return result;
    }

    private void readResultSet(Statement statement, TaskRunResult result) throws SQLException {
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
            result.setColumns(columns);
            result.setRows(rows);
            result.setRowCount((long) rows.size());
            result.setTruncated(truncated);
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

    private String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
