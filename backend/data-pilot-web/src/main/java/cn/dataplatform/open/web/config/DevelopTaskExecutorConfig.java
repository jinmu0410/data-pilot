package cn.dataplatform.open.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 数据研发-SQL 任务执行线程池
 *
 * @author dingqianwen
 */
@Configuration
public class DevelopTaskExecutorConfig {

    @Bean(name = "developTaskExecutor")
    public ThreadPoolTaskExecutor developTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setThreadNamePrefix("develop-task_");
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

}
