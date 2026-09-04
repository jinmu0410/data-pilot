package cn.dataplatform.open.web.service.develop;

import cn.dataplatform.open.common.vo.base.PageRequest;
import cn.dataplatform.open.common.vo.base.PageResult;
import cn.dataplatform.open.web.store.entity.DevelopTask;
import cn.dataplatform.open.web.vo.data.develop.*;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 数据研发-SQL 任务
 *
 * @author dingqianwen
 * @date 2025/1/4
 * @since 1.0.0
 */
public interface DevelopTaskService extends IService<DevelopTask> {

    /**
     * 任务列表
     *
     * @param pageRequest p
     * @return r
     */
    PageResult<DevelopTaskListResponse> list(PageRequest<DevelopTaskListRequest> pageRequest);

    /**
     * 新增任务
     *
     * @param request d
     * @return 任务ID
     */
    Long add(DevelopTaskAddRequest request);

    /**
     * 更新任务
     *
     * @param request d
     * @return r
     */
    Boolean update(DevelopTaskUpdateRequest request);

    /**
     * 任务详情
     *
     * @param id id
     * @return r
     */
    DevelopTaskDetailResponse detail(Long id);

    /**
     * 删除任务
     *
     * @param id id
     * @return r
     */
    Boolean delete(Long id);

    /**
     * 运行任务（id 优先；无 id 为临时运行）
     *
     * @param request d
     * @return r
     */
    DevelopRunResponse run(DevelopTaskRunRequest request);

    /**
     * 运行记录列表
     *
     * @param pageRequest p
     * @return r
     */
    PageResult<DevelopTaskLogListResponse> logList(PageRequest<DevelopTaskLogListRequest> pageRequest);

    /**
     * 运行记录详情
     *
     * @param id id
     * @return r
     */
    DevelopTaskLogDetailResponse logDetail(Long id);

    /**
     * 查询启用且配置了 cron 的任务（供调度器扫描）
     *
     * @return r
     */
    List<DevelopTask> listEnabledCronTasks();

    /**
     * 推进任务的下次调度执行时间
     *
     * @param task t
     */
    void advanceNextExecTime(DevelopTask task);

    /**
     * 以指定触发方式执行任务并写入运行记录
     *
     * @param task        t
     * @param triggerType MANUAL/CRON
     * @return r
     */
    DevelopRunResponse runTask(DevelopTask task, String triggerType);

}
