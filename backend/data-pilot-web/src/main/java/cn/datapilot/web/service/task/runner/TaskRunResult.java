package cn.datapilot.web.service.task.runner;

import lombok.Data;

import java.util.List;

/**
 * 任务执行结果
 *
 * @author jinmu
 */
@Data
public class TaskRunResult {

    /**
     * SUCCESS/FAIL
     */
    private String status;

    private List<String> columns;

    private List<List<String>> rows;

    private boolean truncated;

    private Long rowCount;

    private String logContent;

    private String errorMsg;
}
