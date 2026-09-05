package cn.datapilot.web.vo.system;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 系统配置新增/修改请求
 *
 * @author jinmu
 */
@Data
public class SystemConfigRequest {

    private Long id;

    @NotBlank
    private String configKey;

    private String configValue;

    private String description;
}
