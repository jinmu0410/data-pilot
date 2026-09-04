package cn.dataplatform.open.web.vo.data.develop;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据研发-SQL 运行记录列表项
 *
 * @author dingqianwen
 * @date 2025/1/4
 * @since 1.0.0
 */
@Data
public class DevelopTaskLogListResponse {

    private Long id;

    private Long taskId;

    private String taskCode;

    /**
     * 任务名称
     */
    private String taskName;

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
