package cn.datapilot.web.service.sync.engine;

import cn.datapilot.web.service.SystemConfigService;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * SeaTunnel JDBC 批同步引擎：生成 source -> FieldMapper -> sink HOCON 配置。
 *
 * @author jinmu
 */
@Component
public class SeaTunnelSyncEngine implements SyncEngine {

    @Resource
    private SystemConfigService systemConfigService;

    private String seatunnelHome() {
        return this.systemConfigService.getValue("sync.seatunnel.home", "/opt/seatunnel");
    }

    @Override
    public String type() {
        return "SEATUNNEL";
    }

    @Override
    public String configFileName() {
        return "seatunnel.conf";
    }

    @Override
    public String buildConfig(SyncEngineContext ctx) {
        SyncEngineContext.Endpoint src = ctx.getSource();
        SyncEngineContext.Endpoint tgt = ctx.getTarget();
        boolean hasMapping = ctx.getFieldMappings() != null && !ctx.getFieldMappings().isEmpty();
        boolean customSink = "custom".equalsIgnoreCase(ctx.getSinkWriteStrategy());

        StringBuilder sb = new StringBuilder();
        sb.append("env {\n");
        sb.append("  job.mode = \"BATCH\"\n");
        sb.append("  parallelism = ").append(positiveOrDefault(ctx.getParallelism(), 1)).append("\n");
        sb.append("  job.retry.times = ").append(nonNegativeOrDefault(ctx.getRetryTimes(), 0)).append("\n");
        sb.append("}\n\n");

        sb.append("source {\n");
        sb.append("  Jdbc {\n");
        sb.append("    plugin_output = \"source_rows\"\n");
        appendJdbcConnection(sb, src);
        if ("query".equalsIgnoreCase(ctx.getReadMode()) && StrUtil.isNotBlank(ctx.getQuerySql())) {
            sb.append("    query = ").append(quote(ctx.getQuerySql().trim())).append("\n");
        } else {
            sb.append("    table_path = ").append(quote(src.fullTable())).append("\n");
            if (hasMapping) {
                sb.append("    query = ").append(quote(buildTableQuery(ctx))).append("\n");
            }
        }
        if (StrUtil.isNotBlank(ctx.getWhereCondition())) {
            sb.append("    where_condition = ").append(quote(ctx.getWhereCondition().trim())).append("\n");
        }
        if (StrUtil.isNotBlank(ctx.getPartitionColumn())) {
            sb.append("    partition_column = ").append(quote(ctx.getPartitionColumn().trim())).append("\n");
            sb.append("    partition_num = ").append(positiveOrDefault(ctx.getPartitionNum(), 10)).append("\n");
        }
        if (ctx.getFetchSize() != null && ctx.getFetchSize() > 0) {
            sb.append("    fetch_size = ").append(ctx.getFetchSize()).append("\n");
        }
        sb.append("  }\n");
        sb.append("}\n\n");

        if (hasMapping) {
            sb.append("transform {\n");
            sb.append("  FieldMapper {\n");
            sb.append("    plugin_input = \"source_rows\"\n");
            sb.append("    plugin_output = \"mapped_rows\"\n");
            sb.append("    field_mapper = {\n");
            for (SyncEngineContext.FieldMapping mapping : ctx.getFieldMappings()) {
                sb.append("      ").append(quote(mapping.getSource()))
                        .append(" = ").append(quote(mapping.getTarget())).append("\n");
            }
            sb.append("    }\n");
            sb.append("  }\n");
            sb.append("}\n\n");
        }

        sb.append("sink {\n");
        sb.append("  Jdbc {\n");
        sb.append("    plugin_input = ").append(quote(hasMapping ? "mapped_rows" : "source_rows")).append("\n");
        appendJdbcConnection(sb, tgt);
        if (customSink) {
            sb.append("    generate_sink_sql = false\n");
            sb.append("    query = ").append(quote(ctx.getSinkQuery().trim())).append("\n");
        } else {
            sb.append("    generate_sink_sql = true\n");
            sb.append("    database = ").append(quote(targetDatabase(tgt))).append("\n");
            sb.append("    table = ").append(quote(targetTable(tgt))).append("\n");
            appendStringArray(sb, "primary_keys", ctx.getPrimaryKeys());
            sb.append("    schema_save_mode = ")
                    .append(quote(StrUtil.blankToDefault(ctx.getSchemaSaveMode(), "ERROR_WHEN_SCHEMA_NOT_EXIST"))).append("\n");
            sb.append("    data_save_mode = ")
                    .append(quote(StrUtil.blankToDefault(ctx.getDataSaveMode(), "APPEND_DATA"))).append("\n");
            if ("CUSTOM_PROCESSING".equalsIgnoreCase(ctx.getDataSaveMode()) && StrUtil.isNotBlank(ctx.getCustomSql())) {
                sb.append("    custom_sql = ").append(quote(ctx.getCustomSql().trim())).append("\n");
            }
        }
        sb.append("    batch_size = ").append(positiveOrDefault(ctx.getBatchSize(), 1000)).append("\n");
        sb.append("    max_retries = ").append(nonNegativeOrDefault(ctx.getRetryTimes(), 0)).append("\n");
        sb.append("  }\n");
        sb.append("}\n");
        return sb.toString();
    }

    @Override
    public List<String> buildCommand(String configPath) {
        return List.of(this.seatunnelHome() + "/bin/seatunnel.sh", "--config", configPath);
    }

    private void appendJdbcConnection(StringBuilder sb, SyncEngineContext.Endpoint endpoint) {
        sb.append("    url = ").append(quote(endpoint.getJdbcUrl())).append("\n");
        sb.append("    driver = ").append(quote(endpoint.getDriver())).append("\n");
        sb.append("    username = ").append(quote(endpoint.getUsername())).append("\n");
        sb.append("    password = ").append(quote(endpoint.getPassword())).append("\n");
    }

    private String buildTableQuery(SyncEngineContext ctx) {
        List<String> columns = new ArrayList<>();
        for (SyncEngineContext.FieldMapping mapping : ctx.getFieldMappings()) {
            if (StrUtil.isNotBlank(mapping.getSource())) {
                columns.add(quoteIdentifier(mapping.getSource(), ctx.getSource().getType()));
            }
        }
        if (StrUtil.isNotBlank(ctx.getPartitionColumn())
                && ctx.getFieldMappings().stream().noneMatch(item ->
                ctx.getPartitionColumn().equalsIgnoreCase(item.getSource()))) {
            columns.add(quoteIdentifier(ctx.getPartitionColumn(), ctx.getSource().getType()));
        }
        return "SELECT " + String.join(", ", columns)
                + " FROM " + quoteQualifiedIdentifier(ctx.getSource().fullTable(), ctx.getSource().getType());
    }

    private String quoteQualifiedIdentifier(String value, String type) {
        return String.join(".", List.of(value.split("\\.")).stream()
                .map(part -> quoteIdentifier(part, type)).toList());
    }

    private String quoteIdentifier(String value, String type) {
        String clean = StrUtil.blankToDefault(value, "").trim();
        if ("postgresql".equalsIgnoreCase(type)) {
            return "\"" + clean.replace("\"", "\"\"") + "\"";
        }
        return "`" + clean.replace("`", "``") + "`";
    }

    private String targetDatabase(SyncEngineContext.Endpoint target) {
        if (!"postgresql".equalsIgnoreCase(target.getType())) {
            return StrUtil.blankToDefault(target.getSchema(), databaseFromUrl(target.getJdbcUrl()));
        }
        return databaseFromUrl(target.getJdbcUrl());
    }

    private String targetTable(SyncEngineContext.Endpoint target) {
        if ("postgresql".equalsIgnoreCase(target.getType()) && StrUtil.isNotBlank(target.getSchema())) {
            return target.getSchema() + "." + target.getTable();
        }
        return target.getTable();
    }

    private String databaseFromUrl(String url) {
        if (StrUtil.isBlank(url)) {
            return "";
        }
        String withoutQuery = url.split("\\?", 2)[0];
        int slash = withoutQuery.lastIndexOf('/');
        return slash >= 0 ? withoutQuery.substring(slash + 1) : "";
    }

    private void appendStringArray(StringBuilder sb, String key, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        List<String> cleanValues = values.stream().filter(StrUtil::isNotBlank).map(String::trim).toList();
        if (cleanValues.isEmpty()) {
            return;
        }
        sb.append("    ").append(key).append(" = [")
                .append(String.join(", ", cleanValues.stream().map(this::quote).toList()))
                .append("]\n");
    }

    private int positiveOrDefault(Integer value, int defaultValue) {
        return value == null || value < 1 ? defaultValue : value;
    }

    private int nonNegativeOrDefault(Integer value, int defaultValue) {
        return value == null || value < 0 ? defaultValue : value;
    }

    private String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n") + "\"";
    }
}
