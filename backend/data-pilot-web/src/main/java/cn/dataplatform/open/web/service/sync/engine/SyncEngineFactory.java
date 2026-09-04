package cn.dataplatform.open.web.service.sync.engine;

import cn.dataplatform.open.common.exception.ApiException;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 同步引擎工厂：按引擎标识查找实现
 *
 * @author jinmu
 */
@Component
public class SyncEngineFactory {

    @Resource
    private List<SyncEngine> engines;

    public SyncEngine get(String type) {
        if (type == null) {
            throw new ApiException("同步引擎不能为空");
        }
        return engines.stream()
                .filter(e -> e.type().equalsIgnoreCase(type))
                .findFirst()
                .orElseThrow(() -> new ApiException("不支持的同步引擎: " + type));
    }
}
