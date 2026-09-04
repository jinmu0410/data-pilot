package cn.datapilot.support.service.alarm;

import cn.datapilot.common.body.AlarmSceneMessageBody;
import cn.datapilot.support.store.entity.AlarmScene;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/2/22
 * @since 1.0.0
 */
public interface AlarmSceneService extends IService<AlarmScene> {

    /**
     * 监听告警场景消息处理
     *
     * @param alarmSceneMessageBody 告警场景消息
     */
    void alarm(AlarmSceneMessageBody alarmSceneMessageBody);

}
