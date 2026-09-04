package cn.dataplatform.open.web.service.sync.engine;

import cn.hutool.core.util.StrUtil;
import lombok.Data;

import java.util.List;

/**
 * 同步引擎上下文：已解析的源/目标连接信息
 *
 * @author dingqianwen
 */
@Data
public class SyncEngineContext {

    private Endpoint source;

    private Endpoint target;

    /**
     * 字段映射，空=全字段
     */
    private List<FieldMapping> fieldMappings;

    /**
     * DataX 并发通道数
     */
    private int channel = 3;

    /**
     * DataX SQL 语句模式：抽取数据的 SQL（querySql），存在时优先于 source.table
     */
    private String querySql;

    /**
     * 目标库前置 SQL
     */
    private String preSql;

    /**
     * 目标库后置 SQL
     */
    private String postSql;

    /**
     * 限流（字节数）
     */
    private Long jobSpeedByte;

    /**
     * 限流（记录数）
     */
    private Long jobSpeedRecord;

    /**
     * DataX 读取批次大小（fetchSize）
     */
    private Integer fetchSize;

    /**
     * DataX 写入模式：insert/replace/update/append/truncate
     */
    private String writeMode;

    /**
     * DataX 写入批次大小（batchSize）
     */
    private Integer batchSize;

    @Data
    public static class Endpoint {

        /**
         * MySQL/PostgreSQL/Doris
         */
        private String type;

        private String jdbcUrl;

        private String username;

        private String password;

        private String driver;

        private String schema;

        private String table;

        private String feNodes;

        private String beNodes;

        /**
         * 库.表（无库则仅表名）
         */
        public String fullTable() {
            if (StrUtil.isNotBlank(schema)) {
                return schema + "." + table;
            }
            return table;
        }
    }

    @Data
    public static class FieldMapping {
        private String source;
        private String target;
    }
}
