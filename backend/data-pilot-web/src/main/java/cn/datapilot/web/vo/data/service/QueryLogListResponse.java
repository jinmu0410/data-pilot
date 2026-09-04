package cn.datapilot.web.vo.data.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据服务-调用日志列表项
 *
 * @author jinmu
 * @date 2025/3/15
 * @since 1.0.0
 */
@Data
public class QueryLogListResponse {

    private Long id;

    private String templateCode;

    private String templateName;

    private String method;

    private String status;

    private Long cost;

    private Long number;

    private String hitCache;

    private String ip;

    private String requestId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
