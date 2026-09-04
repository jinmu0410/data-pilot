package cn.dataplatform.open.web.vo.data.service;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据服务-调用日志详情
 *
 * @author jinmu
 * @date 2025/3/15
 * @since 1.0.0
 */
@Data
public class QueryLogDetailResponse {

    private Long id;

    private String workspaceCode;

    private String templateCode;

    private String templateName;

    private String requestArg;

    private String responseArg;

    private String requestId;

    private String method;

    private String exception;

    private Long cost;

    private Long number;

    private String hitCache;

    private String ip;

    private String status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
