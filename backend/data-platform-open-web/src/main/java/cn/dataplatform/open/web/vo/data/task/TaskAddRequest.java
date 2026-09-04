package cn.dataplatform.open.web.vo.data.task;

import cn.dataplatform.open.web.service.task.model.SyncTaskParams;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 统一任务新增（按 taskType 填对应字段，其余留空）
 *
 * @author dingqianwen
 */
@Data
public class TaskAddRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    /**
     * SQL/DATAX/SEATUNNEL/PYTHON/SHELL
     */
    @NotBlank
    private String taskType;

    /**
     * SQL 数据源
     */
    private String datasourceCode;

    private String sqlText;

    /**
     * DATAX/SEATUNNEL 源/目标
     */
    private String sourceDataSourceCode;

    private String sourceSchema;

    private String sourceTable;

    private String targetDataSourceCode;

    private String targetSchema;

    private String targetTable;

    private List<SyncTaskParams.FieldMapping> fieldMapping;

    /**
     * PYTHON/SHELL 脚本
     */
    private String script;

    private String cron;

    private Integer timeout;

    private String status;

    private String description;
}
