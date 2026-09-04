package cn.dataplatform.open.web.vo.data.task;

import cn.dataplatform.open.web.service.task.model.SyncTaskParams;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 统一任务详情（扁平化，按 taskType 填充对应字段）
 *
 * @author dingqianwen
 */
@Data
public class TaskDetailResponse {

    private Long id;

    private String name;

    private String code;

    private String taskType;

    private String cron;

    private Integer timeout;

    private String status;

    private String description;

    /**
     * SQL
     */
    private String datasourceCode;

    private String sqlText;

    /**
     * DATAX/SEATUNNEL
     */
    private String sourceDataSourceCode;

    private String sourceSchema;

    private String sourceTable;

    private String targetDataSourceCode;

    private String targetSchema;

    private String targetTable;

    private List<SyncTaskParams.FieldMapping> fieldMapping;

    /**
     * PYTHON/SHELL
     */
    private String script;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextExecTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
