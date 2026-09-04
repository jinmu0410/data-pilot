package cn.datapilot.web.vo.data.source;

import cn.datapilot.common.enums.DataSourceType;
import cn.datapilot.common.enums.Status;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/1/3
 * @since 1.0.0
 */
@Data
public class DataSourceDetailResponse {

    private Long id;

    private String name;

    private String code;

    /**
     * MySQL Doris StarRocks Oracle等
     *
     * @see DataSourceType
     */
    private String type;

    private String url;

    private String username;

    private String password;

    private String driver;

    private Integer maxPoolSize;

    /**
     * 启用,禁用
     *
     * @see Status
     */
    private String status;

    private String feNodes;
    private String beNodes;
    /**
     * 分表规则
     */
    private String partitioningAlgorithm;

    private String healthCheck;

    private Object maskColumn;

    private String description;

    private Long createUserId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

}
