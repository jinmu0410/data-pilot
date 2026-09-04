package cn.datapilot.web.service.flow;


import cn.datapilot.common.vo.base.PageRequest;
import cn.datapilot.common.vo.base.PageResult;
import cn.datapilot.web.store.entity.DataFlowPublish;
import cn.datapilot.web.vo.data.flow.publish.DataFlowPublishDetailResponse;
import cn.datapilot.web.vo.data.flow.publish.DataFlowPublishListResponse;
import com.baomidou.mybatisplus.extension.service.IService;


/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/1/22
 * @since 1.0.0
 */
public interface DataFlowPublishService extends IService<DataFlowPublish> {

    /**
     * 历史列表
     *
     * @param pageRequest p
     * @return p
     */
    PageResult<DataFlowPublishListResponse> historyList(PageRequest<String> pageRequest);


    /**
     * 删除
     *
     * @param id id
     * @return r
     */
    Boolean delete(Long id);

    /**
     * 获取已发布数据流详情
     *
     * @param id d
     * @return r
     */
    DataFlowPublishDetailResponse detail(Long id);

}
