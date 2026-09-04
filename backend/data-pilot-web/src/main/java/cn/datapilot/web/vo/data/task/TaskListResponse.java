package cn.datapilot.web.vo.data.task;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一任务列表项
 *
 * @author jinmu
 */
@Data
public class TaskListResponse {

    private Long id;

    private String name;

    private String code;

    private String taskType;

    private String cron;

    private Integer timeout;

    private String status;

    private String description;

    /**
     * 摘要：SQL=数据源名；同步=源表 → 目标表；脚本=null
     */
    private String summary;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextExecTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
