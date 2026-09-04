package cn.dataplatform.open.flow.service.core;

import cn.dataplatform.open.flow.service.core.component.FlowComponent;
import cn.dataplatform.open.flow.service.core.pack.StopWatch;
import cn.dataplatform.open.flow.service.core.record.*;
import cn.dataplatform.open.flow.service.core.record.Record;
import lombok.Data;

/**
 * 组件数据传输对象，每次往后传递数据，使用新的Transmit对象，防止数据污染
 *
 * @author jinmu
 * @date 2025/1/6
 * @since 1.0.0
 */
@Data
public class Transmit {

    /**
     * 当前传输的组件
     */
    private FlowComponent flowComponent;

    /**
     * 传输的数据
     * <p>
     *
     * @see BatchPlainRecord 批量普通数据
     * @see BatchStreamRecord 批量流数据，当debezium批量模式时，或者接收外部批量流消息，例如Canal的
     * @see PlainRecord 普通单条数据
     * @see StreamRecord 流式单条数据
     * @see EmptyRecord 空数据
     */
    private Record record;

    /**
     * 用来记录组件执行耗时
     */
    private StopWatch timer;

    /**
     * 获取记录数据
     *
     * @return 记录数据
     * @see EmptyRecord
     */
    public Record getRecord() {
        if (this.record != null) {
            return this.record;
        }
        return EmptyRecord.INSTANCE;
    }

}
