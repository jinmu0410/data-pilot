package cn.datapilot.common.event;

import cn.datapilot.common.body.DataFlowMessageBody;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;

public class DataFlowEvent extends ApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1628296277627810450L;

    public DataFlowEvent(DataFlowMessageBody dataFlowMessageBody) {
        super(dataFlowMessageBody);
    }

    @Override
    public DataFlowMessageBody getSource() {
        return (DataFlowMessageBody) super.getSource();
    }
}
