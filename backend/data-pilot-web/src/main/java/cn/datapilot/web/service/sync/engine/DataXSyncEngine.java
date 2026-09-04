package cn.datapilot.web.service.sync.engine;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DataX 同步引擎：生成 DataX job JSON
 *
 * @author jinmu
 */
@Component
public class DataXSyncEngine implements SyncEngine {

    @Value("${dp.sync.datax.home:/usr/local/datax}")
    private String dataxHome;

    @Override
    public String type() {
        return "DATAX";
    }

    @Override
    public String configFileName() {
        return "job.json";
    }

    @Override
    public String buildConfig(SyncEngineContext ctx) {
        SyncEngineContext.Endpoint src = ctx.getSource();
        SyncEngineContext.Endpoint tgt = ctx.getTarget();

        boolean queryMode = StrUtil.isNotBlank(ctx.getQuerySql());

        Map<String, Object> reader = new LinkedHashMap<>();
        reader.put("name", readerName(src.getType()));
        Map<String, Object> readerParam = new LinkedHashMap<>();
        readerParam.put("username", src.getUsername());
        readerParam.put("password", src.getPassword());
        Map<String, Object> readerConn = new LinkedHashMap<>();
        readerConn.put("jdbcUrl", Collections.singletonList(this.jdbcUrl(src.getJdbcUrl())));
        if (queryMode) {
            readerConn.put("querySql", Collections.singletonList(ctx.getQuerySql()));
        } else {
            readerParam.put("column", sourceColumns(ctx));
            readerConn.put("table", Collections.singletonList(src.fullTable()));
        }
        if (ctx.getFetchSize() != null) {
            readerParam.put("fetchSize", ctx.getFetchSize());
        }
        readerParam.put("connection", Collections.singletonList(readerConn));
        reader.put("parameter", readerParam);

        Map<String, Object> writer = new LinkedHashMap<>();
        writer.put("name", writerName(tgt.getType()));
        Map<String, Object> writerParam = new LinkedHashMap<>();
        writerParam.put("username", tgt.getUsername());
        writerParam.put("password", tgt.getPassword());
        writerParam.put("writeMode", StrUtil.isBlank(ctx.getWriteMode()) ? "insert" : ctx.getWriteMode());
        writerParam.put("column", targetColumns(ctx));
        writerParam.put("preSql", this.splitStatements(ctx.getPreSql()));
        writerParam.put("postSql", this.splitStatements(ctx.getPostSql()));
        if (ctx.getBatchSize() != null) {
            writerParam.put("batchSize", ctx.getBatchSize());
        }
        Map<String, Object> writerConn = new LinkedHashMap<>();
        writerConn.put("jdbcUrl", this.jdbcUrl(tgt.getJdbcUrl()));
        writerConn.put("table", Collections.singletonList(tgt.fullTable()));
        writerParam.put("connection", Collections.singletonList(writerConn));
        writer.put("parameter", writerParam);

        Map<String, Object> content = new LinkedHashMap<>();
        content.put("reader", reader);
        content.put("writer", writer);

        Map<String, Object> speed = new LinkedHashMap<>();
        speed.put("channel", ctx.getChannel());
        if (ctx.getJobSpeedByte() != null) {
            speed.put("byte", ctx.getJobSpeedByte());
        }
        if (ctx.getJobSpeedRecord() != null) {
            speed.put("record", ctx.getJobSpeedRecord());
        }
        Map<String, Object> errorLimit = new LinkedHashMap<>();
        errorLimit.put("record", 0);
        errorLimit.put("percentage", 0.02);
        Map<String, Object> setting = new LinkedHashMap<>();
        setting.put("speed", speed);
        setting.put("errorLimit", errorLimit);

        Map<String, Object> job = new LinkedHashMap<>();
        job.put("setting", setting);
        job.put("content", Collections.singletonList(content));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("job", job);

        return JSON.toJSONString(root, JSONWriter.Feature.PrettyFormat);
    }

    private List<String> splitStatements(String sql) {
        if (StrUtil.isBlank(sql)) {
            return Collections.emptyList();
        }
        return Arrays.stream(sql.split(";"))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> buildCommand(String configPath) {
        return List.of(dataxHome + "/bin/datax.py", configPath);
    }

    /**
     * 为 MySQL 的 JDBC URL 追加 useSSL=false，消除老驱动在 MySQL 8.0 下的 SSL 噪声
     */
    private String jdbcUrl(String url) {
        if (StrUtil.isBlank(url) || !url.startsWith("jdbc:mysql:")) {
            return url;
        }
        if (url.contains("useSSL")) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + "useSSL=false";
    }

    private String readerName(String type) {
        if ("PostgreSQL".equalsIgnoreCase(type)) {
            return "postgresqlreader";
        }
        // MySQL / Doris（Doris 走 MySQL 协议）
        return "mysqlreader";
    }

    private String writerName(String type) {
        if ("PostgreSQL".equalsIgnoreCase(type)) {
            return "postgresqlwriter";
        }
        // MySQL / Doris（Doris 目标用 mysqlwriter，Stream Load 的 doriswriter 后续增强）
        return "mysqlwriter";
    }

    private List<String> sourceColumns(SyncEngineContext ctx) {
        if (ctx.getFieldMappings() == null || ctx.getFieldMappings().isEmpty()) {
            return Collections.singletonList("*");
        }
        return ctx.getFieldMappings().stream().map(SyncEngineContext.FieldMapping::getSource).toList();
    }

    private List<String> targetColumns(SyncEngineContext ctx) {
        if (ctx.getFieldMappings() == null || ctx.getFieldMappings().isEmpty()) {
            return Collections.singletonList("*");
        }
        return ctx.getFieldMappings().stream().map(SyncEngineContext.FieldMapping::getTarget).toList();
    }
}
