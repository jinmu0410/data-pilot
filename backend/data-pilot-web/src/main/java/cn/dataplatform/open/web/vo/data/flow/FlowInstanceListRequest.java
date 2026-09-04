package cn.dataplatform.open.web.vo.data.flow;

import lombok.Data;

/**
 * 任务流实例列表查询
 *
 * @author dingqianwen
 */
@Data
public class FlowInstanceListRequest {

    private Long flowId;

    private String keyword;

    /**
     * RUNNING/SUCCESS/FAIL
     */
    private String status;

    /**
     * MANUAL/CRON
     */
    private String triggerType;
}
