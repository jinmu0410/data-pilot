package cn.dataplatform.open.web.vo.data.service;

import lombok.Data;

import java.util.Map;

/**
 * 数据服务-对外调用请求体
 *
 * @author jinmu
 * @date 2025/2/2
 * @since 1.0.0
 */
@Data
public class QueryCallRequest {

    /**
     * one/count/list/page，默认 list
     */
    private String method = "list";

    private Map<String, Object> params;

    private Integer pageNum = 1;

    private Integer pageSize = 10;

}
