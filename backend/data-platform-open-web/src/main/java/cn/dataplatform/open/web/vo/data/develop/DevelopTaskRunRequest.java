package cn.dataplatform.open.web.vo.data.develop;

import lombok.Data;

/**
 * 数据研发-SQL 任务运行（id 优先；无 id 时为临时运行）
 *
 * @author dingqianwen
 * @date 2025/1/4
 * @since 1.0.0
 */
@Data
public class DevelopTaskRunRequest {

    private Long id;

    private String datasourceCode;

    private String sqlText;

}
