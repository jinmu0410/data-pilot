package cn.datapilot.web.vo.data.sync;

import lombok.Data;

/**
 * 数据集成-同步任务列表查询
 *
 * @author jinmu
 */
@Data
public class SyncTaskListRequest {

    private String keyword;

    private String engine;

    private String status;
}
