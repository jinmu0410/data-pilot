package cn.dataplatform.open.web.vo.data.develop;

import lombok.Data;

/**
 * 数据研发-SQL 任务列表查询
 *
 * @author dingqianwen
 * @date 2025/1/4
 * @since 1.0.0
 */
@Data
public class DevelopTaskListRequest {

    /**
     * 关键字（匹配名称或编码，模糊）
     */
    private String keyword;

    private String name;

    private String datasourceCode;

    private String status;

}
