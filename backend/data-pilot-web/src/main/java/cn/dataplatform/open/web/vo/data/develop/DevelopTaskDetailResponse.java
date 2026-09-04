package cn.dataplatform.open.web.vo.data.develop;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据研发-SQL 任务详情
 *
 * @author jinmu
 * @date 2025/1/4
 * @since 1.0.0
 */
@Data
public class DevelopTaskDetailResponse {

    private Long id;

    private String name;

    private String code;

    private String datasourceCode;

    private String sqlText;

    private String cron;

    private Integer timeout;

    private String status;

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime nextExecTime;

    private Long createUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
