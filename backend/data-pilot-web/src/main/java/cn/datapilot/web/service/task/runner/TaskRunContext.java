package cn.datapilot.web.service.task.runner;

import lombok.Data;

/**
 * 任务执行上下文
 *
 * @author jinmu
 */
@Data
public class TaskRunContext {

    private String taskType;

    /**
     * 类型专属参数 JSON
     */
    private String taskParams;

    /**
     * 超时(秒)
     */
    private Integer timeout;

    private String workspaceCode;

    /**
     * 子进程日志文件路径（用于运行中实时读取）
     */
    private String logPath;
}
