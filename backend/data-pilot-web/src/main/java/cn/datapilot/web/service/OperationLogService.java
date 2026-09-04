package cn.datapilot.web.service;

import cn.datapilot.common.vo.base.PageRequest;
import cn.datapilot.common.vo.base.PageResult;
import cn.datapilot.web.store.entity.OperationLog;
import cn.datapilot.web.vo.operation.log.OperationLogDetailResponse;
import cn.datapilot.web.vo.operation.log.OperationLogListRequest;
import cn.datapilot.web.vo.operation.log.OperationLogListResponse;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/3/9
 * @since 1.0.0
 */
public interface OperationLogService extends IService<OperationLog> {

    /**
     * 操作日志列表
     *
     * @param pageRequest p
     * @return r
     */
    PageResult<OperationLogListResponse> list(PageRequest<OperationLogListRequest> pageRequest);

    /**
     * 删除
     *
     * @param id r
     * @return r
     */
    Boolean delete(Long id);


    /**
     * 详情
     *
     * @param id r
     * @return r
     */
    OperationLogDetailResponse detail(Long id);
}
