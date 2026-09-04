package cn.datapilot.web.vo.data.sync;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 数据集成-同步任务更新
 *
 * @author jinmu
 */
@Data
public class SyncTaskUpdateRequest {

    @NotNull
    private Long id;

    @NotBlank
    @Size(max = 200)
    private String name;

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

    private List<SyncFieldMapping> fieldMapping;

    private String status;

    private String description;
}
