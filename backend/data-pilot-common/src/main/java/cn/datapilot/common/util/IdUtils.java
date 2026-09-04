package cn.datapilot.common.util;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/12/17
 * @since 1.0.0
 */
public class IdUtils extends IdUtil {

    /**
     * 生成简单ID，格式：时间戳(秒级别的十六进制)+6位随机字符串
     *
     * @return 简单ID
     */
    public static String getSimpleId() {
        return Integer.toHexString((int) DateUtil.currentSeconds()) + RandomUtil.randomString(7);
    }

}
