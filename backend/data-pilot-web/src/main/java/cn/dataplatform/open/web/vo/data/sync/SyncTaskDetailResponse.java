package cn.dataplatform.open.web.vo.data.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 数据集成-同步任务详情
 *
 * @author jinmu
 */
@Data
public class SyncTaskDetailResponse {

    private Long id;

    private String name;

    private String code;

    private String engine;

    private String sourceDataSourceCode;

    private String sourceSchema;

    private String sourceTable;

    private String targetDataSourceCode;

    private String targetSchema;

    private String targetTable;

    private List<SyncFieldMapping> fieldMapping;

    private String status;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
