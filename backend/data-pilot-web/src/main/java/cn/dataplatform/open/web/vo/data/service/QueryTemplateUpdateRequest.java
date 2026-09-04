package cn.dataplatform.open.web.vo.data.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 数据服务-查询模板更新
 *
 * @author dingqianwen
 * @date 2025/2/2
 * @since 1.0.0
 */
@Data
public class QueryTemplateUpdateRequest {

    @NotNull
    private Long id;

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    private String dataSourceCode;

    @NotBlank
    private String template;

    private Integer timeout;

    private String status;

    private String description;

}
