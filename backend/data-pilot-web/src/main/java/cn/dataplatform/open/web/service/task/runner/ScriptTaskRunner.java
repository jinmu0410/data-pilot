package cn.dataplatform.open.web.service.task.runner;

import cn.dataplatform.open.web.service.task.SubprocessExecutor;
import cn.dataplatform.open.web.service.task.model.ScriptTaskParams;
import com.alibaba.fastjson2.JSON;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * 脚本任务执行器（PYTHON/SHELL）
 *
 * @author dingqianwen
 */
@Slf4j
@Component
public class ScriptTaskRunner implements TaskRunner {

    @Resource
    private SubprocessExecutor subprocessExecutor;

    @Override
    public Set<String> types() {
        return Set.of("PYTHON", "SHELL");
    }

    @Override
    public TaskRunResult run(TaskRunContext context) {
        ScriptTaskParams params = JSON.parseObject(context.getTaskParams(), ScriptTaskParams.class);
        TaskRunResult result = new TaskRunResult();
        try {
            boolean python = "PYTHON".equalsIgnoreCase(context.getTaskType());
            String ext = python ? ".py" : ".sh";
            Path scriptPath = this.subprocessExecutor.writeFile(ext, params.getScript());
            int timeout = context.getTimeout() == null ? 30 : context.getTimeout();
            // python 用 -u 关闭输出缓冲，保证实时日志流
            List<String> command = python
                    ? List.of("python3", "-u", scriptPath.toString())
                    : List.of("bash", scriptPath.toString());
            SubprocessExecutor.Result r = this.subprocessExecutor.run(command, timeout, context.getLogPath());
            result.setLogContent(r.getLog());
            if (r.isSuccess()) {
                result.setStatus("SUCCESS");
            } else {
                result.setStatus("FAIL");
                result.setErrorMsg("退出码 " + r.getExitCode());
            }
        } catch (Exception e) {
            if (Thread.currentThread().isInterrupted()) {
                result.setStatus("SKIP");
                result.setErrorMsg("任务被终止");
            } else {
                log.error("脚本任务执行失败", e);
                result.setStatus("FAIL");
                result.setErrorMsg(this.rootMessage(e));
            }
        }
        return result;
    }

    private String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }
}
