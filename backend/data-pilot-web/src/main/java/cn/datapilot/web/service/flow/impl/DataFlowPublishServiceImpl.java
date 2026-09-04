package cn.datapilot.web.service.flow.impl;


import cn.datapilot.common.component.OrikaMapper;
import cn.datapilot.common.enums.flow.FlowStatus;
import cn.datapilot.common.vo.base.PageBase;
import cn.datapilot.common.vo.base.PageRequest;
import cn.datapilot.common.vo.base.PageResult;
import cn.datapilot.web.config.Context;
import cn.datapilot.web.service.flow.DataFlowPublishService;
import cn.datapilot.web.service.flow.DataFlowService;
import cn.datapilot.web.store.entity.DataFlow;
import cn.datapilot.web.store.entity.DataFlowPublish;
import cn.datapilot.web.store.mapper.DataFlowPublishMapper;
import cn.datapilot.web.vo.data.flow.publish.DataFlowPublishDetailResponse;
import cn.datapilot.web.vo.data.flow.publish.DataFlowPublishListResponse;
import cn.datapilot.web.vo.workspace.WorkspaceData;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/1/22
 * @since 1.0.0
 */
@Service
public class DataFlowPublishServiceImpl extends ServiceImpl<DataFlowPublishMapper, DataFlowPublish>
        implements DataFlowPublishService {

    @Resource
    private OrikaMapper orikaMapper;
    @Lazy
    @Resource
    private DataFlowService dataFlowService;

    /**
     * 历史列表
     *
     * @param pageRequest p
     * @return p
     */
    @Override
    public PageResult<DataFlowPublishListResponse> historyList(PageRequest<String> pageRequest) {
        PageBase pageBase = pageRequest.getPage();
        WorkspaceData workspace = Context.getWorkspace();
        Page<DataFlowPublish> page = this.lambdaQuery()
                .eq(DataFlowPublish::getCode, pageRequest.getQuery())
                .eq(DataFlowPublish::getWorkspaceCode, workspace.getCode())
                // 排除当前已经发布的版本
                .notIn(DataFlowPublish::getStatus, Arrays.asList(FlowStatus.ENABLE.name(), FlowStatus.PAUSE.name()))
                .orderByDesc(DataFlowPublish::getId)
                .page(PageDTO.of(pageBase.getCurrent(), pageBase.getSize()));
        List<DataFlowPublish> records = page.getRecords();
        if (records.isEmpty()) {
            return new PageResult<>(CollUtil.newArrayList(), page.getCurrent(), page.getSize(), 0L);
        }
        List<DataFlow> dataFlowPublishes = this.dataFlowService.lambdaQuery()
                .eq(DataFlow::getWorkspaceCode, workspace.getCode())
                .in(DataFlow::getCode, records.stream().map(DataFlowPublish::getCode)
                        .collect(Collectors.toList()))
                .list();
        Map<String, Long> publishIdMap = dataFlowPublishes.stream().collect(Collectors.toMap(DataFlow::getCode, DataFlow::getId));
        List<DataFlowPublishListResponse> flowPublishListResponses = records.stream()
                .map(m -> {
                    DataFlowPublishListResponse map = this.orikaMapper.map(m, DataFlowPublishListResponse.class);
                    map.setFlowId(publishIdMap.get(m.getCode()));
                    return map;
                }).
                collect(Collectors.toList());
        return new PageResult<>(flowPublishListResponses, page.getCurrent(), page.getSize(), page.getTotal());
    }


    /**
     * 删除
     *
     * @param id id
     * @return r
     */
    @Override
    public Boolean delete(Long id) {
        return this.removeById(id);
    }

    /**
     * 获取已发布数据流详情
     *
     * @param id d
     * @return r
     */
    @Override
    public DataFlowPublishDetailResponse detail(Long id) {
        DataFlowPublish byId = this.getById(id);
        if (byId != null) {
            DataFlowPublishDetailResponse dataFlowPublishDetailResponse = new DataFlowPublishDetailResponse();
            this.orikaMapper.map(byId, dataFlowPublishDetailResponse);
            dataFlowPublishDetailResponse.setDesign(JSON.parseObject(byId.getDesign()));
            String specifyInstances = byId.getSpecifyInstances();
            if (StrUtil.isNotBlank(specifyInstances)) {
                dataFlowPublishDetailResponse.setSpecifyInstances(JSON.parseArray(specifyInstances, String.class));
            }
            return dataFlowPublishDetailResponse;
        }
        return null;
    }

}
