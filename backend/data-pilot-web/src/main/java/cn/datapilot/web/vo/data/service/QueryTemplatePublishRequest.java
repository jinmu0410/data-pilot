package cn.datapilot.web.vo.data.service;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 数据服务-查询模板发布
 *
 * @author jinmu
 * @date 2025/2/2
 * @since 1.0.0
 */
@Data
public class QueryTemplatePublishRequest {

    @NotNull
    private Long id;

    /**
     * 对外调用密钥，空=公开
     */
    private String secret;

    /**
     * PUBLIC/API_KEY/HMAC_SHA256
     */
    private String authType;

    /**
     * ENABLE/DISABLE
     */
    private String enableCache;

    /**
     * ENABLE/DISABLE
     */
    private String enableLimiting;

    /**
     * GLOBAL/IP
     */
    private String limitType;

    private Integer limitRate;

    private Integer limitRefreshInterval;

    /**
     * SECONDS/MINUTES/HOURS
     */
    private String limitTimeUnit;

    /**
     * ENABLE/DISABLE
     */
    private String recordLog;

}
