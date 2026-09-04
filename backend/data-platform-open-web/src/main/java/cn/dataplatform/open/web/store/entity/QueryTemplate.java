package cn.dataplatform.open.web.store.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据服务-查询模板
 *
 * @author dingqianwen
 * @date 2025/2/2
 * @since 1.0.0
 */
@TableName("query_template")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class QueryTemplate implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    /**
     * SQL 模板，支持 ${param} 占位符
     */
    private String template;

    private String workspaceCode;

    /**
     * ENABLE/DISABLE
     */
    private String status;

    private String description;

    private String dataSourceCode;

    /**
     * 查询超时(秒)
     */
    private Integer timeout;

    /**
     * 对外调用密钥，空=公开
     */
    private String secret;

    private String currentVersion;

    private String publishVersion;

    private Long createUserId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;

}
