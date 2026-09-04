package cn.datapilot.web.vo.data.task;

import lombok.Data;

import java.util.List;

/**
 * 同步引擎生成的配置与命令
 *
 * @author jinmu
 */
@Data
public class TaskConfigResponse {

    private String engine;

    private String configContent;

    private List<String> command;
}
