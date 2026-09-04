package cn.dataplatform.open.web.vo.data.service;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 数据服务-SQL 执行结果（测试/预览与对外调用共用）
 *
 * @author jinmu
 * @date 2025/2/2
 * @since 1.0.0
 */
@Data
public class QueryExecuteResult {

    /**
     * 列名
     */
    private List<String> columns;

    /**
     * 行数据（列名 → 值）
     */
    private List<Map<String, Object>> rows;

    private Long rowCount;

    private Boolean truncated;

    private Long durationMs;

}
