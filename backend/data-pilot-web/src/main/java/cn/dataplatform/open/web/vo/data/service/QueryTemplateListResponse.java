package cn.dataplatform.open.web.vo.data.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据服务-查询模板列表项
 *
 * @author jinmu
 * @date 2025/2/2
 * @since 1.0.0
 */
@Data
public class QueryTemplateListResponse {

    private Long id;

    private String name;

    private String code;

    private String dataSourceCode;

    private String dataSourceName;

    private String status;

    private String currentVersion;

    private String publishVersion;

    private Integer timeout;

    private String description;

    private Long createUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
