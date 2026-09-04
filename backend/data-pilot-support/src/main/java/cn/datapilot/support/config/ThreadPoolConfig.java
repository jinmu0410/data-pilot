package cn.datapilot.support.config;

import cn.datapilot.common.config.ThreadPoolTaskExecutorBeanPostProcessor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * 线程池
 *
 * @author 丁乾文
 * @date 2021/6/17
 * @since 1.0.0
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    public static final String VIRTUAL_EXECUTOR = "virtualExecutor";

    @Resource
    @Lazy
    private ThreadPoolTaskExecutorBeanPostProcessor.TaskDecoratorProxy taskDecoratorProxy;

    /**
     * 虚拟线程池
     *
     * @return 虚拟线程池
     */
    @Bean(name = VIRTUAL_EXECUTOR)
    public AsyncTaskExecutor virtualExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setTaskDecorator(this.taskDecoratorProxy.getTaskDecorator(null));
        // 开启虚拟线程模式
        executor.setVirtualThreads(true);
        return executor;
    }

}
