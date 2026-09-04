package cn.dataplatform.open.web.service.sync;

import cn.dataplatform.open.common.vo.base.PageRequest;
import cn.dataplatform.open.common.vo.base.PageResult;
import cn.dataplatform.open.web.store.entity.SyncTask;
import cn.dataplatform.open.web.vo.data.sync.*;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 数据集成-同步任务
 *
 * @author dingqianwen
 */
public interface SyncTaskService extends IService<SyncTask> {

    PageResult<SyncTaskListResponse> list(PageRequest<SyncTaskListRequest> pageRequest);

    Long add(SyncTaskAddRequest request);

    Boolean update(SyncTaskUpdateRequest request);

    SyncTaskDetailResponse detail(Long id);

    Boolean delete(Long id);

    SyncConfigResponse generateConfig(Long id);

    Long run(Long id);

    PageResult<SyncTaskLogListResponse> logList(PageRequest<SyncTaskLogListRequest> pageRequest);

    SyncTaskLogDetailResponse logDetail(Long id);
}
