package cn.dataplatform.open.web.vo.data.develop;

import lombok.Data;

/**
 * 数据研发-SQL 运行记录列表查询
 *
 * @author dingqianwen
 * @date 2025/1/4
 * @since 1.0.0
 */
@Data
public class DevelopTaskLogListRequest {

    private Long taskId;

    /**
     * 任务名称关键字（模糊匹配任务名称）
     */
    private String keyword;

    /**
     * 运行状态 SUCCESS/FAIL
     */
    private String status;

    /**
     * 触发类型 MANUAL/CRON
     */
    private String triggerType;

}
