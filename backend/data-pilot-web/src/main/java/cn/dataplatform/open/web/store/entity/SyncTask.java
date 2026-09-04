package cn.dataplatform.open.web.store.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据集成-同步任务
 *
 * @author jinmu
 * @date 2025/1/4
 * @since 1.0.0
 */
@TableName("sync_task")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class SyncTask implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String workspaceCode;

    /**
     * DATAX/SEATUNNEL
     */
    private String engine;

    private String sourceDataSourceCode;

    private String sourceSchema;

    private String sourceTable;

    private String targetDataSourceCode;

    private String targetSchema;

    private String targetTable;

    /**
     * 字段映射 JSON [{source,target}]，空=全字段
     */
    private String fieldMapping;

    /**
     * ENABLE/DISABLE
     */
    private String status;

    private String description;

    private Long createUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

}
