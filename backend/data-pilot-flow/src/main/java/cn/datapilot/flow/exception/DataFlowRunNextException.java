package cn.datapilot.flow.exception;

/**
 * 数据流下游节点运行异常
 *
 * @author jinmu
 * @date 2025/6/27
 * @since 1.0.0
 */
public class DataFlowRunNextException extends RuntimeException {

    public DataFlowRunNextException(String message) {
        super(message);
    }

    public DataFlowRunNextException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

}
