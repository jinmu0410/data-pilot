package cn.dataplatform.open.flow.service.core;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/1/6
 * @since 1.0.0
 */
@Data
public class Context implements Cloneable, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 执行唯一标识
     */
    private String id;

    /**
     * 数据流启动时间
     */
    private LocalDateTime startTime;

    /**
     * 克隆
     *
     * @return Context
     */
    @Override
    public Context clone() {
        try {
            return (Context) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }

}
