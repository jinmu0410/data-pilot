package cn.datapilot.web.vo.data.service;

import lombok.Data;

/**
 * 数据服务-调用日志列表查询
 *
 * @author jinmu
 * @date 2025/3/15
 * @since 1.0.0
 */
@Data
public class QueryLogListRequest {

    private String templateCode;

    private String status;

}
