package cn.datapilot.web.vo.data.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据服务-查询模板发布历史项
 *
 * @author jinmu
 * @date 2025/2/2
 * @since 1.0.0
 */
@Data
public class QueryTemplatePublishResponse {

    private Long id;

    private String name;

    private String code;

    private String version;

    private String template;

    private String dataSourceCode;

    private String status;

    private String secret;

    private String authType;

    private Integer timeout;

    private String enableCache;

    private String enableLimiting;

    private String limitType;

    private Integer limitRate;

    private Integer limitRefreshInterval;

    private String limitTimeUnit;

    private String recordLog;

    private Long createUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
