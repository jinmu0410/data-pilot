package cn.datapilot.web.vo.data.flow;

import lombok.Data;

import java.util.List;

/**
 * 任务流设计（data_flow.design JSON 结构）
 *
 * @author jinmu
 */
@Data
public class TaskFlowDesign {

    private List<FlowNode> nodes;

    private List<FlowEdge> edges;

    @Data
    public static class FlowNode {

        private String id;

        /**
         * SQL/DATAX/SEATUNNEL/PYTHON/SHELL
         */
        private String type;

        private TaskFlowNode properties;
    }

    @Data
    public static class FlowEdge {

        private String id;

        private String sourceNodeId;

        private String targetNodeId;

        private EdgeProps properties;
    }

    @Data
    public static class EdgeProps {

        private Integer order;
    }
}
