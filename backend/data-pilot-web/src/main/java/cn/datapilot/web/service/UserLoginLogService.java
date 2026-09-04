package cn.datapilot.web.service;


import cn.datapilot.common.vo.base.PageRequest;
import cn.datapilot.common.vo.base.PageResult;
import cn.datapilot.web.store.entity.UserLoginLog;
import cn.datapilot.web.vo.login.log.LoginLogDetailResponse;
import cn.datapilot.web.vo.login.log.LoginLogListRequest;
import cn.datapilot.web.vo.login.log.LoginLogListResponse;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/3/9
 * @since 1.0.0
 */
public interface UserLoginLogService extends IService<UserLoginLog> {

    /**
     * 登录日志列表
     *
     * @param pageRequest p
     * @return r
     */
    PageResult<LoginLogListResponse> list(PageRequest<LoginLogListRequest> pageRequest);

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
    LoginLogDetailResponse detail(Long id);

}
