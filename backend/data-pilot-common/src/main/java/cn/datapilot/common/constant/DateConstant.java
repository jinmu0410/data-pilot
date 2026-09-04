package cn.datapilot.common.constant;

import java.time.format.DateTimeFormatter;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2026/3/18
 * @since 1.0.0
 */
public interface DateConstant {

    DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DateConstant.DATE_TIME_FORMAT);


    String DEFAULT_TIME_ZONE = "GMT+0800";
    String DATE_FORMAT = "yyyy-MM-dd";
    String DATE_COMPACT_FORMAT = "yyyyMMdd";
    String TIME_FORMAT = "HH:mm:ss";
    String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    /**
     * 带毫秒的时间格式
     */
    String DATE_TIME_MILLIS_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * "8uuuu-MM-dd HH:mm:ss"
     */
    String ELASTIC_DATE_TIME_SPECIFIER = "8uuuu-MM-dd HH:mm:ss";


}
