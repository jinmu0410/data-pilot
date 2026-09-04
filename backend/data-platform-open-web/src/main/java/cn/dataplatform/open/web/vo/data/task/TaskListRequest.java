package cn.dataplatform.open.web.vo.data.task;

import lombok.Data;

/**
 * 统一任务列表查询
 *
 * @author dingqianwen
 */
@Data
public class TaskListRequest {

    private String keyword;

    private String taskType;

    private String status;
}
