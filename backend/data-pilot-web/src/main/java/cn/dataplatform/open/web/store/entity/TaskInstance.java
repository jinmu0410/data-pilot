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
 * 统一任务实例（append-only，createTime 手动 set）
 *
 * @author jinmu
 */
@TableName("task_instance")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class TaskInstance implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long taskId;

    private String taskCode;

    private String workspaceCode;

    private String taskType;

    /**
     * MANUAL/CRON
     */
    private String triggerType;

    /**
     * RUNNING/SUCCESS/FAIL
     */
    private String status;

    /**
     * 所属任务流实例
     */
    private Long flowInstanceId;

    /**
     * 节点 id
     */
    private String nodeId;

    /**
     * 节点名称
     */
    private String nodeName;

    /**
     * 运行时的参数快照
     */
    private String taskParams;

    /**
     * SQL 结果预览 JSON {columns,rows,truncated}
     */
    private String result;

    private Long rowCount;

    /**
     * 子进程 stdout/stderr
     */
    private String logContent;

    /**
     * 子进程日志文件路径（用于运行中实时读取）
     */
    private String logPath;

    private String errorMsg;

    private Long durationMs;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private LocalDateTime createTime;

}
