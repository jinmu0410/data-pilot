package cn.dataplatform.open.common.alarm.scene;

/**
 * 数据流相关告警场景标记接口，实现该接口的场景可按数据流编码过滤
 *
 * @author jinmu
 * @date 2026/7/3
 * @since 1.0.0
 */
public interface DataFlowScene extends Scene {

    /**
     * 当前告警场景关联的数据流编码
     *
     * @return 数据流编码
     */
    String getFlowCode();

}