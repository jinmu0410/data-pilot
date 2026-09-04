package cn.dataplatform.open.web.vo.data.task;

import lombok.Data;

import java.util.List;

/**
 * 同步引擎生成的配置与命令
 *
 * @author dingqianwen
 */
@Data
public class TaskConfigResponse {

    private String engine;

    private String configContent;

    private List<String> command;
}
