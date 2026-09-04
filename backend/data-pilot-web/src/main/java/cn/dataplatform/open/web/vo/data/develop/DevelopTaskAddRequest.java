package cn.dataplatform.open.web.vo.data.develop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 数据研发-SQL 任务新增
 *
 * @author dingqianwen
 * @date 2025/1/4
 * @since 1.0.0
 */
@Data
public class DevelopTaskAddRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    @NotBlank
    private String datasourceCode;

    @NotBlank
    private String sqlText;

    private String cron;

    private Integer timeout;

    private String status;

    private String description;

}
