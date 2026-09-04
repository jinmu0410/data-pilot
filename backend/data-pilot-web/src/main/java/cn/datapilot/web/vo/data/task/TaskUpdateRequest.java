package cn.datapilot.web.vo.data.task;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 统一任务更新
 *
 * @author jinmu
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TaskUpdateRequest extends TaskAddRequest {

    @NotNull
    private Long id;
}
