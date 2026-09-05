package cn.datapilot.common.util;

import cn.datapilot.common.constant.Constant;
import cn.hutool.core.util.StrUtil;
import org.slf4j.MDC;

/**
 * 一句话功能简述
 *
 * @author jinmu
 * @date 2026/3/29
 * @since 1.0.0
 */
public class MDCUtils {

    /**
     * 获取当前线程上下文中的请求ID。
     *
     * @return 当前请求ID，如果未设置则返回 {@code null}
     */
    public static String getRequestId() {
        return MDC.get(Constant.REQUEST_ID);
    }

    /**
     * 设置当前线程上下文中的请求ID。
     *
     * @param requestId 请求ID，建议使用 UUID 或业务生成的唯一字符串
     */
    public static void setRequestId(String requestId) {
        if (StrUtil.isBlank(requestId)) {
            return;
        }
        MDC.put(Constant.REQUEST_ID, requestId);
    }

}
