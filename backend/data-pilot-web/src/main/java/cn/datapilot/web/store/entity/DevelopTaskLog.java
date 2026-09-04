package cn.datapilot.web.store.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据研发-SQL 运行记录
 *
 * @author jinmu
 * @date 2025/1/4
 * @since 1.0.0
 */
@TableName("develop_task_log")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class DevelopTaskLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String taskCode;

    private String workspaceCode;

    /**
     * MANUAL/CRON
     */
    private String triggerType;

    /**
     * RUNNING/SUCCESS/FAIL
     */
    private String status;

    private String sqlText;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long durationMs;

    private Long rowCount;

    /**
     * 列名 + 前 N 行
     */
    private String preview;

    private String errorMsg;

    private LocalDateTime createTime;

}
