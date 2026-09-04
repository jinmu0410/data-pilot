package cn.dataplatform.open.web.vo.data.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据集成-同步运行实例详情
 *
 * @author jinmu
 */
@Data
public class SyncTaskLogDetailResponse {

    private Long id;

    private Long taskId;

    private String taskCode;

    private String engine;

    private String status;

    private String triggerType;

    private String configContent;

    private String logContent;

    private String errorMsg;

    private Long durationMs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
