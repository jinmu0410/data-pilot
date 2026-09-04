package cn.datapilot.common.event;

import org.springframework.context.ApplicationEvent;

import java.io.Serial;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/3/15
 * @since 1.0.0
 */
public class QueryLogEvent extends ApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1628296277627810450L;

    public QueryLogEvent(String string) {
        super(string);
    }

    @Override
    public String getSource() {
        return (String) super.getSource();
    }

}

