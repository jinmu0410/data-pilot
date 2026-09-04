package cn.datapilot.web.vo.data.service;

import lombok.Data;

/**
 * 数据服务-对外调用响应
 *
 * @author jinmu
 * @date 2025/2/2
 * @since 1.0.0
 */
@Data
public class QueryCallResponse {

    /**
     * one/count/list/page
     */
    private String method;

    /**
     * one→Map；count→Long；list→List&lt;Map&gt;；page→{records,total,current,size}
     */
    private Object data;

}
