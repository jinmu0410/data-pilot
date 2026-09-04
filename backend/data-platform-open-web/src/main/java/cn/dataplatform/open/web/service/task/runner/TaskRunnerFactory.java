package cn.dataplatform.open.web.service.task.runner;

import cn.dataplatform.open.common.exception.ApiException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 按任务类型分发执行器
 *
 * @author dingqianwen
 */
@Component
public class TaskRunnerFactory {

    @Resource
    private List<TaskRunner> runners;

    public TaskRunner get(String taskType) {
        if (taskType == null) {
            throw new ApiException("任务类型不能为空");
        }
        return runners.stream()
                .filter(r -> r.types().stream().anyMatch(t -> t.equalsIgnoreCase(taskType)))
                .findFirst()
                .orElseThrow(() -> new ApiException("不支持的任务类型: " + taskType));
    }
}
