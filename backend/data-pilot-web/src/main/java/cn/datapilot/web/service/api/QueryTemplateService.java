package cn.datapilot.web.service.api;

import cn.datapilot.common.vo.base.PageRequest;
import cn.datapilot.common.vo.base.PageResult;
import cn.datapilot.web.store.entity.QueryTemplate;
import cn.datapilot.web.vo.data.service.*;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 数据服务-查询模板
 *
 * @author jinmu
 * @date 2025/2/2
 * @since 1.0.0
 */
public interface QueryTemplateService extends IService<QueryTemplate> {

    /**
     * 模板列表
     *
     * @param pageRequest p
     * @return r
     */
    PageResult<QueryTemplateListResponse> list(PageRequest<QueryTemplateListRequest> pageRequest);

    /**
     * 模板详情
     *
     * @param id id
     * @return r
     */
    QueryTemplateDetailResponse detail(Long id);

    /**
     * 新增模板
     *
     * @param request d
     * @return 模板ID
     */
    Long add(QueryTemplateAddRequest request);

    /**
     * 更新模板
     *
     * @param request d
     * @return r
     */
    Boolean update(QueryTemplateUpdateRequest request);

    /**
     * 删除模板
     *
     * @param id id
     * @return r
     */
    Boolean delete(Long id);

    /**
     * 发布模板
     *
     * @param request d
     * @return 发布快照
     */
    QueryTemplatePublishResponse publish(QueryTemplatePublishRequest request);

    /**
     * 测试模板（不写日志）
     *
     * @param request d
     * @return r
     */
    QueryExecuteResult test(QueryTemplateTestRequest request);

    /**
     * 对外调用（校验 secret、限流、缓存、执行、记录日志）
     *
     * @param code    模板编码
     * @param secret  请求头密钥
     * @param timestamp HMAC 时间戳
     * @param nonce HMAC 随机数
     * @param signature HMAC 签名
     * @param requestBody 原始请求体
     * @param request 调用请求
     * @param ip      调用方 IP
     * @return r
     */
    QueryCallResponse call(String code, String secret, String timestamp, String nonce, String signature,
                           String requestBody, QueryCallRequest request, String ip);

    /**
     * 调用日志列表
     *
     * @param pageRequest p
     * @return r
     */
    PageResult<QueryLogListResponse> logList(PageRequest<QueryLogListRequest> pageRequest);

    /**
     * 调用日志详情
     *
     * @param id id
     * @return r
     */
    QueryLogDetailResponse logDetail(Long id);

}
