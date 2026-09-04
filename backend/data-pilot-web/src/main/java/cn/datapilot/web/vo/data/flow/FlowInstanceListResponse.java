package cn.datapilot.web.vo.data.flow;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 任务流实例列表项
 *
 * @author jinmu
 */
@Data
public class FlowInstanceListResponse {

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
}
