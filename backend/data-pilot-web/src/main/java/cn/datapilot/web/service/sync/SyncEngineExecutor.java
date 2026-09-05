package cn.datapilot.web.service.sync;

import cn.datapilot.common.exception.ApiException;
import cn.datapilot.web.service.SystemConfigService;
import cn.datapilot.web.service.sync.engine.SyncEngine;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 同步引擎子进程执行器：写配置 + subprocess 执行 + 采集日志
 *
 * @author jinmu
 */
@Slf4j
@Service
public class SyncEngineExecutor {

    @Resource
    private SystemConfigService systemConfigService;

    private String workDir() {
        return this.systemConfigService.getValue("sync.work-dir", "/tmp/dp-sync");
    }

    private int timeoutSeconds() {
        String value = this.systemConfigService.getValue("sync.timeout-seconds", "600");
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 600;
        }
    }

    /**
     * 执行引擎，返回退出码与日志；引擎未安装时抛异常
     */
    public ExecuteResult execute(SyncEngine engine, String configContent) throws IOException, InterruptedException {
        return this.execute(engine, configContent, null);
    }

    public ExecuteResult execute(SyncEngine engine, String configContent, String logPathStr) throws IOException, InterruptedException {
        return this.execute(engine, configContent, logPathStr, null);
    }

    public ExecuteResult execute(SyncEngine engine, String configContent, String logPathStr, Integer taskTimeoutSeconds)
            throws IOException, InterruptedException {
        Path dir = Paths.get(this.workDir());
        Files.createDirectories(dir);
        String token = UUID.fastUUID().toString(true);
        Path configPath = dir.resolve(token + "-" + engine.configFileName());
        Path logPath = StrUtil.isNotBlank(logPathStr) ? Paths.get(logPathStr) : dir.resolve(token + ".log");
        Files.writeString(configPath, configContent, StandardCharsets.UTF_8);

        List<String> command = engine.buildCommand(configPath.toString());
        String executable = command.get(0);
        if (!Files.exists(Paths.get(executable))) {
            ExecuteResult result = new ExecuteResult();
            result.setExitCode(-1);
            result.setSuccess(false);
            result.setErrorMsg("引擎未安装: " + executable + "，当前仅支持生成配置");
            result.setLog("【引擎未安装】" + executable + "，以下为生成的配置（未实际执行）：\n\n" + configContent);
            return result;
        }

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.redirectOutput(logPath.toFile());
        Process process = pb.start();
        boolean finished;
        try {
            int effectiveTimeout = taskTimeoutSeconds == null || taskTimeoutSeconds < 1
                    ? this.timeoutSeconds() : taskTimeoutSeconds;
            finished = process.waitFor(effectiveTimeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw e;
        }
        if (!finished) {
            process.destroyForcibly();
            int effectiveTimeout = taskTimeoutSeconds == null || taskTimeoutSeconds < 1
                    ? this.timeoutSeconds() : taskTimeoutSeconds;
            throw new ApiException("同步执行超时(>" + effectiveTimeout + "s)，已终止");
        }

        int exitCode = process.exitValue();
        String output = Files.exists(logPath) ? Files.readString(logPath, StandardCharsets.UTF_8) : "";
        ExecuteResult result = new ExecuteResult();
        result.setExitCode(exitCode);
        result.setLog(output);
        result.setSuccess(exitCode == 0);
        return result;
    }

    @Data
    public static class ExecuteResult {
        private int exitCode;
        private String log;
        private String errorMsg;
        private boolean success;
    }
}
