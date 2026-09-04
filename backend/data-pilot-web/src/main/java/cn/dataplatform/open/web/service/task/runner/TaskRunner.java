package cn.dataplatform.open.web.service.task.runner;

import java.util.Set;

/**
 * 统一任务执行器（类比 DolphinScheduler task plugin）
 *
 * @author jinmu
 */
public interface TaskRunner {

    /**
     * 支持的任务类型集合
     */
    Set<String> types();

    /**
     * 执行任务并返回结果
     */
    TaskRunResult run(TaskRunContext context);
}
