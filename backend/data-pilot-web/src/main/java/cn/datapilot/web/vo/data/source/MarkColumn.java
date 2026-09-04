package cn.datapilot.web.vo.data.source;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class MarkColumn {

    @NotEmpty
    private String columnName;

    /**
     * @see cn.datapilot.common.enums.MaskType
     */
    @NotEmpty
    private String maskType;

}
