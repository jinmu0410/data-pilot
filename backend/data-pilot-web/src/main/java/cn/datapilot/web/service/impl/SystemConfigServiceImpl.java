package cn.datapilot.web.service.impl;

import cn.datapilot.common.exception.ApiException;
import cn.datapilot.web.service.SystemConfigService;
import cn.datapilot.web.store.entity.SystemConfig;
import cn.datapilot.web.store.mapper.SystemConfigMapper;
import cn.datapilot.web.vo.system.SystemConfigRequest;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统配置服务实现
 *
 * @author jinmu
 */
@Service
public class SystemConfigServiceImpl extends ServiceImpl<SystemConfigMapper, SystemConfig>
        implements SystemConfigService {

    private volatile Map<String, String> cache;

    @Override
    public List<SystemConfig> listConfig() {
        return this.list(new LambdaQueryWrapper<SystemConfig>().orderByAsc(SystemConfig::getId));
    }

    @Override
    public Boolean add(SystemConfigRequest request) {
        if (StrUtil.isBlank(request.getConfigKey())) {
            throw new ApiException("配置键不能为空");
        }
        long exists = this.count(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, request.getConfigKey()));
        if (exists > 0) {
            throw new ApiException("配置键已存在: " + request.getConfigKey());
        }
        SystemConfig config = new SystemConfig();
        config.setConfigKey(request.getConfigKey());
        config.setConfigValue(request.getConfigValue());
        config.setDescription(request.getDescription());
        boolean saved = this.save(config);
        if (saved) {
            this.cache = null;
        }
        return saved;
    }

    @Override
    public Boolean update(SystemConfigRequest request) {
        if (request.getId() == null) {
            throw new ApiException("配置 id 不能为空");
        }
        SystemConfig config = new SystemConfig();
        config.setId(request.getId());
        config.setConfigKey(request.getConfigKey());
        config.setConfigValue(request.getConfigValue());
        config.setDescription(request.getDescription());
        boolean updated = this.updateById(config);
        if (updated) {
            this.cache = null;
        }
        return updated;
    }

    @Override
    public Boolean delete(Long id) {
        boolean removed = this.removeById(id);
        if (removed) {
            this.cache = null;
        }
        return removed;
    }

    @Override
    public String getValue(String key, String defaultValue) {
        Map<String, String> c = this.cache;
        if (c == null) {
            synchronized (this) {
                if (this.cache == null) {
                    this.cache = this.list().stream()
                            .collect(Collectors.toMap(SystemConfig::getConfigKey, SystemConfig::getConfigValue, (a, b) -> a));
                }
                c = this.cache;
            }
        }
        String value = c.get(key);
        return StrUtil.isNotBlank(value) ? value : defaultValue;
    }
}
