package cn.dataplatform.open.web.store.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据服务-查询模板发布快照
 *
 * @author jinmu
 * @date 2025/2/2
 * @since 1.0.0
 */
@TableName("query_template_publish")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class QueryTemplatePublish implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private String template;

    private String workspaceCode;

    private String status;

    private String description;

    private String dataSourceCode;

    private String secret;

    private Integer timeout;

    /**
     * ENABLE/DISABLE
     */
    private String enableCache;

    /**
     * ENABLE/DISABLE
     */
    private String enableLimiting;

    private Integer limitRate;

    private Integer limitRefreshInterval;

    private String limitTimeUnit;

    /**
     * ENABLE/DISABLE
     */
    private String recordLog;

    private String version;

    private Long createUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

}
