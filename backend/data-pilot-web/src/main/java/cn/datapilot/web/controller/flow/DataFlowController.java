package cn.datapilot.web.controller.flow;

import cn.datapilot.common.vo.base.IdRequest;
import cn.datapilot.common.vo.base.PageRequest;
import cn.datapilot.common.vo.base.PageResult;
import cn.datapilot.common.vo.base.PlainResult;
import cn.datapilot.web.annotation.Auth;
import cn.datapilot.web.annotation.DataPermission;
import cn.datapilot.web.annotation.ReSubmitLock;
import cn.datapilot.web.config.Context;
import cn.datapilot.web.enums.OperationPermissionType;
import cn.datapilot.web.enums.RecordType;
import cn.datapilot.web.service.flow.DataFlowService;
import cn.datapilot.web.service.flow.FlowRunService;
import cn.datapilot.web.service.task.TaskParamsHelper;
import cn.datapilot.web.service.task.runner.SyncTaskRunner;
import cn.datapilot.web.vo.data.flow.*;
import cn.datapilot.web.vo.data.task.TaskConfigResponse;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/1/4
 * @since 1.0.0
 */
@RestController
@RequestMapping("/dataflow")
public class DataFlowController {

    @Resource
    private DataFlowService dataFlowService;

    @Resource
    private FlowRunService flowRunService;

    @Resource
    private TaskParamsHelper taskParamsHelper;

    @Resource
    private SyncTaskRunner syncTaskRunner;

    /**
     * 数据流列表
     *
     * @param pageRequest p
     * @return r
     */
    @Auth("data:flow:list")
    @PostMapping("list")
    public PageResult<DataFlowListResponse> list(@RequestBody @Valid PageRequest<DataFlowListRequest> pageRequest) {
        return this.dataFlowService.list(pageRequest);
    }


    /**
     * 获取数据流详情
     *
     * @param idRequest d
     * @return r
     */
    @Auth("data:flow:detail")
    @PostMapping("detail")
    public PlainResult<DataFlowDetailResponse> detail(@RequestBody @Valid IdRequest idRequest) {
        DataFlowDetailResponse dataFlowDetailResponse = this.dataFlowService.detail(idRequest.getId());
        return new PlainResult<>(dataFlowDetailResponse);
    }


    /**
     * 创建数据流
     *
     * @param dataFlowCreateRequest d
     * @return r
     */
    @ReSubmitLock
    @Auth("data:flow:create")
    @PostMapping("create")
    public PlainResult<DataFlowCreateResponse> create(@RequestBody @Valid
                                                      DataFlowCreateRequest dataFlowCreateRequest) {
        DataFlowCreateResponse dataFlowCreateResponse = this.dataFlowService.create(dataFlowCreateRequest);
        return new PlainResult<>(dataFlowCreateResponse);
    }


    /**
     * 更新数据流
     *
     * @param dataFlowUpdateRequest d
     * @return r
     */
    @ReSubmitLock
    @DataPermission(type = OperationPermissionType.EDIT, recordType = RecordType.DATA_FLOW, id = "#dataFlowUpdateRequest.id")
    @Auth("data:flow:update")
    @PostMapping("update")
    public PlainResult<Boolean> update(@RequestBody @Valid
                                       DataFlowUpdateRequest dataFlowUpdateRequest) {
        Boolean update = this.dataFlowService.update(dataFlowUpdateRequest);
        return new PlainResult<>(update);
    }

    /**
     * 发布
     *
     * @param publishRequest d
     * @return r
     */
    @DataPermission(type = OperationPermissionType.PUBLISH, recordType = RecordType.DATA_FLOW, id = "#publishRequest.id")
    @ReSubmitLock
    @Auth("data:flow:publish")
    @PostMapping("publish")
    public PlainResult<Boolean> publish(@RequestBody @Valid PublishRequest publishRequest) {
        return new PlainResult<>(this.dataFlowService.publish(publishRequest));
    }


    /**
     * 停止流程
     *
     * @param idRequest d
     * @return r
     */
    @DataPermission(type = OperationPermissionType.PUBLISH, recordType = RecordType.DATA_FLOW, id = "#idRequest.id")
    @ReSubmitLock
    @Auth("data:flow:stop")
    @PostMapping("stop")
    public PlainResult<Boolean> stop(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.dataFlowService.stop(idRequest.getId()));
    }

    /**
     * 启动流程
     *
     * @param idRequest d
     * @return r
     */
    @DataPermission(type = OperationPermissionType.PUBLISH, recordType = RecordType.DATA_FLOW, id = "#idRequest.id")
    @ReSubmitLock
    @Auth("data:flow:start")
    @PostMapping("start")
    public PlainResult<Boolean> start(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.dataFlowService.start(idRequest.getId()));
    }

    /**
     * 删除流程
     *
     * @param idRequest d
     * @return r
     */
    @Auth("data:flow:delete")
    @DataPermission(type = OperationPermissionType.EDIT, recordType = RecordType.DATA_FLOW, id = "#idRequest.id")
    @ReSubmitLock
    @PostMapping("delete")
    public PlainResult<Boolean> delete(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.dataFlowService.delete(idRequest.getId()));
    }

    /**
     * 回滚至某个版本
     *
     * @param idRequest id
     * @return r
     */
    @Auth("data:flow:publish")
    @DataPermission(type = OperationPermissionType.EDIT, recordType = RecordType.DATA_FLOW, id = "#idRequest.id")
    @ReSubmitLock
    @PostMapping("rollback")
    public PlainResult<Boolean> rollback(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.dataFlowService.rollback(idRequest.getId()));
    }

    /**
     * 手动运行任务流
     *
     * @param request id + failureStrategy
     * @return flowInstanceId
     */
    @Auth("data:flow:run")
    @ReSubmitLock
    @PostMapping("run")
    public PlainResult<Long> run(@RequestBody @Valid FlowRunRequest request) {
        return new PlainResult<>(this.dataFlowService.run(request.getId(), request.getFailureStrategy()));
    }

    /**
     * 任务流实例列表
     *
     * @param pageRequest p
     * @return r
     */
    @Auth("data:flow:instance")
    @PostMapping("instance/list")
    public PageResult<FlowInstanceListResponse> instanceList(@RequestBody @Valid PageRequest<FlowInstanceListRequest> pageRequest) {
        return this.flowRunService.instanceList(pageRequest);
    }

    /**
     * 任务流实例详情
     *
     * @param idRequest id
     * @return r
     */
    @Auth("data:flow:instance")
    @PostMapping("instance/detail")
    public PlainResult<FlowInstanceDetailResponse> instanceDetail(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.flowRunService.instanceDetail(idRequest.getId()));
    }

    /**
     * 预览同步引擎（DataX/SeaTunnel）最终生成的配置
     */
    @Auth("data:flow:detail")
    @PostMapping("config/preview")
    public PlainResult<TaskConfigResponse> previewConfig(@RequestBody @Valid ConfigPreviewRequest request) {
        String taskParams = this.taskParamsHelper.serialize(request.getTaskType(), request.getNode());
        TaskConfigResponse response = this.syncTaskRunner.buildConfig(
                request.getTaskType(), taskParams, Context.getWorkspace().getCode());
        return new PlainResult<>(response);
    }

}
