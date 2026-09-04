package cn.dataplatform.open.common.util;

import cn.dataplatform.open.common.exception.ParallelException;
import cn.hutool.core.collection.CollUtil;
import org.slf4j.MDC;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/4/24
 * @since 1.0.0
 */
public class ParallelStreamUtils {

    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    /**
     * 虚拟线程（Virtual Thread）名称前缀，主要用于高效处理 I/O 密集型任务
     */
    public static final String VIRTUAL_THREAD_PREFIX = "parallel-vt-";
    /**
     * 平台物理线程（Platform Thread）名称前缀，主要用于处理计算/CPU 密集型任务
     */
    public static final String PLATFORM_THREAD_PREFIX = "parallel-pt-";

    /**
     * IO密集
     */
    private static final ExecutorService VIRTUAL_EXECUTOR = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                    .name(VIRTUAL_THREAD_PREFIX, 1)
                    .factory()
    );
    /**
     * CPU密集
     */
    private static final ExecutorService PLATFORM_EXECUTOR = new ThreadPoolExecutor(
            Math.max(8, CPU_CORES),
            Math.max(16, CPU_CORES * 2),
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            Thread.ofPlatform()
                    .name(PLATFORM_THREAD_PREFIX, 1)
                    .factory(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 执行并行流操作-使用虚拟线程，处理IO 密集任务
     *
     * @param list   数据列表
     * @param action 操作
     * @param <T>    组件类型
     */
    public static <T> void forEach(Collection<T> list, Consumer<T> action) {
        ParallelStreamUtils.forEach(list, action, true);
    }

    /**
     * 执行并行流操作
     *
     * @param list          数据列表
     * @param action        操作
     * @param <T>           组件类型
     * @param virtualThread 是否使用虚拟线程 true：IO 密集，false：CPU 密集
     */
    public static <T> void forEach(Collection<T> list, Consumer<T> action, boolean virtualThread) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        // 如果只有一个元素，不用多线程执行
        if (list.size() == 1) {
            T only = list.iterator().next();
            action.accept(only);
            return;
        }
        List<Future<?>> futures = ParallelStreamUtils.getFutures(list, action, virtualThread);
        try {
            // 等待所有任务完成
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (InterruptedException e) {
            // 取消其他运行中的Future
            FutureUtils.cancelFutures(futures);
            Thread.currentThread().interrupt();
            throw new ParallelException("并行处理被中断", e);
        } catch (ExecutionException e) {
            // 取消其他运行中的Future
            FutureUtils.cancelFutures(futures);
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException r) {
                throw r;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new ParallelException("并行处理失败", cause);
        }
    }

    /**
     * 获取Future列表
     *
     * @param list          数据列表
     * @param action        操作
     * @param virtualThread 是否使用虚拟线程
     * @param <T>           组件类型
     * @return Future列表
     */
    @NonNull
    private static <T> List<Future<?>> getFutures(Collection<T> list, Consumer<T> action,
                                                  boolean virtualThread) {
        ExecutorService executor = virtualThread ? VIRTUAL_EXECUTOR : PLATFORM_EXECUTOR;
        List<Future<?>> futures = new ArrayList<>(list.size());
        Map<String, String> copyOfContextMap = MDC.getCopyOfContextMap();
        for (T component : list) {
            Future<?> future = executor.submit(() -> {
                try {
                    if (copyOfContextMap != null) {
                        MDC.setContextMap(copyOfContextMap);
                    }
                    action.accept(component);
                } finally {
                    MDC.clear();
                }
            });
            futures.add(future);
        }
        return futures;
    }

}
