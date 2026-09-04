package cn.dataplatform.open.web.vo.data.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一任务实例详情
 *
 * @author dingqianwen
 */
@Data
public class TaskInstanceDetailResponse {

    private Long id;

    private Long taskId;

    private String taskCode;

    private String taskName;

    private String taskType;

    private String nodeId;

    private String nodeName;

    private String triggerType;

    private String status;

    /**
     * 运行时的参数快照 JSON
     */
    private String taskParams;

    private Long durationMs;

    private Long rowCount;

    /**
     * SQL 结果预览列名
     */
    private List<String> columns;

    /**
     * SQL 结果预览行
     */
    private List<List<String>> rows;

    private Boolean truncated;

    private String logContent;

    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
