package cn.dataplatform.open.web.vo.data.flow;

import cn.dataplatform.open.web.vo.data.task.TaskInstanceDetailResponse;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务流实例详情（内联各节点的 task_instance 记录）
 *
 * @author dingqianwen
 */
@Data
public class FlowInstanceDetailResponse {

    private Long id;

    private Long flowId;

    private String flowCode;

    private String flowName;

    private String triggerType;

    private String failureStrategy;

    private String status;

    private String errorMsg;

    private Long durationMs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    private List<TaskInstanceDetailResponse> nodes;
}
