package cn.dataplatform.open.web.store.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据集成-同步运行实例
 *
 * @author dingqianwen
 * @date 2025/1/4
 * @since 1.0.0
 */
@TableName("sync_task_log")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class SyncTaskLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String taskCode;

    private String workspaceCode;

    /**
     * DATAX/SEATUNNEL
     */
    private String engine;

    /**
     * RUNNING/SUCCESS/FAIL
     */
    private String status;

    /**
     * MANUAL
     */
    private String triggerType;

    /**
     * 生成的引擎配置快照
     */
    private String configContent;

    /**
     * 执行日志 stdout+stderr
     */
    private String logContent;

    private String errorMsg;

    private Long durationMs;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;

}
