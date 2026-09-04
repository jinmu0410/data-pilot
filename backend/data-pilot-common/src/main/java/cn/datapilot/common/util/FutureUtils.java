package cn.datapilot.common.util;

import cn.hutool.core.collection.CollUtil;
import lombok.SneakyThrows;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * 一句话功能简述
 *
 * @author dingqw
 * @date 2026/3/29
 * @since 1.0.0
 */
public class FutureUtils {


    /**
     * 等待Future完成，并正确处理异常
     *
     * @param future 待等待的Future对象
     */
    @SneakyThrows
    public static void awaitFuture(Future<?> future) {
        if (future == null) {
            return;
        }
        try {
            future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw e;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception ex) {
                throw ex;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw e;
        }
    }

    /**
     * 取消Future
     *
     * @param futures Future列表
     */
    public static void cancelFutures(List<Future<?>> futures) {
        if (CollUtil.isEmpty(futures)) {
            return;
        }
        for (Future<?> future : futures) {
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }
    }

}
