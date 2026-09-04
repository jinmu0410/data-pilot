package cn.datapilot.web.service.flow.impl;

import cn.datapilot.common.component.OrikaMapper;
import cn.datapilot.common.enums.RedisKey;
import cn.datapilot.common.enums.Status;
import cn.datapilot.common.enums.flow.FlowStatus;
import cn.datapilot.common.exception.ApiException;
import cn.datapilot.common.util.CronUtils;
import cn.datapilot.common.util.VersionUtils;
import cn.datapilot.common.vo.base.PageBase;
import cn.datapilot.common.vo.base.PageRequest;
import cn.datapilot.common.vo.base.PageResult;
import cn.datapilot.common.vo.flow.FlowError;
import cn.datapilot.common.vo.flow.FlowHeartbeat;
import cn.datapilot.web.annotation.OperationLog;
import cn.datapilot.web.config.Context;
import cn.datapilot.web.enums.OperationLogAction;
import cn.datapilot.web.enums.OperationLogFunction;
import cn.datapilot.web.service.OperationLogService;
import cn.datapilot.web.service.UserService;
import cn.datapilot.web.service.datasource.DataSourceService;
import cn.datapilot.web.service.flow.DataFlowPublishService;
import cn.datapilot.web.service.flow.DataFlowService;
import cn.datapilot.web.service.flow.FlowRunService;
import cn.datapilot.web.service.task.TaskParamsHelper;
import cn.datapilot.web.service.task.TaskType;
import cn.datapilot.web.store.entity.DataFlow;
import cn.datapilot.web.store.entity.DataFlowPublish;
import cn.datapilot.web.store.entity.DataSource;
import cn.datapilot.web.store.entity.User;
import cn.datapilot.web.store.mapper.DataFlowMapper;
import cn.datapilot.web.vo.data.flow.*;
import cn.datapilot.web.vo.user.UserData;
import cn.datapilot.web.vo.workspace.WorkspaceData;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/1/4
 * @since 1.0.0
 */
@Service
public class DataFlowServiceImpl extends ServiceImpl<DataFlowMapper, DataFlow> implements DataFlowService {

    @Resource
    private DataFlowPublishService dataFlowPublishService;
    @Resource
    private OrikaMapper orikaMapper;
    @Resource
    private OperationLogService operationLogService;
    @Resource
    private UserService userService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private DataSourceService dataSourceService;
    @Resource
    private FlowRunService flowRunService;
    @Resource
    private TaskParamsHelper taskParamsHelper;

    /**
     * 数据流列表
     *
     * @param pageRequest p
     * @return r
     */
    @Override
    public PageResult<DataFlowListResponse> list(PageRequest<DataFlowListRequest> pageRequest) {
        WorkspaceData workspace = Context.getWorkspace();
        PageBase page = pageRequest.getPage();
        DataFlowListRequest query = pageRequest.getQuery();
        Page<DataFlow> dataFlowPage = this.lambdaQuery()
                .and(StrUtil.isNotBlank(query.getKeyword()), q -> q
                        .like(DataFlow::getName, query.getKeyword())
                        .or()
                        .like(DataFlow::getCode, query.getKeyword())
                )
                .like(StrUtil.isNotBlank(query.getName()), DataFlow::getName, query.getName())
                .eq(StrUtil.isNotBlank(query.getCode()), DataFlow::getCode, query.getCode())
                .eq(DataFlow::getWorkspaceCode, workspace.getCode())
                .last("ORDER BY CASE status " +
                        "WHEN 'ENABLE' THEN 1 " +
                        "WHEN 'PAUSE' THEN 2 " +
                        "WHEN 'TBP' THEN 3 " +
                        "ELSE 4 END ASC, update_time DESC")
                .page(new Page<>(page.getCurrent(), page.getSize()));
        PageResult<DataFlowListResponse> pageResult = new PageResult<>();
        List<DataFlow> records = dataFlowPage.getRecords();
        if (CollUtil.isEmpty(records)) {
            pageResult.setData(CollUtil.newArrayList(), page.getCurrent(), page.getSize(), 0L);
            return pageResult;
        }
        List<Long> flowIds = records.stream().map(DataFlow::getId).toList();
        List<cn.datapilot.web.store.entity.OperationLog> operationLogs = this.operationLogService.lambdaQuery()
                .select(cn.datapilot.web.store.entity.OperationLog::getUserId, cn.datapilot.web.store.entity.OperationLog::getRecordId)
                .eq(cn.datapilot.web.store.entity.OperationLog::getWorkspaceCode, workspace.getCode())
                .eq(cn.datapilot.web.store.entity.OperationLog::getFunction, OperationLogFunction.DATA_FLOW.name())
                .in(cn.datapilot.web.store.entity.OperationLog::getRecordId, flowIds)
                .groupBy(cn.datapilot.web.store.entity.OperationLog::getUserId, cn.datapilot.web.store.entity.OperationLog::getRecordId)
                .list();
        // 按照数据流id分组 value是user list
        Map<Long, List<Long>> map = operationLogs.stream().collect(Collectors.groupingBy(cn.datapilot.web.store.entity.OperationLog::getRecordId,
                Collectors.mapping(cn.datapilot.web.store.entity.OperationLog::getUserId, Collectors.toList())));
        Set<Long> userIds = operationLogs.stream().map(cn.datapilot.web.store.entity.OperationLog::getUserId)
                .collect(Collectors.toSet());
        Map<Long, User> userMap;
        if (CollUtil.isNotEmpty(userIds)) {
            List<User> list = this.userService.lambdaQuery()
                    .select(User::getId, User::getUsername, User::getAvatar)
                    .in(User::getId, userIds).list();
            userMap = list.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        } else {
            userMap = new HashMap<>();
        }
        // publish
        List<String> codes = records.stream().map(DataFlow::getCode).toList();
        List<DataFlowPublish> dataFlowPublishes = this.dataFlowPublishService.lambdaQuery()
                .eq(DataFlowPublish::getWorkspaceCode, workspace.getCode())
                .in(DataFlowPublish::getCode, codes)
                .in(DataFlowPublish::getStatus, Arrays.asList(FlowStatus.ENABLE.name(), FlowStatus.PAUSE.name()))
                .list();
        // 按照code+版本to map
        Map<String, DataFlowPublish> dataFlowPublishMap = dataFlowPublishes.stream()
                .collect(Collectors.toMap(m -> String.format("%s-%s", m.getCode(), m.getVersion()), Function.identity()));
        Long currentUserId = Context.getUser().getId();
        List<DataFlowListResponse> collect = records.parallelStream()
                .map(m -> {
                    DataFlowListResponse dataFlowListResponse = new DataFlowListResponse();
                    this.orikaMapper.map(m, dataFlowListResponse);
                    // 编辑过的用户
                    List<Long> uIds = map.get(m.getId());
                    if (CollUtil.isEmpty(uIds)) {
                        uIds = new ArrayList<>();
                    } else {
                        uIds.remove(m.getCreateUserId());
                        // 去重复
                        uIds = uIds.stream().distinct().collect(Collectors.toList());
                    }
                    // 把创建者放在第一个
                    uIds.addFirst(m.getCreateUserId());
                    // 如果有当前登录用户，登录用户放在第二个
                    if (uIds.contains(currentUserId) && !Objects.equals(m.getCreateUserId(), currentUserId)) {
                        // 如果只有一个，不需要调整
                        if (uIds.size() > 2) {
                            uIds.remove(currentUserId);
                            uIds.add(1, currentUserId);
                        }
                    }
                    // 最多返回6个
                    uIds = CollUtil.sub(uIds, 0, 6);
                    List<UserData> users = uIds.stream().map(userMap::get)
                            .filter(Objects::nonNull)
                            .map(u -> {
                                UserData user = new UserData();
                                this.orikaMapper.map(u, user);
                                return user;
                            })
                            .collect(Collectors.toList());
                    dataFlowListResponse.setUsers(users);
                    // 已发布的
                    DataFlowPublish dataFlowPublish = dataFlowPublishMap.get(String.format("%s-%s", m.getCode(), m.getPublishVersion()));
                    if (dataFlowPublish != null) {
                        String key = String.format("%s-%s", m.getWorkspaceCode(), m.getCode());
                        dataFlowListResponse.setPublishId(dataFlowPublish.getId());
                        // 查询flow服务是否被标记为执行异常,异常则标记为异常状态
                        RList<FlowError> flowErrors = this.redissonClient.getList(RedisKey.FLOW_ERROR.build(key));
                        if (flowErrors.isExists()) {
                            dataFlowListResponse.setFlowErrors(flowErrors.readAll());
                        }
                        RMap<String, FlowHeartbeat> rMap = this.redissonClient.getMap(RedisKey.FLOW_HEARTBEAT.build(key));
                        dataFlowListResponse.setFlowHeartbeats(rMap.values());
                    }
                    return dataFlowListResponse;
                }).collect(Collectors.toList());
        pageResult.setData(collect, dataFlowPage.getCurrent(), dataFlowPage.getSize(), dataFlowPage.getTotal());
        return pageResult;
    }


    /**
     * 创建数据流
     *
     * @param dataFlowCreateRequest d
     * @return r
     */
    @OperationLog(function = OperationLogFunction.DATA_FLOW, action = OperationLogAction.ADD,
            requestExtractId = false, id = "#id")
    @Override
    public DataFlowCreateResponse create(DataFlowCreateRequest dataFlowCreateRequest) {
        WorkspaceData workspace = Context.getWorkspace();
        // 检查名称是否重复
        if (this.lambdaQuery().eq(DataFlow::getName, dataFlowCreateRequest.getName())
                .eq(DataFlow::getWorkspaceCode, workspace.getCode())
                .exists()) {
            throw new ApiException("数据流名称已经存在");
        }
        DataFlow dataFlow = new DataFlow();
        this.orikaMapper.map(dataFlowCreateRequest, dataFlow);
        dataFlow.setCode(UUID.fastUUID().toString(true));
        dataFlow.setCreateUserId(Context.getUser().getId());
        dataFlow.setStatus(FlowStatus.TBP.name());
        dataFlow.setWorkspaceCode(workspace.getCode());
        dataFlow.setCurrentVersion(VersionUtils.INIT_VERSION);
        this.save(dataFlow);
        DataFlowCreateResponse dataFlowCreateResponse = new DataFlowCreateResponse();
        dataFlowCreateResponse.setId(dataFlow.getId());
        dataFlowCreateResponse.setCode(dataFlow.getCode());
        return dataFlowCreateResponse;
    }


    /**
     * 更新数据流
     *
     * @param dataFlowUpdateRequest d
     * @return r
     */
    @OperationLog(function = OperationLogFunction.DATA_FLOW, action = OperationLogAction.UPDATE,
            id = "#dataFlowUpdateRequest.id")
    @Override
    public Boolean update(DataFlowUpdateRequest dataFlowUpdateRequest) {
        // 排除掉自己，检查名称是否存在
        if (this.lambdaQuery().eq(DataFlow::getName, dataFlowUpdateRequest.getName())
                .ne(DataFlow::getId, dataFlowUpdateRequest.getId())
                .eq(DataFlow::getWorkspaceCode, Context.getWorkspace().getCode())
                .exists()) {
            throw new ApiException("数据流名称已经存在");
        }
        DataFlow dataFlow = this.getById(dataFlowUpdateRequest.getId());
        if (dataFlow == null) {
            throw new ApiException("数据流不存在");
        }
        String designString = dataFlowUpdateRequest.getDesign();
        // 更新版本
        if (StrUtil.isBlank(dataFlow.getCurrentVersion())) {
            dataFlow.setCurrentVersion(VersionUtils.INIT_VERSION);
        } else {
            // 如果已经发布过，开始更新版本号
            if (StrUtil.isNotBlank(dataFlow.getPublishVersion())
                    // 并且存在数据流信息更新
                    && StrUtil.isNotBlank(designString)
            ) {
                // 如果测试与已经发布版本一致,则需要更新一个版本号
                if (dataFlow.getCurrentVersion().equals(dataFlow.getPublishVersion())) {
                    // 获取下一个版本
                    dataFlow.setCurrentVersion(VersionUtils.getNextVersion(dataFlow.getCurrentVersion()));
                } else {
                    // 更新小版本
                    dataFlow.setCurrentVersion(VersionUtils.getNextVersion(dataFlow.getCurrentVersion(), true));
                }
            }
        }
        this.orikaMapper.map(dataFlowUpdateRequest, dataFlow);
        List<String> specifyInstances = dataFlowUpdateRequest.getSpecifyInstances();
        if (specifyInstances != null) {
            dataFlow.setSpecifyInstances(JSON.toJSONString(specifyInstances));
        }
        // cron 变更后重置调度游标，交给调度器重臂
        dataFlow.setNextExecTime(null);
        this.taskParamsHelper.validateCron(dataFlowUpdateRequest.getCron());
        if (StrUtil.isNotBlank(designString)) {
            TaskFlowDesign design = JSON.parseObject(designString, TaskFlowDesign.class);
            if (design == null || CollUtil.isEmpty(design.getNodes())) {
                throw new ApiException("任务流节点不能为空");
            }
            // 收集引用数据源
            Set<String> datasourceCodeSet = new HashSet<>();
            for (TaskFlowDesign.FlowNode node : design.getNodes()) {
                if (node.getProperties() == null) {
                    continue;
                }
                this.collectDatasourceCodes(datasourceCodeSet, node.getType(), node.getProperties());
            }
            dataFlow.setDatasourceCodes(JSON.toJSONString(datasourceCodeSet));
            if (CollUtil.isNotEmpty(datasourceCodeSet)) {
                List<DataSource> dataSources = this.dataSourceService.lambdaQuery()
                        .select(DataSource::getStatus, DataSource::getCode)
                        .eq(DataSource::getWorkspaceCode, dataFlow.getWorkspaceCode())
                        .in(DataSource::getCode, datasourceCodeSet)
                        .list();
                if (datasourceCodeSet.size() != dataSources.size()) {
                    Set<String> exists = dataSources.stream().map(DataSource::getCode).collect(Collectors.toSet());
                    Set<String> missing = new HashSet<>(datasourceCodeSet);
                    missing.removeAll(exists);
                    throw new ApiException("数据源不存在:" + String.join(",", missing));
                }
                List<String> disableCodes = dataSources.stream()
                        .filter(dataSource -> !Objects.equals(dataSource.getStatus(), Status.ENABLE.name()))
                        .map(DataSource::getCode).toList();
                if (CollUtil.isNotEmpty(disableCodes)) {
                    throw new ApiException("数据源状态禁用:" + String.join(",", disableCodes));
                }
            }
            // 非暂存：完整校验节点参数 + DAG 无环
            if (Objects.equals(dataFlowUpdateRequest.getTemporarily(), false)) {
                for (TaskFlowDesign.FlowNode node : design.getNodes()) {
                    this.taskParamsHelper.validate(node.getType(), node.getProperties(), dataFlow.getWorkspaceCode());
                }
                this.taskParamsHelper.topoSort(design);
            }
            dataFlow.setDesign(designString);
        }
        return this.updateById(dataFlow);
    }

    /**
     * 获取数据流详情
     *
     * @param id d
     * @return r
     */
    @Override
    public DataFlowDetailResponse detail(Long id) {
        DataFlow dataFlow = this.getById(id);
        if (dataFlow == null) {
            return null;
        }
        DataFlowDetailResponse dataFlowDetailResponse = new DataFlowDetailResponse();
        this.orikaMapper.map(dataFlow, dataFlowDetailResponse);
        dataFlowDetailResponse.setDesign(JSON.parseObject(dataFlow.getDesign()));
        String specifyInstances = dataFlow.getSpecifyInstances();
        if (StrUtil.isNotBlank(specifyInstances)) {
            dataFlowDetailResponse.setSpecifyInstances(JSON.parseArray(specifyInstances, String.class));
        } else {
            dataFlowDetailResponse.setSpecifyInstances(Collections.emptyList());
        }
        return dataFlowDetailResponse;
    }

    /**
     * 发布
     *
     * @param publishRequest d
     * @return r
     */
    @OperationLog(function = OperationLogFunction.DATA_FLOW, action = OperationLogAction.PUBLISH,
            id = "#publishRequest.id")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean publish(PublishRequest publishRequest) {
        Long id = publishRequest.getId();
        DataFlow dataFlow = this.getById(id);
        if (dataFlow == null) {
            return false;
        }
        // 如果已经发布版本与当前版本一致
        if (Objects.equals(dataFlow.getPublishVersion(), dataFlow.getCurrentVersion())) {
            throw new ApiException("当前版本已经发布");
        }
        // 校验 design + cron
        String flowDesign = dataFlow.getDesign();
        TaskFlowDesign design = JSON.parseObject(flowDesign, TaskFlowDesign.class);
        if (design == null || CollUtil.isEmpty(design.getNodes())) {
            throw new ApiException("任务流节点不能为空");
        }
        for (TaskFlowDesign.FlowNode node : design.getNodes()) {
            this.taskParamsHelper.validate(node.getType(), node.getProperties(), dataFlow.getWorkspaceCode());
        }
        this.taskParamsHelper.topoSort(design);
        this.taskParamsHelper.validateCron(dataFlow.getCron());

        dataFlow.setStatus(Status.ENABLE.name());
        dataFlow.setNextExecTime(null);
        // 原来的版本变为禁用状态
        this.dataFlowPublishService.lambdaUpdate()
                .set(DataFlowPublish::getStatus, FlowStatus.HISTORY.name())
                .eq(DataFlowPublish::getCode, dataFlow.getCode())
                .eq(DataFlowPublish::getWorkspaceCode, dataFlow.getWorkspaceCode())
                .in(DataFlowPublish::getStatus, Arrays.asList(FlowStatus.ENABLE.name(), FlowStatus.PAUSE.name()))
                .update();
        // 生成新的发布版本
        DataFlowPublish dataFlowPublish = new DataFlowPublish();
        this.orikaMapper.map(dataFlow, dataFlowPublish);
        dataFlowPublish.setId(null);
        // 重新填充当前时间
        dataFlowPublish.setCreateTime(null);
        dataFlowPublish.setUpdateTime(null);
        dataFlowPublish.setVersion(dataFlow.getCurrentVersion());
        dataFlowPublish.setCreateUserId(Context.getUser().getId());
        dataFlowPublish.setPublishDescription(publishRequest.getPublishDescription());
        this.dataFlowPublishService.save(dataFlowPublish);
        dataFlow.setPublishVersion(dataFlow.getCurrentVersion());
        this.updateById(dataFlow);
        // 清理旧版本的异常信息
        RList<FlowError> flowErrors = this.redissonClient.getList(
                RedisKey.FLOW_ERROR.build(dataFlowPublish.getWorkspaceCode() + "-" + dataFlowPublish.getCode()));
        flowErrors.delete();
        return true;
    }

    /**
     * 启动流程
     *
     * @param id d
     * @return r
     */
    @OperationLog(function = OperationLogFunction.DATA_FLOW, action = OperationLogAction.START,
            id = "#id")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean start(Long id) {
        DataFlow dataFlow = this.getById(id);
        if (dataFlow == null) {
            throw new ApiException("数据流不存在");
        }
        if (StrUtil.equals(dataFlow.getStatus(), Status.ENABLE.name())) {
            throw new ApiException("数据流已经启用");
        }
        if (StrUtil.isBlank(dataFlow.getPublishVersion())) {
            throw new ApiException("未发布的数据流不能启用");
        }
        dataFlow.setStatus(Status.ENABLE.name());
        dataFlow.setNextExecTime(null);
        this.updateById(dataFlow);
        DataFlowPublish dataFlowPublish = this.dataFlowPublishService.lambdaQuery()
                .eq(DataFlowPublish::getWorkspaceCode, dataFlow.getWorkspaceCode())
                .eq(DataFlowPublish::getCode, dataFlow.getCode())
                .eq(DataFlowPublish::getVersion, dataFlow.getPublishVersion())
                .one();
        if (dataFlowPublish == null) {
            throw new ApiException("已发布的版本不存在");
        }
        if (StrUtil.equals(dataFlowPublish.getStatus(), Status.ENABLE.name())) {
            throw new ApiException("已发布数据流已经启用");
        }
        dataFlowPublish.setStatus(Status.ENABLE.name());
        this.dataFlowPublishService.updateById(dataFlowPublish);
        RList<FlowError> flowErrors = this.redissonClient.getList(RedisKey.FLOW_ERROR.build(
                dataFlowPublish.getWorkspaceCode() + "-" + dataFlowPublish.getCode())
        );
        flowErrors.delete();
        return true;
    }

    /**
     * 停止流程
     *
     * @param id d
     * @return r
     */
    @OperationLog(function = OperationLogFunction.DATA_FLOW, action = OperationLogAction.STOP,
            id = "#id")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean stop(Long id) {
        DataFlow dataFlow = this.getById(id);
        if (dataFlow == null) {
            return false;
        }
        dataFlow.setStatus(FlowStatus.PAUSE.name());
        dataFlow.setNextExecTime(null);
        this.updateById(dataFlow);
        DataFlowPublish dataFlowPublish = this.dataFlowPublishService.lambdaQuery()
                .eq(DataFlowPublish::getWorkspaceCode, dataFlow.getWorkspaceCode())
                .eq(DataFlowPublish::getCode, dataFlow.getCode())
                .eq(DataFlowPublish::getStatus, FlowStatus.ENABLE.name())
                .one();
        if (dataFlowPublish != null) {
            this.dataFlowPublishService.lambdaUpdate()
                    .set(DataFlowPublish::getStatus, FlowStatus.PAUSE.name())
                    .eq(DataFlowPublish::getId, dataFlowPublish.getId())
                    .update();
            RList<FlowError> flowErrors = this.redissonClient.getList(RedisKey.FLOW_ERROR.build(
                    dataFlowPublish.getWorkspaceCode() + "-" + dataFlowPublish.getCode()));
            flowErrors.delete();
        }
        return true;
    }

    /**
     * 删除流程
     *
     * @param id d
     * @return r
     */
    @OperationLog(function = OperationLogFunction.DATA_FLOW, action = OperationLogAction.DELETE,
            id = "#id")
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean delete(Long id) {
        DataFlow dataFlow = this.getById(id);
        if (dataFlow == null) {
            return false;
        }
        // 如果运行中的，二次确认需要先停用，才能删除
        if (StrUtil.equals(dataFlow.getStatus(), FlowStatus.ENABLE.name())) {
            throw new ApiException("请先停止数据流后再进行删除");
        }
        this.removeById(id);
        // 清理异常信息
        RList<FlowError> flowErrors = this.redissonClient.getList(RedisKey.FLOW_ERROR.build(
                dataFlow.getWorkspaceCode() + "-" + dataFlow.getCode()));
        flowErrors.delete();
        // 删除已发布数据流数据
        this.dataFlowPublishService.lambdaUpdate()
                .eq(DataFlowPublish::getCode, dataFlow.getCode())
                .eq(DataFlowPublish::getWorkspaceCode, dataFlow.getWorkspaceCode())
                .remove();
        return true;
    }

    /**
     * 回滚至某个版本
     *
     * @param id id
     * @return r
     */
    @OperationLog(function = OperationLogFunction.DATA_FLOW, action = OperationLogAction.ROLLBACK,
            id = "#id")
    @Override
    public Boolean rollback(Long id) {
        final DataFlowPublish dataFlowPublish = this.dataFlowPublishService.getById(id);
        if (dataFlowPublish == null) {
            throw new ApiException("回滚的版本不存在");
        }
        DataFlow dataFlow = this.lambdaQuery()
                .eq(DataFlow::getCode, dataFlowPublish.getCode())
                .eq(DataFlow::getWorkspaceCode, dataFlowPublish.getWorkspaceCode())
                .one();
        if (dataFlow == null) {
            return false;
        }
        Long flowId = dataFlow.getId();
        String status = dataFlow.getStatus();
        String currentVersion = dataFlow.getCurrentVersion();
        String publishVersion = dataFlow.getPublishVersion();
        this.orikaMapper.map(dataFlowPublish, dataFlow);
        // 上方复制，导致ID错乱
        dataFlow.setId(flowId);
        dataFlow.setStatus(status);
        if (currentVersion.equals(publishVersion)) {
            // 如果没有待发布，生成一个大版本
            dataFlow.setCurrentVersion(VersionUtils.getNextVersion(currentVersion));
        } else {
            // 当前已经是待发布，版本保留
            dataFlow.setCurrentVersion(currentVersion);
        }
        return this.updateById(dataFlow);
    }

    @Override
    public Long run(Long id, String failureStrategy) {
        return this.flowRunService.run(id, "MANUAL", failureStrategy);
    }

    @Override
    public List<DataFlow> listEnabledCronFlows() {
        return this.lambdaQuery()
                .eq(DataFlow::getStatus, Status.ENABLE.name())
                .isNotNull(DataFlow::getCron)
                .ne(DataFlow::getCron, "")
                .list();
    }

    @Override
    public void advanceNextExecTime(DataFlow flow) {
        if (StrUtil.isBlank(flow.getCron()) || !CronUtils.isValid(flow.getCron())) {
            flow.setNextExecTime(null);
        } else {
            List<ZonedDateTime> nexts = CronUtils.nextExecutionTime(flow.getCron(), ZonedDateTime.now(), 1);
            flow.setNextExecTime(nexts.isEmpty() ? null
                    : nexts.get(0).withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime());
        }
        this.updateById(flow);
    }

    private void collectDatasourceCodes(Set<String> codes, String type, TaskFlowNode props) {
        try {
            switch (TaskType.of(type)) {
                case SQL -> this.addIfNotBlank(codes, props.getDatasourceCode());
                case DATAX, SEATUNNEL -> {
                    this.addIfNotBlank(codes, props.getSourceDataSourceCode());
                    this.addIfNotBlank(codes, props.getTargetDataSourceCode());
                }
                default -> {
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void addIfNotBlank(Set<String> set, String code) {
        if (StrUtil.isNotBlank(code)) {
            set.add(code);
        }
    }
}

