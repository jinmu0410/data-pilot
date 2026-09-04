package cn.datapilot.web.vo.data.develop;

import lombok.Data;

import java.util.List;

/**
 * 数据研发-SQL 运行结果
 *
 * @author jinmu
 * @date 2025/1/4
 * @since 1.0.0
 */
@Data
public class DevelopRunResponse {

    /**
     * 已保存任务的运行记录 ID，临时运行时为 null
     */
    private Long logId;

    private String status;

    private List<String> columns;

    private List<List<String>> rows;

    private Long rowCount;

    private Boolean truncated;

    private Long durationMs;

    private String error;

}
