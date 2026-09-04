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
 * 数据服务-调用日志
 *
 * @author jinmu
 * @date 2025/3/15
 * @since 1.0.0
 */
@TableName("query_log")
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
public class QueryLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String workspaceCode;

    private String templateCode;

    private String templateName;

    /**
     * 请求参数
     */
    private String requestArg;

    /**
     * 响应参数，最长记录 2000 字符
     */
    private String responseArg;

    private String requestId;

    /**
     * one/count/list/page
     */
    private String method;

    /**
     * 异常
     */
    private String exception;

    /**
     * 耗时(毫秒)
     */
    private Long cost;

    /**
     * 查询数量
     */
    private Long number;

    /**
     * 是否命中缓存 YES/NO
     */
    private String hitCache;

    private String ip;

    /**
     * SUCCESS/FAIL
     */
    private String status;

    private LocalDateTime createTime;

}
