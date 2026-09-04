package cn.dataplatform.open.web.vo.data.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据集成-同步运行实例列表项
 *
 * @author dingqianwen
 */
@Data
public class SyncTaskLogListResponse {

    private Long id;

    private Long taskId;

    private String taskCode;

    private String taskName;

    private String engine;

    private String status;

    private String triggerType;

    private Long durationMs;

    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
