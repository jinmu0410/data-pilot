package cn.dataplatform.open.web.vo.data.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据集成-同步任务列表项
 *
 * @author dingqianwen
 */
@Data
public class SyncTaskListResponse {

    private Long id;

    private String name;

    private String code;

    private String engine;

    private String sourceDataSourceCode;

    private String sourceDataSourceName;

    private String sourceSchema;

    private String sourceTable;

    private String targetDataSourceCode;

    private String targetDataSourceName;

    private String targetSchema;

    private String targetTable;

    private String status;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
