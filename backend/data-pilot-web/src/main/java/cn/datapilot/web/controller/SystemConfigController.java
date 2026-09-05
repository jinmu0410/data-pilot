package cn.datapilot.web.controller;

import cn.datapilot.common.vo.base.IdRequest;
import cn.datapilot.common.vo.base.PlainResult;
import cn.datapilot.web.annotation.Auth;
import cn.datapilot.web.annotation.ReSubmitLock;
import cn.datapilot.web.service.SystemConfigService;
import cn.datapilot.web.store.entity.SystemConfig;
import cn.datapilot.web.vo.system.SystemConfigRequest;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 系统配置管理
 *
 * @author jinmu
 */
@RestController
@RequestMapping("/system/config")
public class SystemConfigController {

    @Resource
    private SystemConfigService systemConfigService;

    /**
     * 配置列表
     */
    @Auth("system:config:list")
    @PostMapping("list")
    public PlainResult<List<SystemConfig>> list() {
        return new PlainResult<>(this.systemConfigService.listConfig());
    }

    /**
     * 新增配置
     */
    @Auth("system:config:add")
    @ReSubmitLock
    @PostMapping("add")
    public PlainResult<Boolean> add(@RequestBody @Valid SystemConfigRequest request) {
        return new PlainResult<>(this.systemConfigService.add(request));
    }

    /**
     * 修改配置
     */
    @Auth("system:config:update")
    @ReSubmitLock
    @PostMapping("update")
    public PlainResult<Boolean> update(@RequestBody @Valid SystemConfigRequest request) {
        return new PlainResult<>(this.systemConfigService.update(request));
    }

    /**
     * 删除配置
     */
    @Auth("system:config:delete")
    @ReSubmitLock
    @PostMapping("delete")
    public PlainResult<Boolean> delete(@RequestBody @Valid IdRequest idRequest) {
        return new PlainResult<>(this.systemConfigService.delete(idRequest.getId()));
    }
}
