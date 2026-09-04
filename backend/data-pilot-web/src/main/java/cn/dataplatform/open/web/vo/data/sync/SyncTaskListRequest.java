package cn.dataplatform.open.web.vo.data.sync;

import lombok.Data;

/**
 * 数据集成-同步任务列表查询
 *
 * @author dingqianwen
 */
@Data
public class SyncTaskListRequest {

    private String keyword;

    private String engine;

    private String status;
}
