package cn.datapilot.common.exception;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2026/2/11
 * @since 1.0.0
 */
public class ParallelException extends RuntimeException {

    public ParallelException(String message, Throwable cause) {
        super(message, cause);
    }

}
