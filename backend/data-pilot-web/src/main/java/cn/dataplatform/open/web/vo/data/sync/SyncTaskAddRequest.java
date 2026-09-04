package cn.dataplatform.open.web.vo.data.sync;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 数据集成-同步任务新增
 *
 * @author dingqianwen
 */
@Data
public class SyncTaskAddRequest {

    @NotBlank
    @Size(max = 200)
    private String name;

    /**
     * DATAX/SEATUNNEL
     */
    @NotBlank
    private String engine;

    @NotBlank
    private String sourceDataSourceCode;

    private String sourceSchema;

    @NotBlank
    private String sourceTable;

    @NotBlank
    private String targetDataSourceCode;

    private String targetSchema;

    @NotBlank
    private String targetTable;

    /**
     * 字段映射，空=全字段
     */
    private List<SyncFieldMapping> fieldMapping;

    private String status;

    private String description;
}
