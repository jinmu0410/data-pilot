package cn.datapilot.web.service;

import cn.datapilot.web.store.entity.SystemConfig;
import cn.datapilot.web.vo.system.SystemConfigRequest;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 系统配置服务
 *
 * @author jinmu
 */
public interface SystemConfigService extends IService<SystemConfig> {

    List<SystemConfig> listConfig();

    Boolean add(SystemConfigRequest request);

    Boolean update(SystemConfigRequest request);

    Boolean delete(Long id);

    /**
     * 按 key 读取配置值，不存在或为空时返回默认值
     */
    String getValue(String key, String defaultValue);
}
