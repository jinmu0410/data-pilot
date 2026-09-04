package cn.datapilot.web.vo.data.flow;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 手动运行任务流请求
 *
 * @author jinmu
 */
@Data
public class FlowRunRequest {

    @NotNull
    private Long id;

    /**
     * 失败策略 CONTINUE（继续）/ END（结束），空默认 CONTINUE
     */
    private String failureStrategy;

}
