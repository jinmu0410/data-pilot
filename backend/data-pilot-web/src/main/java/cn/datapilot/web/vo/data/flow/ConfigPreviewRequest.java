package cn.datapilot.web.vo.data.flow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 同步引擎配置预览请求
 *
 * @author jinmu
 */
@Data
public class ConfigPreviewRequest {

    @NotBlank
    private String taskType;

    private TaskFlowNode node;
}
