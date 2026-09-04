package cn.dataplatform.open.web.controller.api;

import cn.dataplatform.open.common.util.IPUtils;
import cn.dataplatform.open.common.vo.base.PlainResult;
import cn.dataplatform.open.web.service.api.QueryTemplateService;
import cn.dataplatform.open.web.vo.data.service.QueryCallRequest;
import cn.dataplatform.open.web.vo.data.service.QueryCallResponse;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

/**
 * 数据服务-对外调用接口（靠 secret 自校验，无登录态）
 *
 * @author jinmu
 * @date 2025/2/2
 * @since 1.0.0
 */
@RestController
@RequestMapping("/open/api")
public class QueryApiController {

    @Resource
    private QueryTemplateService queryTemplateService;

    /**
     * 对外调用
     *
     * @param code    模板编码
     * @param secret  请求头 X-Secret
     * @param request 调用请求
     * @return r
     */
    @PostMapping("{code}")
    public PlainResult<QueryCallResponse> call(@PathVariable String code,
                                               @RequestHeader(value = "X-Secret", required = false) String secret,
                                               @RequestBody QueryCallRequest request) {
        return new PlainResult<>(this.queryTemplateService.call(code, secret, request, IPUtils.getRequestIp()));
    }

}
