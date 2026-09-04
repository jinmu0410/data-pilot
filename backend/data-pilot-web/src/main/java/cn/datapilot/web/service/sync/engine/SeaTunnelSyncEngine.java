package cn.datapilot.web.service.sync.engine;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SeaTunnel 同步引擎：生成 SeaTunnel HOCON 配置
 *
 * @author jinmu
 */
@Component
public class SeaTunnelSyncEngine implements SyncEngine {

    @Value("${dp.sync.seatunnel.home:/opt/seatunnel}")
    private String seatunnelHome;

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

        String sourceQuery = buildSourceQuery(ctx);

        StringBuilder sb = new StringBuilder();
        sb.append("env {\n");
        sb.append("  job.mode = \"BATCH\"\n");
        sb.append("}\n\n");

        sb.append("source {\n");
        sb.append("  Jdbc {\n");
        sb.append("    url = ").append(quote(src.getJdbcUrl())).append("\n");
        sb.append("    driver = ").append(quote(src.getDriver())).append("\n");
        sb.append("    user = ").append(quote(src.getUsername())).append("\n");
        sb.append("    password = ").append(quote(src.getPassword())).append("\n");
        sb.append("    query = ").append(quote(sourceQuery)).append("\n");
        sb.append("  }\n");
        sb.append("}\n\n");

        sb.append("sink {\n");
        sb.append("  Jdbc {\n");
        sb.append("    url = ").append(quote(tgt.getJdbcUrl())).append("\n");
        sb.append("    driver = ").append(quote(tgt.getDriver())).append("\n");
        sb.append("    user = ").append(quote(tgt.getUsername())).append("\n");
        sb.append("    password = ").append(quote(tgt.getPassword())).append("\n");
        if (StrUtil.isNotBlank(tgt.getSchema())) {
            sb.append("    database = ").append(quote(tgt.getSchema())).append("\n");
        }
        sb.append("    table = ").append(quote(tgt.getTable())).append("\n");
        sb.append("    save_mode = \"append\"\n");
        sb.append("  }\n");
        sb.append("}\n");

        return sb.toString();
    }

    @Override
    public List<String> buildCommand(String configPath) {
        return List.of(seatunnelHome + "/bin/seatunnel.sh", "--config", configPath);
    }

    private String buildSourceQuery(SyncEngineContext ctx) {
        SyncEngineContext.Endpoint src = ctx.getSource();
        String table = src.fullTable();
        if (ctx.getFieldMappings() == null || ctx.getFieldMappings().isEmpty()) {
            return "SELECT * FROM " + table;
        }
        String columns = ctx.getFieldMappings().stream()
                .map(SyncEngineContext.FieldMapping::getSource)
                .collect(Collectors.joining(", "));
        return "SELECT " + columns + " FROM " + table;
    }

    private String quote(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
