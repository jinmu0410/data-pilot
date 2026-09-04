package cn.dataplatform.open.web.vo.data.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一任务实例列表项
 *
 * @author dingqianwen
 */
@Data
public class TaskInstanceListResponse {

    private Long id;

    private Long taskId;

    private String taskCode;

    private String taskName;

    private String taskType;

    private String triggerType;

    private String status;

    private Long durationMs;

    private Long rowCount;

    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
