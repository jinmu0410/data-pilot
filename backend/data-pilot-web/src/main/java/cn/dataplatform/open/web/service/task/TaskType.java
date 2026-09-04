package cn.dataplatform.open.web.service.task;

import cn.dataplatform.open.common.exception.ApiException;
import cn.hutool.core.util.StrUtil;

/**
 * 统一任务类型
 *
 * @author jinmu
 */
public enum TaskType {

    SQL,
    DATAX,
    SEATUNNEL,
    PYTHON,
    SHELL;

    public boolean isSync() {
        return this == DATAX || this == SEATUNNEL;
    }

    public boolean isScript() {
        return this == PYTHON || this == SHELL;
    }

    public static TaskType of(String type) {
        if (StrUtil.isBlank(type)) {
            throw new ApiException("任务类型不能为空");
        }
        for (TaskType value : values()) {
            if (value.name().equalsIgnoreCase(type)) {
                return value;
            }
        }
        throw new ApiException("不支持的任务类型: " + type);
    }
}
