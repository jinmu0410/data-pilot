package cn.dataplatform.open.web.vo.data.service;

import lombok.Data;

import java.util.Map;

/**
 * 数据服务-查询模板测试（id 优先；无 id 时用 dataSourceCode+template 临时执行）
 *
 * @author jinmu
 * @date 2025/2/2
 * @since 1.0.0
 */
@Data
public class QueryTemplateTestRequest {

    private Long id;

    private String dataSourceCode;

    private String template;

    private Map<String, Object> params;

}
