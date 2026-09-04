package cn.dataplatform.open.web.service.task;

import cn.dataplatform.open.common.exception.ApiException;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 通用子进程执行器：写文件 + subprocess 执行 + 采集日志
 *
 * @author jinmu
 */
@Slf4j
@Service
public class SubprocessExecutor {

    @Value("${dp.task.work-dir:/tmp/dp-task}")
    private String workDir;

    /**
     * 将内容写入工作目录并返回绝对路径
     */
    public Path writeFile(String suffix, String content) throws IOException {
        Path dir = Paths.get(workDir);
        Files.createDirectories(dir);
        String token = UUID.fastUUID().toString(true);
        Path path = dir.resolve(token + suffix);
        Files.writeString(path, content, StandardCharsets.UTF_8);
        return path;
    }

    /**
     * 执行命令，限时等待，返回退出码与日志
     */
    public Result run(List<String> command, int timeoutSeconds) throws IOException, InterruptedException {
        return this.run(command, timeoutSeconds, null);
    }

    public Result run(List<String> command, int timeoutSeconds, String logPathStr) throws IOException, InterruptedException {
        Path dir = Paths.get(workDir);
        Files.createDirectories(dir);
        String token = UUID.fastUUID().toString(true);
        Path logPath = StrUtil.isNotBlank(logPathStr) ? Paths.get(logPathStr) : dir.resolve(token + ".log");

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        pb.redirectOutput(logPath.toFile());
        Process process = pb.start();
        boolean finished;
        try {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw e;
        }
        if (!finished) {
            process.destroyForcibly();
            throw new ApiException("任务执行超时(>" + timeoutSeconds + "s)，已终止");
        }

        int exitCode = process.exitValue();
        String output = Files.exists(logPath) ? Files.readString(logPath, StandardCharsets.UTF_8) : "";
        Result result = new Result();
        result.setExitCode(exitCode);
        result.setLog(output);
        result.setSuccess(exitCode == 0);
        return result;
    }

    @Data
    public static class Result {
        private int exitCode;
        private String log;
        private boolean success;
    }
}
