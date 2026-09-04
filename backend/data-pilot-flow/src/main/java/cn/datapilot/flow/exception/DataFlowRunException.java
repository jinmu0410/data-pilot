package cn.datapilot.flow.exception;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/11/29
 * @since 1.0.0
 */
public class DataFlowRunException extends RuntimeException {

    public DataFlowRunException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

    public DataFlowRunException(String message) {
        super(message);
    }

    public DataFlowRunException(Exception e) {
        super(e);
    }

}
