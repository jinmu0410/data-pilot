package cn.dataplatform.open.web.service.flow;

import cn.dataplatform.open.common.vo.base.PageRequest;
import cn.dataplatform.open.common.vo.base.PageResult;
import cn.dataplatform.open.web.vo.data.flow.FlowInstanceDetailResponse;
import cn.dataplatform.open.web.vo.data.flow.FlowInstanceListRequest;
import cn.dataplatform.open.web.vo.data.flow.FlowInstanceListResponse;

/**
 * 任务流执行服务（DAG 拓扑执行 + 实例查询）
 *
 * @author jinmu
 */
public interface FlowRunService {

    /**
     * 触发一次任务流运行，返回任务流实例 id
     *
     * @param flowId          任务流 id
     * @param triggerType     MANUAL/CRON
     * @param failureStrategy 失败策略 CONTINUE/END（空默认 CONTINUE）
     * @return flowInstanceId
     */
    Long run(Long flowId, String triggerType, String failureStrategy);

    PageResult<FlowInstanceListResponse> instanceList(PageRequest<FlowInstanceListRequest> pageRequest);

    FlowInstanceDetailResponse instanceDetail(Long id);
}
