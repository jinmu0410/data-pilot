package cn.dataplatform.open.web.service.task.model;

import lombok.Data;

/**
 * SQL 任务参数
 *
 * @author jinmu
 */
@Data
public class SqlTaskParams {

    private String datasourceCode;

    /**
     * SQL 类型：QUERY 查询（有结果集）/ NON_QUERY 非查询（DDL、update/delete/insert）
     */
    private String sqlType;

    private String sqlText;

    /**
     * 前置 SQL（主 SQL 前执行，如 SET 会话变量、use db）
     */
    private String preSql;

    /**
     * 后置 SQL（主 SQL 后执行，如清理）
     */
    private String postSql;

    /**
     * SQL 参数，格式 key1=value1;key2=value2，SQL 内用 ${key} 占位替换
     */
    private String sqlParams;
}
