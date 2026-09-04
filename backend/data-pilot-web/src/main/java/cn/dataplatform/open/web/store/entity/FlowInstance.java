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
 * 任务流实例（append-only，createTime 手动 set）
 *
 * @author jinmu
 */
@TableName("flow_instance")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class FlowInstance implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long flowId;

    private String flowCode;

    private String workspaceCode;

    /**
     * MANUAL/CRON
     */
    private String triggerType;

    /**
     * 失败策略 CONTINUE/END
     */
    private String failureStrategy;

    /**
     * RUNNING/SUCCESS/FAIL
     */
    private String status;

    private String errorMsg;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private Long durationMs;

    private LocalDateTime createTime;
}
