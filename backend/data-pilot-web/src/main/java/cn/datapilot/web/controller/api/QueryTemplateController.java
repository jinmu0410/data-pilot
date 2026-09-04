package cn.datapilot.web.controller.api;

import cn.datapilot.common.vo.base.IdRequest;
import cn.datapilot.common.vo.base.PageRequest;
import cn.datapilot.common.vo.base.PageResult;
import cn.datapilot.common.vo.base.PlainResult;
import cn.datapilot.web.annotation.Auth;
import cn.datapilot.web.annotation.ReSubmitLock;
import cn.datapilot.web.service.api.QueryTemplateService;
import cn.datapilot.web.vo.data.service.*;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 数据服务-查询模板
 *
 * @author jinmu
 * @date 2025/2/2
 * @since 1.0.0
 */
@RestController
@RequestMapping("/service/api")
public class QueryTemplateController {

    @Resource
    private QueryTemplateService queryTemplateService;

    /**
     * 模板列表
     *
     * @param pageRequest p
     * @return r
     */
    @Auth("service:api:list")
    @PostMapping("list")
    public PageResult<QueryTemplateListResponse> list(@RequestBody @Valid PageRequest<QueryTemplateListRequest> pageRequest) {
        return this.queryTemplateService.list(pageRequest);
    }

    /**
     * 模板详情
     *
     * @param idRequest d
     * @return r
     */
    @Auth("service:api:detail")
    @PostMapping("detail")
    public PlainResult<QueryTemplateDetailResponse> detail(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.queryTemplateService.detail(idRequest.getId()));
    }

    /**
     * 新增模板
     *
     * @param request d
     * @return r
     */
    @Auth("service:api:add")
    @ReSubmitLock
    @PostMapping("add")
    public PlainResult<Long> add(@RequestBody @Valid QueryTemplateAddRequest request) {
        return new PlainResult<>(this.queryTemplateService.add(request));
    }

    /**
     * 更新模板
     *
     * @param request d
     * @return r
     */
    @Auth("service:api:update")
    @ReSubmitLock
    @PostMapping("update")
    public PlainResult<Boolean> update(@RequestBody @Valid QueryTemplateUpdateRequest request) {
        return new PlainResult<>(this.queryTemplateService.update(request));
    }

    /**
     * 删除模板
     *
     * @param idRequest d
     * @return r
     */
    @Auth("service:api:delete")
    @ReSubmitLock
    @PostMapping("delete")
    public PlainResult<Boolean> delete(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.queryTemplateService.delete(idRequest.getId()));
    }

    /**
     * 发布模板
     *
     * @param request d
     * @return r
     */
    @Auth("service:api:publish")
    @ReSubmitLock
    @PostMapping("publish")
    public PlainResult<QueryTemplatePublishResponse> publish(@RequestBody @Valid QueryTemplatePublishRequest request) {
        return new PlainResult<>(this.queryTemplateService.publish(request));
    }

    /**
     * 测试模板（不写日志）
     *
     * @param request d
     * @return r
     */
    @Auth("service:api:detail")
    @ReSubmitLock
    @PostMapping("test")
    public PlainResult<QueryExecuteResult> test(@RequestBody QueryTemplateTestRequest request) {
        return new PlainResult<>(this.queryTemplateService.test(request));
    }

    /**
     * 调用日志列表
     *
     * @param pageRequest p
     * @return r
     */
    @Auth("service:api:log")
    @PostMapping("log/list")
    public PageResult<QueryLogListResponse> logList(@RequestBody @Valid PageRequest<QueryLogListRequest> pageRequest) {
        return this.queryTemplateService.logList(pageRequest);
    }

    /**
     * 调用日志详情
     *
     * @param idRequest d
     * @return r
     */
    @Auth("service:api:log")
    @PostMapping("log/detail")
    public PlainResult<QueryLogDetailResponse> logDetail(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.queryTemplateService.logDetail(idRequest.getId()));
    }

}
