package cn.dataplatform.open.web.store.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据研发-SQL 任务
 *
 * @author dingqianwen
 * @date 2025/1/4
 * @since 1.0.0
 */
@TableName("develop_task")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class DevelopTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String code;

    private String name;

    private String workspaceCode;

    private String datasourceCode;

    /**
     * SQL 语句
     */
    private String sqlText;

    /**
     * cron 表达式，空=仅手动
     */
    private String cron;

    /**
     * 查询超时(秒)
     */
    private Integer timeout;

    /**
     * ENABLE/DISABLE
     */
    private String status;

    private String description;

    /**
     * 下次调度执行时间
     */
    private LocalDateTime nextExecTime;

    private Long createUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

}
