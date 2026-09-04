package cn.datapilot.web.service.impl;


import cn.datapilot.common.component.OrikaMapper;
import cn.datapilot.web.config.Context;
import cn.datapilot.web.service.UserRoleService;
import cn.datapilot.web.store.entity.Role;
import cn.datapilot.web.store.entity.UserRole;
import cn.datapilot.web.store.mapper.UserRoleMapper;
import cn.datapilot.web.vo.role.RoleDetailResponse;
import cn.datapilot.web.vo.role.UserRoleUpsertRequest;
import cn.datapilot.web.vo.user.UserData;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserRoleServiceImpl extends ServiceImpl<UserRoleMapper, UserRole> implements UserRoleService {

    @Resource
    private OrikaMapper orikaMapper;
    @Resource
    private UserRoleMapper userRoleMapper;

    /**
     * 用户的角色列表
     *
     * @return r
     */
    @Override
    public List<RoleDetailResponse> list(Long userId) {
        List<Role> roleList = userRoleMapper.listRoleByUserId(userId);
        return roleList.stream().map(source -> {
            RoleDetailResponse target = new RoleDetailResponse();
            this.orikaMapper.map(source, target);
            return target;
        }).collect(Collectors.toList());
    }

    /**
     * 我的角色列表
     *
     * @return r
     */
    @Override
    public List<RoleDetailResponse> my() {
        UserData userData = Context.getUser();
        Long userId = userData.getId();
        return this.list(userId);
    }


    /**
     * 更新用户角色
     *
     * @param userRoleUpsertRequest 请求
     * @return r
     */
    @Override
    public Boolean upsert(UserRoleUpsertRequest userRoleUpsertRequest) {
        UserData userData = Context.getUser();
        Long createUserId = userData.getId();
        Long userId = userRoleUpsertRequest.getUserId();
        Long roleId = userRoleUpsertRequest.getRoleId();
        String status = userRoleUpsertRequest.getStatus();
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        UserRole entity = this.userRoleMapper.selectOne(new QueryWrapper<>(userRole));
        userRole.setStatus(status);
        if (entity == null) {
            userRole.setCreateUserId(createUserId);
            this.userRoleMapper.insert(userRole);
        } else {
            userRole.setId(entity.getId());
            this.userRoleMapper.updateById(userRole);
        }
        return true;
    }

}
