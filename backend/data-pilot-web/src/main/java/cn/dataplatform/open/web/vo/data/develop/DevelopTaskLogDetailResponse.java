package cn.dataplatform.open.web.vo.data.develop;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据研发-SQL 运行记录详情
 *
 * @author jinmu
 * @date 2025/1/4
 * @since 1.0.0
 */
@Data
public class DevelopTaskLogDetailResponse {

    private Long id;

    private Long taskId;

    private String taskCode;

    private String triggerType;

    private String status;

    private String sqlText;

    private Long durationMs;

    private Long rowCount;

    /**
     * 列名
     */
    private List<String> columns;

    /**
     * 预览行（每行按列名顺序的字符串值）
     */
    private List<List<String>> rows;

    private Boolean truncated;

    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
