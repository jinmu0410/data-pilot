package cn.datapilot.web.service.task.model;

import lombok.Data;

import java.util.List;

/**
 * 数据同步任务参数（DATAX/SEATUNNEL）
 *
 * @author jinmu
 */
@Data
public class SyncTaskParams {

    private String sourceDataSourceCode;

    private String sourceSchema;

    private String sourceTable;

    /**
     * SeaTunnel 读取模式：table/query
     */
    private String readMode;

    /**
     * SeaTunnel 表读取附加过滤条件，需以 where 开头
     */
    private String whereCondition;

    /**
     * SeaTunnel JDBC 查询分片字段与分片数
     */
    private String partitionColumn;

    private Integer partitionNum;

    private String targetDataSourceCode;

    private String targetSchema;

    private String targetTable;

    /**
     * 字段映射，空=全字段
     */
    private List<FieldMapping> fieldMapping;

    /**
     * DataX SQL 语句模式（DolphinScheduler 风格）：抽取数据的 SQL，as 别名映射目标列
     */
    private String sqlText;

    /**
     * 目标库前置 SQL（目标库执行）
     */
    private String preSql;

    /**
     * 目标库后置 SQL（目标库执行）
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

    /**
     * DataX 并发通道数
     */
    private Integer channel;

    /**
     * SeaTunnel 目标写入策略：generated/custom
     */
    private String sinkWriteStrategy;

    /**
     * SeaTunnel 自定义参数化写入 SQL
     */
    private String sinkQuery;

    private String schemaSaveMode;

    private String dataSaveMode;

    private String customSql;

    private List<String> primaryKeys;

    private Integer parallelism;

    private Integer retryTimes;

    @Data
    public static class FieldMapping {

        private String source;

        private String target;
    }
}
