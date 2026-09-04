package cn.datapilot.common.event;

import cn.datapilot.common.body.DataFlowComponentMessageBody;
import org.springframework.context.ApplicationEvent;

import java.io.Serial;

public class DataFlowComponentEvent extends ApplicationEvent {

    @Serial
    private static final long serialVersionUID = 1628296277627810450L;

    public DataFlowComponentEvent(DataFlowComponentMessageBody dataFlowComponentMessageBody) {
        super(dataFlowComponentMessageBody);
    }

    @Override
    public DataFlowComponentMessageBody getSource() {
        return (DataFlowComponentMessageBody) super.getSource();
    }
}
