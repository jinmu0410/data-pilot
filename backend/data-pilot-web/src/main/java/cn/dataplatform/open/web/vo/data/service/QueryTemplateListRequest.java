package cn.dataplatform.open.web.vo.data.service;

import lombok.Data;

/**
 * 数据服务-查询模板列表查询
 *
 * @author jinmu
 * @date 2025/2/2
 * @since 1.0.0
 */
@Data
public class QueryTemplateListRequest {

    /**
     * 关键字（匹配名称或编码，模糊）
     */
    private String keyword;

    private String dataSourceCode;

    private String status;

}
