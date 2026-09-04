package cn.dataplatform.open.web.controller.develop;

import cn.dataplatform.open.common.vo.base.IdRequest;
import cn.dataplatform.open.common.vo.base.PageRequest;
import cn.dataplatform.open.common.vo.base.PageResult;
import cn.dataplatform.open.common.vo.base.PlainResult;
import cn.dataplatform.open.web.annotation.Auth;
import cn.dataplatform.open.web.annotation.ReSubmitLock;
import cn.dataplatform.open.web.service.develop.DevelopTaskService;
import cn.dataplatform.open.web.vo.data.develop.*;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据研发-SQL 任务
 *
 * @author jinmu
 * @date 2025/1/4
 * @since 1.0.0
 */
@RestController
@RequestMapping("/develop/task")
public class DevelopTaskController {

    @Resource
    private DevelopTaskService developTaskService;

    /**
     * 任务列表
     *
     * @param pageRequest p
     * @return r
     */
    @Auth("develop:task:list")
    @PostMapping("list")
    public PageResult<DevelopTaskListResponse> list(@RequestBody @Valid PageRequest<DevelopTaskListRequest> pageRequest) {
        return this.developTaskService.list(pageRequest);
    }

    /**
     * 任务详情
     *
     * @param idRequest d
     * @return r
     */
    @Auth("develop:task:detail")
    @PostMapping("detail")
    public PlainResult<DevelopTaskDetailResponse> detail(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.developTaskService.detail(idRequest.getId()));
    }

    /**
     * 新增任务
     *
     * @param request d
     * @return r
     */
    @Auth("develop:task:add")
    @ReSubmitLock
    @PostMapping("add")
    public PlainResult<Long> add(@RequestBody @Valid DevelopTaskAddRequest request) {
        return new PlainResult<>(this.developTaskService.add(request));
    }

    /**
     * 更新任务
     *
     * @param request d
     * @return r
     */
    @Auth("develop:task:update")
    @ReSubmitLock
    @PostMapping("update")
    public PlainResult<Boolean> update(@RequestBody @Valid DevelopTaskUpdateRequest request) {
        return new PlainResult<>(this.developTaskService.update(request));
    }

    /**
     * 删除任务
     *
     * @param idRequest d
     * @return r
     */
    @Auth("develop:task:delete")
    @ReSubmitLock
    @PostMapping("delete")
    public PlainResult<Boolean> delete(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.developTaskService.delete(idRequest.getId()));
    }

    /**
     * 运行任务（临时运行或按 id 运行）
     *
     * @param request d
     * @return r
     */
    @Auth("develop:task:run")
    @ReSubmitLock
    @PostMapping("run")
    public PlainResult<DevelopRunResponse> run(@RequestBody DevelopTaskRunRequest request) {
        return new PlainResult<>(this.developTaskService.run(request));
    }

    /**
     * 运行记录列表
     *
     * @param pageRequest p
     * @return r
     */
    @Auth("develop:task:list")
    @PostMapping("log/list")
    public PageResult<DevelopTaskLogListResponse> logList(@RequestBody @Valid PageRequest<DevelopTaskLogListRequest> pageRequest) {
        return this.developTaskService.logList(pageRequest);
    }

    /**
     * 运行记录详情
     *
     * @param idRequest d
     * @return r
     */
    @Auth("develop:task:detail")
    @PostMapping("log/detail")
    public PlainResult<DevelopTaskLogDetailResponse> logDetail(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.developTaskService.logDetail(idRequest.getId()));
    }

}
