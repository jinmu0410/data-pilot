package cn.datapilot.web.vo.data.flow;

import lombok.Data;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/1/3
 * @since 1.0.0
 */
@Data
public class DataFlowListRequest {

    /**
     * 关键字（匹配名称或编码，模糊）
     */
    private String keyword;

    private String name;

    private String code;

    private String status;
}
