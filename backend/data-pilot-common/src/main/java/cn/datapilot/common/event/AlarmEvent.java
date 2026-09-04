package cn.datapilot.common.event;

import cn.datapilot.common.body.AlarmMessageBody;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/2/22
 * @since 1.0.0
 */
public class AlarmEvent extends ApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1628296277627810450L;

    public AlarmEvent(AlarmMessageBody source) {
        super(source);
    }


    @Override
    public AlarmMessageBody getSource() {
        return (AlarmMessageBody) super.getSource();
    }
}
