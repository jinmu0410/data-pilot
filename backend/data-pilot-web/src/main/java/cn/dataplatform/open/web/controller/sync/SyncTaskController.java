package cn.dataplatform.open.web.controller.sync;

import cn.dataplatform.open.common.vo.base.IdRequest;
import cn.dataplatform.open.common.vo.base.PageRequest;
import cn.dataplatform.open.common.vo.base.PageResult;
import cn.dataplatform.open.common.vo.base.PlainResult;
import cn.dataplatform.open.web.annotation.Auth;
import cn.dataplatform.open.web.annotation.ReSubmitLock;
import cn.dataplatform.open.web.service.sync.SyncTaskService;
import cn.dataplatform.open.web.vo.data.sync.*;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据集成-同步任务
 *
 * @author jinmu
 */
@RestController
@RequestMapping("/sync/task")
public class SyncTaskController {

    @Resource
    private SyncTaskService syncTaskService;

    @Auth("data:sync:list")
    @PostMapping("list")
    public PageResult<SyncTaskListResponse> list(@RequestBody @Valid PageRequest<SyncTaskListRequest> pageRequest) {
        return this.syncTaskService.list(pageRequest);
    }

    @Auth("data:sync:detail")
    @PostMapping("detail")
    public PlainResult<SyncTaskDetailResponse> detail(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.syncTaskService.detail(idRequest.getId()));
    }

    @Auth("data:sync:add")
    @ReSubmitLock
    @PostMapping("add")
    public PlainResult<Long> add(@RequestBody @Valid SyncTaskAddRequest request) {
        return new PlainResult<>(this.syncTaskService.add(request));
    }

    @Auth("data:sync:update")
    @ReSubmitLock
    @PostMapping("update")
    public PlainResult<Boolean> update(@RequestBody @Valid SyncTaskUpdateRequest request) {
        return new PlainResult<>(this.syncTaskService.update(request));
    }

    @Auth("data:sync:delete")
    @ReSubmitLock
    @PostMapping("delete")
    public PlainResult<Boolean> delete(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.syncTaskService.delete(idRequest.getId()));
    }

    @Auth("data:sync:detail")
    @PostMapping("generateConfig")
    public PlainResult<SyncConfigResponse> generateConfig(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.syncTaskService.generateConfig(idRequest.getId()));
    }

    @Auth("data:sync:run")
    @ReSubmitLock
    @PostMapping("run")
    public PlainResult<Long> run(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.syncTaskService.run(idRequest.getId()));
    }

    @Auth("data:sync:log")
    @PostMapping("logList")
    public PageResult<SyncTaskLogListResponse> logList(@RequestBody @Valid PageRequest<SyncTaskLogListRequest> pageRequest) {
        return this.syncTaskService.logList(pageRequest);
    }

    @Auth("data:sync:log")
    @PostMapping("logDetail")
    public PlainResult<SyncTaskLogDetailResponse> logDetail(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.syncTaskService.logDetail(idRequest.getId()));
    }
}
