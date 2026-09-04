package cn.dataplatform.open.web.vo.data.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据服务-查询模板详情
 *
 * @author dingqianwen
 * @date 2025/2/2
 * @since 1.0.0
 */
@Data
public class QueryTemplateDetailResponse {

    private Long id;

    private String name;

    private String code;

    private String template;

    private String dataSourceCode;

    private String dataSourceName;

    private String status;

    private Integer timeout;

    private String secret;

    private String currentVersion;

    private String publishVersion;

    private String description;

    private Long createUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
