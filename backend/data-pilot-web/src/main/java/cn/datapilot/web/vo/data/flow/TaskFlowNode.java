package cn.datapilot.web.vo.data.flow;

import cn.datapilot.web.service.task.model.SyncTaskParams;
import lombok.Data;

import java.util.List;

/**
 * 任务流节点参数（design JSON 中 node.properties，扁平化，按 type 填对应字段）
 *
 * @author jinmu
 */
@Data
public class TaskFlowNode {

    private String name;

    private String description;

    private Integer timeout;

    /**
     * SQL
     */
    private String datasourceCode;

    private String sqlType;

    private String sqlText;

    private String preSql;

    private String postSql;

    private String sqlParams;

    /**
     * DATAX/SEATUNNEL
     */
    private String sourceDataSourceCode;

    private String sourceSchema;

    private String sourceTable;

    private String readMode;

    private String whereCondition;

    private String partitionColumn;

    private Integer partitionNum;

    private String targetDataSourceCode;

    private String targetSchema;

    private String targetTable;

    private List<SyncTaskParams.FieldMapping> fieldMapping;

    /**
     * DataX 限流（字节数/记录数）
     */
    private Long jobSpeedByte;

    private Long jobSpeedRecord;

    /**
     * DataX 读取/写入批次与并发
     */
    private Integer fetchSize;

    private String writeMode;

    private Integer batchSize;

    private Integer channel;

    /**
     * SeaTunnel JDBC 写入及运行参数
     */
    private String sinkWriteStrategy;

    private String sinkQuery;

    private String schemaSaveMode;

    private String dataSaveMode;

    private String customSql;

    private List<String> primaryKeys;

    private Integer parallelism;

    private Integer retryTimes;

    /**
     * PYTHON/SHELL
     */
    private String script;
}
