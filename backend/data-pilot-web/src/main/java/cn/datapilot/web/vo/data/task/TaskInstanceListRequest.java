package cn.datapilot.web.vo.data.task;

import lombok.Data;

/**
 * 统一任务实例列表查询
 *
 * @author jinmu
 */
@Data
public class TaskInstanceListRequest {

    private Long taskId;

    private String keyword;

    private String taskType;

    /**
     * RUNNING/SUCCESS/FAIL
     */
    private String status;

    /**
     * MANUAL/CRON
     */
    private String triggerType;
}
