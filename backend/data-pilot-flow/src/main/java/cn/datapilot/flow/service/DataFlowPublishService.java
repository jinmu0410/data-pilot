package cn.datapilot.flow.service;

import cn.datapilot.flow.store.entity.DataFlowPublish;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/1/22
 * @since 1.0.0
 */
public interface DataFlowPublishService extends IService<DataFlowPublish> {

    /**
     * 加载数据流程
     *
     * @param id 数据流程ID
     */
    void load(Long id);

    /**
     * 停止数据流程
     *
     * @param id 数据流程ID
     */
    void stop(Long id);

}
