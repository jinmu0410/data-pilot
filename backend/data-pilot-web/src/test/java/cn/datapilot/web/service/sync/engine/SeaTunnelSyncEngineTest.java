package cn.datapilot.web.service.sync.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeaTunnelSyncEngineTest {

    private final SeaTunnelSyncEngine engine = new SeaTunnelSyncEngine();

    @Test
    void shouldBuildGeneratedJdbcSinkWithFieldMapper() {
        SyncEngineContext context = baseContext();
        context.setReadMode("table");
        context.setFieldMappings(List.of(mapping("user_id", "id"), mapping("user_name", "name")));
        context.setPrimaryKeys(List.of("id"));
        context.setSchemaSaveMode("ERROR_WHEN_SCHEMA_NOT_EXIST");
        context.setDataSaveMode("APPEND_DATA");
        context.setParallelism(3);
        context.setBatchSize(2000);

        String config = engine.buildConfig(context);

        assertTrue(config.contains("parallelism = 3"));
        assertTrue(config.contains("username = \"reader\""));
        assertTrue(config.contains("table_path = \"sales.users\""));
        assertTrue(config.contains("FieldMapper"));
        assertTrue(config.contains("\"user_id\" = \"id\""));
        assertTrue(config.contains("generate_sink_sql = true"));
        assertTrue(config.contains("primary_keys = [\"id\"]"));
        assertTrue(config.contains("data_save_mode = \"APPEND_DATA\""));
        assertFalse(config.contains("\n    save_mode ="));
    }

    @Test
    void shouldBuildCustomJdbcSinkWithoutSaveModes() {
        SyncEngineContext context = baseContext();
        context.setReadMode("query");
        context.setQuerySql("SELECT id, name FROM sales.users");
        context.setSinkWriteStrategy("custom");
        context.setSinkQuery("INSERT INTO public.users(id, name) VALUES (?, ?)");

        String config = engine.buildConfig(context);

        assertTrue(config.contains("generate_sink_sql = false"));
        assertTrue(config.contains("INSERT INTO public.users(id, name) VALUES (?, ?)"));
        assertFalse(config.contains("schema_save_mode"));
        assertFalse(config.contains("data_save_mode"));
    }

    private SyncEngineContext baseContext() {
        SyncEngineContext context = new SyncEngineContext();
        context.setSource(endpoint("MySQL", "jdbc:mysql://localhost:3306/sales", "sales", "users", "reader"));
        context.setTarget(endpoint("PostgreSQL", "jdbc:postgresql://localhost:5432/warehouse", "public", "users", "writer"));
        return context;
    }

    private SyncEngineContext.Endpoint endpoint(String type, String url, String schema, String table, String username) {
        SyncEngineContext.Endpoint endpoint = new SyncEngineContext.Endpoint();
        endpoint.setType(type);
        endpoint.setJdbcUrl(url);
        endpoint.setDriver("driver");
        endpoint.setUsername(username);
        endpoint.setPassword("secret");
        endpoint.setSchema(schema);
        endpoint.setTable(table);
        return endpoint;
    }

    private SyncEngineContext.FieldMapping mapping(String source, String target) {
        SyncEngineContext.FieldMapping mapping = new SyncEngineContext.FieldMapping();
        mapping.setSource(source);
        mapping.setTarget(target);
        return mapping;
    }
}
