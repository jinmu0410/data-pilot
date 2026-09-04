package cn.dataplatform.open.web.vo.data.sync;

import lombok.Data;

/**
 * 数据集成-同步运行实例列表查询
 *
 * @author dingqianwen
 */
@Data
public class SyncTaskLogListRequest {

    private Long taskId;

    private String engine;

    private String status;
}
