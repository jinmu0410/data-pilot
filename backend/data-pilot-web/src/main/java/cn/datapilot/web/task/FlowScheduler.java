package cn.datapilot.web.task;

import cn.datapilot.web.service.flow.DataFlowService;
import cn.datapilot.web.service.flow.FlowRunService;
import cn.datapilot.web.store.entity.DataFlow;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 任务流调度器：扫描启用且配置了 cron 的任务流，到点触发整条 DAG
 *
 * @author jinmu
 */
@Slf4j
@Component
public class FlowScheduler {

    private static final String FLOW_LOCK_PREFIX = "dp:flow:lock:";

    @Resource
    private DataFlowService dataFlowService;
    @Resource
    private FlowRunService flowRunService;
    @Resource
    private RedissonClient redissonClient;
    @Resource(name = "dpTaskExecutor")
    private ThreadPoolTaskExecutor dpTaskExecutor;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void start() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "flow-scheduler");
            thread.setDaemon(true);
            return thread;
        });
        this.scheduler.scheduleWithFixedDelay(this::scan, 5, 30, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        if (this.scheduler != null) {
            this.scheduler.shutdownNow();
        }
    }

    /**
     * 每 30 秒扫描一次到期任务流并触发执行
     */
    private void scan() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<DataFlow> flows = this.dataFlowService.listEnabledCronFlows();
            for (DataFlow flow : flows) {
                LocalDateTime nextExecTime = flow.getNextExecTime();
                if (nextExecTime == null) {
                    // 首次初始化调度游标，不立即执行
                    this.dataFlowService.advanceNextExecTime(flow);
                    continue;
                }
                if (nextExecTime.isAfter(now)) {
                    continue;
                }
                // 推进游标，防止下次扫描重复触发
                this.dataFlowService.advanceNextExecTime(flow);
                this.dpTaskExecutor.execute(() -> {
                    RLock lock = this.redissonClient.getLock(FLOW_LOCK_PREFIX + flow.getCode());
                    if (!lock.tryLock()) {
                        return;
                    }
                    try {
                        this.flowRunService.run(flow.getId(), "CRON", "CONTINUE");
                    } catch (Exception e) {
                        log.error("任务流调度执行失败, flowId:{}", flow.getId(), e);
                    } finally {
                        if (lock.isHeldByCurrentThread()) {
                            lock.unlock();
                        }
                    }
                });
            }
        } catch (Exception e) {
            log.error("任务流调度扫描失败", e);
        }
    }
}
