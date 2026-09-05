package cn.datapilot.web.service;

import cn.datapilot.common.vo.base.PageRequest;
import cn.datapilot.common.vo.base.PageResult;
import cn.datapilot.web.store.entity.Role;
import cn.datapilot.web.vo.role.*;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author jinmu
 */
public interface RoleService extends IService<Role> {

    /**
     * 列表
     *
     * @param pageRequest 分页请求
     * @return user
     */
    PageResult<RoleListResponse> list(PageRequest<RoleListRequest> pageRequest);

    /**
     * 详情
     *
     * @param id id
     * @return r
     */
    RoleDetailResponse detail(Long id);

    /**
     * 添加
     *
     * @param roleAddRequest 请求
     * @return r
     */
    Boolean add(RoleAddRequest roleAddRequest);

    /**
     * 更新
     *
     * @param roleUpdateRequest 请求
     * @return r
     */
    Boolean update(RoleUpdateRequest roleUpdateRequest);

    /**
     * 删除
     *
     * @param id id
     * @return r
     */
    Boolean delete(Long id);

}
