package cn.datapilot.web.vo.dashboard.base;

import cn.datapilot.web.vo.user.UserData;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/3/1
 * @since 1.0.0
 */
@Data
public class LoginLogResponse {

    private Long id;

    private UserData user;

    private String ip;
    private String platform;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

}
