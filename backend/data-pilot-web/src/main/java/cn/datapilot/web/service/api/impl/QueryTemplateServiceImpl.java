package cn.datapilot.web.service.api.impl;

import cn.datapilot.common.component.OrikaMapper;
import cn.datapilot.common.enums.RedisKey;
import cn.datapilot.common.enums.Status;
import cn.datapilot.common.exception.ApiException;
import cn.datapilot.common.util.AesUtils;
import cn.datapilot.common.vo.base.PageBase;
import cn.datapilot.common.vo.base.PageRequest;
import cn.datapilot.common.vo.base.PageResult;
import cn.datapilot.web.config.Context;
import cn.datapilot.web.interceptor.TraceInterceptor;
import cn.datapilot.web.service.api.QueryTemplateService;
import cn.datapilot.web.service.datasource.DataSourceService;
import cn.datapilot.web.store.entity.DataSource;
import cn.datapilot.web.store.entity.QueryLog;
import cn.datapilot.web.store.entity.QueryTemplate;
import cn.datapilot.web.store.entity.QueryTemplatePublish;
import cn.datapilot.web.store.mapper.QueryLogMapper;
import cn.datapilot.web.store.mapper.QueryTemplateMapper;
import cn.datapilot.web.store.mapper.QueryTemplatePublishMapper;
import cn.datapilot.web.vo.data.service.*;
import cn.datapilot.web.vo.workspace.WorkspaceData;
import cn.hutool.cache.impl.TimedCache;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.*;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 数据服务-查询模板实现
 *
 * @author jinmu
 * @date 2025/2/2
 * @since 1.0.0
 */
@Slf4j
@Service
public class QueryTemplateServiceImpl extends ServiceImpl<QueryTemplateMapper, QueryTemplate>
        implements QueryTemplateService {

    /**
     * 预览行数上限
     */
    private static final int PREVIEW_ROW_LIMIT = 200;

    /**
     * 对外分页单页上限，避免超大请求占满连接与内存
     */
    private static final int MAX_PAGE_SIZE = 1000;

    /**
     * 响应参数记录上限（字符）
     */
    private static final int RESPONSE_ARG_LIMIT = 2000;

    /**
     * 占位符：${param}
     */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([a-zA-Z0-9_]+)}");

    /**
     * 合法调用方法
     */
    private static final Set<String> METHODS = Set.of("one", "count", "list", "page");

    private static final String AUTH_PUBLIC = "PUBLIC";
    private static final String AUTH_API_KEY = "API_KEY";
    private static final String AUTH_HMAC_SHA256 = "HMAC_SHA256";
    private static final String API_KEY_HASH_PREFIX = "key$";
    private static final String API_KEY_ENCRYPTED_PREFIX = "api$";
    private static final String HMAC_PREFIX = "hmac$";
    private static final String LIMIT_GLOBAL = "ENABLE";
    private static final String LIMIT_IP = "LIMIT_IP";
    private static final long SIGNATURE_TOLERANCE_SECONDS = 300;

    /**
     * 进程内结果缓存（TTL 60s）
     */
    private final TimedCache<String, QueryCallResponse> cache = new TimedCache<>(60 * 1000);

    @Resource
    private OrikaMapper orikaMapper;
    @Resource
    private DataSourceService dataSourceService;
    @Resource
    private QueryTemplatePublishMapper queryTemplatePublishMapper;
    @Resource
    private QueryLogMapper queryLogMapper;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    @Value("${dp.password.secret-key:0200300020000001}")
    private String apiEncryptionKey;

    @Override
    public PageResult<QueryTemplateListResponse> list(PageRequest<QueryTemplateListRequest> pageRequest) {
        WorkspaceData workspace = Context.getWorkspace();
        PageBase page = pageRequest.getPage();
        QueryTemplateListRequest query = Optional.ofNullable(pageRequest.getQuery()).orElse(new QueryTemplateListRequest());
        Page<QueryTemplate> templatePage = this.lambdaQuery()
                .and(StrUtil.isNotBlank(query.getKeyword()), q -> q
                        .like(QueryTemplate::getName, query.getKeyword())
                        .or()
                        .like(QueryTemplate::getCode, query.getKeyword()))
                .eq(StrUtil.isNotBlank(query.getDataSourceCode()), QueryTemplate::getDataSourceCode, query.getDataSourceCode())
                .eq(StrUtil.isNotBlank(query.getStatus()), QueryTemplate::getStatus, query.getStatus())
                .eq(QueryTemplate::getWorkspaceCode, workspace.getCode())
                .orderByDesc(QueryTemplate::getUpdateTime)
                .page(new Page<>(page.getCurrent(), page.getSize()));
        PageResult<QueryTemplateListResponse> pageResult = new PageResult<>();
        List<QueryTemplate> records = templatePage.getRecords();
        if (CollUtil.isEmpty(records)) {
            pageResult.setData(CollUtil.newArrayList(), page.getCurrent(), page.getSize(), 0L);
            return pageResult;
        }
        Map<String, String> dsNameMap = this.dataSourceNameMap(workspace.getCode(),
                records.stream().map(QueryTemplate::getDataSourceCode).filter(StrUtil::isNotBlank).collect(Collectors.toSet()));
        List<QueryTemplateListResponse> collect = records.stream().map(m -> {
            QueryTemplateListResponse response = new QueryTemplateListResponse();
            this.orikaMapper.map(m, response);
            response.setDataSourceName(dsNameMap.get(m.getDataSourceCode()));
            return response;
        }).collect(Collectors.toList());
        pageResult.setData(collect, templatePage.getCurrent(), templatePage.getSize(), templatePage.getTotal());
        return pageResult;
    }

    @Override
    public QueryTemplateDetailResponse detail(Long id) {
        WorkspaceData workspace = Context.getWorkspace();
        QueryTemplate template = this.resolveTemplate(id, workspace.getCode());
        if (template == null) {
            return null;
        }
        QueryTemplateDetailResponse response = new QueryTemplateDetailResponse();
        this.orikaMapper.map(template, response);
        AuthConfig auth = this.resolveAuth(template.getSecret());
        response.setAuthType(auth.type());
        response.setSecret(null);
        response.setHasSecret(StrUtil.isNotBlank(template.getSecret()));
        if (StrUtil.isNotBlank(template.getDataSourceCode())) {
            Map<String, String> dsNameMap = this.dataSourceNameMap(template.getWorkspaceCode(), Set.of(template.getDataSourceCode()));
            response.setDataSourceName(dsNameMap.get(template.getDataSourceCode()));
        }
        return response;
    }

    @Override
    public Long add(QueryTemplateAddRequest request) {
        WorkspaceData workspace = Context.getWorkspace();
        if (this.lambdaQuery().eq(QueryTemplate::getName, request.getName())
                .eq(QueryTemplate::getWorkspaceCode, workspace.getCode())
                .exists()) {
            throw new ApiException("模板名称已存在");
        }
        this.resolveDataSource(request.getDataSourceCode(), workspace.getCode());
        this.validateReadOnlyTemplate(request.getTemplate());
        QueryTemplate template = new QueryTemplate();
        this.orikaMapper.map(request, template);
        template.setCode(UUID.fastUUID().toString(true));
        template.setWorkspaceCode(workspace.getCode());
        template.setCreateUserId(Context.getUser().getId());
        if (StrUtil.isBlank(template.getStatus())) {
            template.setStatus(Status.ENABLE.name());
        }
        if (template.getTimeout() == null) {
            template.setTimeout(30);
        }
        this.save(template);
        return template.getId();
    }

    @Override
    public Boolean update(QueryTemplateUpdateRequest request) {
        WorkspaceData workspace = Context.getWorkspace();
        if (this.lambdaQuery().eq(QueryTemplate::getName, request.getName())
                .ne(QueryTemplate::getId, request.getId())
                .eq(QueryTemplate::getWorkspaceCode, workspace.getCode())
                .exists()) {
            throw new ApiException("模板名称已存在");
        }
        QueryTemplate template = this.resolveTemplate(request.getId(), workspace.getCode());
        if (template == null) {
            throw new ApiException("模板不存在");
        }
        this.resolveDataSource(request.getDataSourceCode(), workspace.getCode());
        this.validateReadOnlyTemplate(request.getTemplate());
        this.orikaMapper.map(request, template);
        this.updateById(template);
        return true;
    }

    @Override
    public Boolean delete(Long id) {
        WorkspaceData workspace = Context.getWorkspace();
        QueryTemplate template = this.resolveTemplate(id, workspace.getCode());
        if (template == null) {
            return false;
        }
        this.removeById(id);
        this.queryTemplatePublishMapper.delete(new LambdaQueryWrapper<QueryTemplatePublish>()
                .eq(QueryTemplatePublish::getCode, template.getCode())
                .eq(QueryTemplatePublish::getWorkspaceCode, workspace.getCode()));
        this.cache.clear();
        return true;
    }

    @Override
    public QueryTemplatePublishResponse publish(QueryTemplatePublishRequest request) {
        WorkspaceData workspace = Context.getWorkspace();
        QueryTemplate template = this.resolveTemplate(request.getId(), workspace.getCode());
        if (template == null) {
            throw new ApiException("模板不存在");
        }
        this.resolveDataSource(template.getDataSourceCode(), workspace.getCode());
        this.validateReadOnlyTemplate(template.getTemplate());
        long count = this.queryTemplatePublishMapper.selectCount(
                new LambdaQueryWrapper<QueryTemplatePublish>()
                        .eq(QueryTemplatePublish::getCode, template.getCode())
                        .eq(QueryTemplatePublish::getWorkspaceCode, workspace.getCode()));
        String version = "v" + (count + 1);

        if ("ENABLE".equals(request.getEnableLimiting())) {
            if (request.getLimitRate() == null || request.getLimitRate() < 1) {
                throw new ApiException("限流次数必须大于 0");
            }
            if (request.getLimitRefreshInterval() == null || request.getLimitRefreshInterval() < 1) {
                throw new ApiException("限流周期必须大于 0");
            }
            if (StrUtil.isBlank(request.getLimitTimeUnit())
                    || !Set.of("SECONDS", "MINUTES", "HOURS").contains(request.getLimitTimeUnit())) {
                throw new ApiException("限流时间单位不合法");
            }
        }

        String authType = StrUtil.blankToDefault(request.getAuthType(), AUTH_API_KEY).toUpperCase(Locale.ROOT);
        if (!Set.of(AUTH_PUBLIC, AUTH_API_KEY, AUTH_HMAC_SHA256).contains(authType)) {
            throw new ApiException("不支持的鉴权方式: {}", authType);
        }
        String rawSecret = StrUtil.trim(request.getSecret());
        AuthConfig currentAuth = this.resolveAuth(template.getSecret());
        boolean keepCurrentSecret = StrUtil.isBlank(rawSecret) && authType.equals(currentAuth.type())
                && StrUtil.isNotBlank(template.getSecret());
        if (!AUTH_PUBLIC.equals(authType) && StrUtil.isBlank(rawSecret) && !keepCurrentSecret) {
            throw new ApiException("启用鉴权时必须配置密钥");
        }
        if (StrUtil.isNotBlank(rawSecret) && rawSecret.getBytes(StandardCharsets.UTF_8).length < 16) {
            throw new ApiException("密钥长度不能少于 16 个 UTF-8 字节");
        }
        if (rawSecret != null && rawSecret.getBytes(StandardCharsets.UTF_8).length > 60) {
            throw new ApiException("密钥长度不能超过 60 个 UTF-8 字节");
        }

        QueryTemplatePublish publish = new QueryTemplatePublish();
        publish.setName(template.getName());
        publish.setCode(template.getCode());
        publish.setTemplate(template.getTemplate());
        publish.setWorkspaceCode(template.getWorkspaceCode());
        publish.setStatus(template.getStatus());
        publish.setDescription(template.getDescription());
        publish.setDataSourceCode(template.getDataSourceCode());
        publish.setTimeout(template.getTimeout());
        publish.setSecret(AUTH_PUBLIC.equals(authType) ? null
                : (keepCurrentSecret ? template.getSecret() : this.encodeAuth(authType, rawSecret)));
        publish.setEnableCache("ENABLE".equals(request.getEnableCache()) ? "ENABLE" : "DISABLE");
        String limitType = "IP".equalsIgnoreCase(request.getLimitType()) ? "IP" : "GLOBAL";
        publish.setEnableLimiting("ENABLE".equals(request.getEnableLimiting())
                ? ("IP".equals(limitType) ? LIMIT_IP : LIMIT_GLOBAL) : "DISABLE");
        publish.setLimitRate(request.getLimitRate());
        publish.setLimitRefreshInterval(request.getLimitRefreshInterval());
        publish.setLimitTimeUnit(request.getLimitTimeUnit());
        publish.setRecordLog("DISABLE".equals(request.getRecordLog()) ? "DISABLE" : "ENABLE");
        publish.setVersion(version);
        publish.setCreateUserId(Context.getUser().getId());
        this.queryTemplatePublishMapper.insert(publish);

        // 发布策略变更后清理旧限流器，下一次调用将按新版本重新初始化。
        this.redissonClient.getRateLimiter(RedisKey.QUERY_TEMPLATE_LIMIT.build(template.getCode())).delete();

        template.setSecret(publish.getSecret());
        template.setCurrentVersion(version);
        template.setPublishVersion(version);
        this.updateById(template);

        QueryTemplatePublishResponse response = new QueryTemplatePublishResponse();
        this.orikaMapper.map(publish, response);
        response.setAuthType(authType);
        response.setSecret(null);
        response.setLimitType(limitType);
        return response;
    }

    @Override
    public QueryExecuteResult test(QueryTemplateTestRequest request) {
        WorkspaceData workspace = Context.getWorkspace();
        String dataSourceCode;
        String templateText;
        Integer timeout;
        if (request.getId() != null) {
            QueryTemplate template = this.resolveTemplate(request.getId(), workspace.getCode());
            if (template == null) {
                throw new ApiException("模板不存在");
            }
            dataSourceCode = template.getDataSourceCode();
            templateText = template.getTemplate();
            timeout = template.getTimeout();
        } else {
            if (StrUtil.isBlank(request.getDataSourceCode())) {
                throw new ApiException("请选择数据源");
            }
            if (StrUtil.isBlank(request.getTemplate())) {
                throw new ApiException("模板不能为空");
            }
            dataSourceCode = request.getDataSourceCode();
            templateText = request.getTemplate();
            timeout = 30;
        }
        DataSource dataSource = this.resolveDataSource(dataSourceCode, workspace.getCode());
        PreparedSql prepared = this.prepareSql(templateText, request.getParams());
        javax.sql.DataSource ds = this.dataSourceService.dataSourceConnect(dataSource, javax.sql.DataSource.class);
        return this.runSelect(ds, prepared.sql(), prepared.bindValues(), timeout == null ? 30 : timeout, PREVIEW_ROW_LIMIT);
    }

    @Override
    public QueryCallResponse call(String code, String secret, String timestamp, String nonce, String signature,
                                  String requestBody, QueryCallRequest request, String ip) {
        QueryTemplatePublish publish = this.queryTemplatePublishMapper.selectOne(
                new LambdaQueryWrapper<QueryTemplatePublish>()
                        .eq(QueryTemplatePublish::getCode, code)
                        .eq(QueryTemplatePublish::getStatus, Status.ENABLE.name())
                        .orderByDesc(QueryTemplatePublish::getId)
                        .last("LIMIT 1"));
        if (publish == null) {
            throw new ApiException("接口不存在或未发布");
        }
        if (request == null) {
            throw new ApiException("请求体不能为空");
        }
        String requestId = TraceInterceptor.getRequestId();
        String method = StrUtil.isBlank(request.getMethod()) ? "list" : request.getMethod().toLowerCase();
        if (!METHODS.contains(method)) {
            throw new ApiException("不支持的调用方法: {}", method);
        }
        Map<String, Object> params = request.getParams();
        int pageNum = request.getPageNum() == null ? 1 : Math.max(1, request.getPageNum());
        int pageSize = request.getPageSize() == null ? 10 : Math.max(1, Math.min(MAX_PAGE_SIZE, request.getPageSize()));
        long start = System.currentTimeMillis();

        QueryLog queryLog = new QueryLog();
        queryLog.setWorkspaceCode(publish.getWorkspaceCode());
        queryLog.setTemplateCode(publish.getCode());
        queryLog.setTemplateName(publish.getName());
        queryLog.setMethod(method);
        queryLog.setRequestId(requestId);
        queryLog.setIp(ip);
        queryLog.setRequestArg(JSON.toJSONString(params));
        queryLog.setCreateTime(LocalDateTime.now());
        queryLog.setHitCache("NO");

        try {
            this.verifyAuth(publish, secret, timestamp, nonce, signature, requestBody);
            this.checkRateLimit(publish, ip);
            QueryCallResponse response;
            if ("ENABLE".equals(publish.getEnableCache())) {
                String cacheKey = this.buildCacheKey(publish.getCode(), publish.getVersion(), method, params, pageNum, pageSize);
                response = this.cache.get(cacheKey);
                if (response != null) {
                    queryLog.setHitCache("YES");
                    queryLog.setStatus("SUCCESS");
                    queryLog.setNumber(this.extractNumber(response));
                    queryLog.setResponseArg(this.truncate(JSON.toJSONString(response.getData())));
                    return response;
                }
                response = this.doExecute(publish, method, params, pageNum, pageSize);
                this.cache.put(cacheKey, response);
            } else {
                response = this.doExecute(publish, method, params, pageNum, pageSize);
            }
            queryLog.setStatus("SUCCESS");
            queryLog.setNumber(this.extractNumber(response));
            queryLog.setResponseArg(this.truncate(JSON.toJSONString(response.getData())));
            return response;
        } catch (Exception e) {
            queryLog.setStatus("FAIL");
            queryLog.setException(this.rootMessage(e));
            if (e instanceof ApiException) {
                throw (ApiException) e;
            }
            throw new ApiException("查询执行失败", e);
        } finally {
            queryLog.setCost(System.currentTimeMillis() - start);
            if ("ENABLE".equals(publish.getRecordLog())) {
                this.taskExecutor.execute(() -> {
                    try {
                        this.queryLogMapper.insert(queryLog);
                    } catch (Exception e) {
                        log.error("写入调用日志失败, templateCode:{}", publish.getCode(), e);
                    }
                });
            }
        }
    }

    @Override
    public PageResult<QueryLogListResponse> logList(PageRequest<QueryLogListRequest> pageRequest) {
        WorkspaceData workspace = Context.getWorkspace();
        PageBase page = pageRequest.getPage();
        QueryLogListRequest query = Optional.ofNullable(pageRequest.getQuery()).orElse(new QueryLogListRequest());
        Page<QueryLog> logPage = this.queryLogMapper.selectPage(
                new Page<>(page.getCurrent(), page.getSize()),
                new LambdaQueryWrapper<QueryLog>()
                        .eq(StrUtil.isNotBlank(query.getTemplateCode()), QueryLog::getTemplateCode, query.getTemplateCode())
                        .eq(StrUtil.isNotBlank(query.getStatus()), QueryLog::getStatus, query.getStatus())
                        .eq(QueryLog::getWorkspaceCode, workspace.getCode())
                        .orderByDesc(QueryLog::getId));
        PageResult<QueryLogListResponse> pageResult = new PageResult<>();
        List<QueryLogListResponse> collect = logPage.getRecords().stream().map(m -> {
            QueryLogListResponse response = new QueryLogListResponse();
            this.orikaMapper.map(m, response);
            return response;
        }).collect(Collectors.toList());
        pageResult.setData(collect, logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        return pageResult;
    }

    @Override
    public QueryLogDetailResponse logDetail(Long id) {
        WorkspaceData workspace = Context.getWorkspace();
        QueryLog queryLog = this.queryLogMapper.selectOne(new LambdaQueryWrapper<QueryLog>()
                .eq(QueryLog::getId, id)
                .eq(QueryLog::getWorkspaceCode, workspace.getCode()));
        if (queryLog == null) {
            return null;
        }
        QueryLogDetailResponse response = new QueryLogDetailResponse();
        this.orikaMapper.map(queryLog, response);
        return response;
    }

    /**
     * 实际执行查询并组装调用响应
     */
    private QueryCallResponse doExecute(QueryTemplatePublish publish, String method, Map<String, Object> params,
                                        int pageNum, int pageSize) {
        DataSource dataSource = this.resolveDataSource(publish.getDataSourceCode(), publish.getWorkspaceCode());
        javax.sql.DataSource ds = this.dataSourceService.dataSourceConnect(dataSource, javax.sql.DataSource.class);
        int timeout = publish.getTimeout() == null ? 30 : publish.getTimeout();
        PreparedSql prepared = this.prepareSql(publish.getTemplate(), params);

        QueryCallResponse response = new QueryCallResponse();
        response.setMethod(method);
        switch (method) {
            case "one" -> {
                QueryExecuteResult result = this.runSelect(ds, prepared.sql(), prepared.bindValues(), timeout, 1);
                response.setData(result.getRows().isEmpty() ? null : result.getRows().get(0));
            }
            case "count" -> response.setData(this.runCount(ds, prepared.sql(), prepared.bindValues(), timeout));
            case "page" -> {
                long total = this.runCount(ds, prepared.sql(), prepared.bindValues(), timeout);
                List<Object> bindValues = new ArrayList<>(prepared.bindValues());
                bindValues.add(pageSize);
                bindValues.add((long) (pageNum - 1) * pageSize);
                String pageSql = "SELECT * FROM (" + prepared.sql() + ") _t LIMIT ? OFFSET ?";
                QueryExecuteResult result = this.runSelect(ds, pageSql, bindValues, timeout, pageSize);
                Map<String, Object> pageData = new LinkedHashMap<>();
                pageData.put("records", result.getRows());
                pageData.put("total", total);
                pageData.put("current", pageNum);
                pageData.put("size", pageSize);
                response.setData(pageData);
            }
            default -> {
                QueryExecuteResult result = this.runSelect(ds, prepared.sql(), prepared.bindValues(), timeout, PREVIEW_ROW_LIMIT);
                response.setData(result.getRows());
            }
        }
        return response;
    }

    /**
     * 解析模板占位符为 ? 并收集绑定值
     */
    private PreparedSql prepareSql(String template, Map<String, Object> params) {
        this.validateReadOnlyTemplate(template);
        Matcher matcher = PLACEHOLDER.matcher(template);
        List<String> names = new ArrayList<>();
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            names.add(matcher.group(1));
            matcher.appendReplacement(sb, "?");
        }
        matcher.appendTail(sb);
        List<Object> bindValues = new ArrayList<>(names.size());
        for (String name : names) {
            if (params == null || !params.containsKey(name)) {
                throw new ApiException("缺少参数: {}", name);
            }
            bindValues.add(params.get(name));
        }
        return new PreparedSql(sb.toString(), bindValues);
    }

    /**
     * 数据 API 只允许单条只读查询。占位符只能用于值绑定，不能借助多语句执行写操作。
     */
    private void validateReadOnlyTemplate(String template) {
        if (StrUtil.isBlank(template)) {
            throw new ApiException("模板不能为空");
        }
        String normalized = this.stripLeadingComments(template).stripLeading();
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (!lower.matches("^(select|with)\\b[\\s\\S]*")) {
            throw new ApiException("API 模板只允许 SELECT 或 WITH 查询");
        }
        if (this.containsStatementSeparator(normalized)) {
            throw new ApiException("API 模板只允许单条 SQL，不支持多语句");
        }
    }

    private String stripLeadingComments(String sql) {
        String value = sql;
        boolean changed;
        do {
            changed = false;
            value = value.stripLeading();
            if (value.startsWith("--")) {
                int lineEnd = value.indexOf('\n');
                value = lineEnd < 0 ? "" : value.substring(lineEnd + 1);
                changed = true;
            } else if (value.startsWith("/*")) {
                int commentEnd = value.indexOf("*/", 2);
                if (commentEnd < 0) {
                    throw new ApiException("SQL 注释未闭合");
                }
                value = value.substring(commentEnd + 2);
                changed = true;
            }
        } while (changed);
        return value;
    }

    private boolean containsStatementSeparator(String sql) {
        boolean singleQuote = false;
        boolean doubleQuote = false;
        boolean backtick = false;
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
            if (!singleQuote && !doubleQuote && !backtick && current == '-' && next == '-') {
                int lineEnd = sql.indexOf('\n', i + 2);
                if (lineEnd < 0) return false;
                i = lineEnd;
                continue;
            }
            if (!singleQuote && !doubleQuote && !backtick && current == '/' && next == '*') {
                int commentEnd = sql.indexOf("*/", i + 2);
                if (commentEnd < 0) throw new ApiException("SQL 注释未闭合");
                i = commentEnd + 1;
                continue;
            }
            if (!doubleQuote && !backtick && current == '\'' && (i == 0 || sql.charAt(i - 1) != '\\')) singleQuote = !singleQuote;
            else if (!singleQuote && !backtick && current == '"' && (i == 0 || sql.charAt(i - 1) != '\\')) doubleQuote = !doubleQuote;
            else if (!singleQuote && !doubleQuote && current == '`') backtick = !backtick;
            else if (!singleQuote && !doubleQuote && !backtick && current == ';' && !sql.substring(i + 1).trim().isEmpty()) return true;
        }
        return false;
    }

    /**
     * 执行 SELECT 并读取最多 rowLimit 行
     */
    private QueryExecuteResult runSelect(javax.sql.DataSource ds, String sql, List<Object> bindValues,
                                         int timeout, int rowLimit) {
        QueryExecuteResult result = new QueryExecuteResult();
        long start = System.currentTimeMillis();
        try (Connection connection = ds.getConnection();
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setQueryTimeout(timeout);
            this.bind(ps, bindValues);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columnCount = meta.getColumnCount();
                List<String> columns = new ArrayList<>(columnCount);
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(meta.getColumnLabel(i));
                }
                List<Map<String, Object>> rows = new ArrayList<>();
                boolean truncated = false;
                while (rs.next()) {
                    if (rows.size() >= rowLimit) {
                        truncated = true;
                        break;
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(columns.get(i - 1), this.convertValue(rs.getObject(i)));
                    }
                    rows.add(row);
                }
                result.setColumns(columns);
                result.setRows(rows);
                result.setRowCount((long) rows.size());
                result.setTruncated(truncated);
            }
        } catch (SQLException e) {
            throw new ApiException("SQL 执行失败", e);
        } finally {
            result.setDurationMs(System.currentTimeMillis() - start);
        }
        return result;
    }

    /**
     * 执行 SELECT COUNT(*) 包裹查询
     */
    private long runCount(javax.sql.DataSource ds, String sql, List<Object> bindValues, int timeout) {
        String countSql = "SELECT COUNT(*) AS cnt FROM (" + sql + ") _t";
        try (Connection connection = ds.getConnection();
             PreparedStatement ps = connection.prepareStatement(countSql)) {
            ps.setQueryTimeout(timeout);
            this.bind(ps, bindValues);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
                return 0L;
            }
        } catch (SQLException e) {
            throw new ApiException("SQL 执行失败", e);
        }
    }

    private void bind(PreparedStatement ps, List<Object> bindValues) throws SQLException {
        for (int i = 0; i < bindValues.size(); i++) {
            ps.setObject(i + 1, bindValues.get(i));
        }
    }

    private void verifyAuth(QueryTemplatePublish publish, String providedSecret, String timestamp, String nonce,
                            String signature, String requestBody) {
        AuthConfig auth = this.resolveAuth(publish.getSecret());
        if (AUTH_PUBLIC.equals(auth.type())) {
            return;
        }
        if (AUTH_API_KEY.equals(auth.type())) {
            String candidate = auth.hashed() ? this.sha256Hex(StrUtil.nullToEmpty(providedSecret)) : providedSecret;
            if (StrUtil.isBlank(providedSecret) || !this.constantTimeEquals(auth.secret(), candidate)) {
                throw new ApiException("API Key 校验失败");
            }
            return;
        }
        if (StrUtil.isBlank(timestamp) || StrUtil.isBlank(nonce) || StrUtil.isBlank(signature)) {
            throw new ApiException("HMAC 鉴权缺少 X-Timestamp、X-Nonce 或 X-Signature");
        }
        long requestTimestamp;
        try {
            requestTimestamp = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            throw new ApiException("X-Timestamp 必须是 Unix 秒级时间戳");
        }
        long now = System.currentTimeMillis() / 1000;
        if (Math.abs(now - requestTimestamp) > SIGNATURE_TOLERANCE_SECONDS) {
            throw new ApiException("HMAC 签名已过期，请校准调用方时间");
        }
        if (!nonce.matches("[a-zA-Z0-9_-]{8,64}")) {
            throw new ApiException("X-Nonce 必须为 8-64 位字母、数字、下划线或横线");
        }
        String bodyHash = this.sha256Hex(StrUtil.nullToEmpty(requestBody));
        String signingContent = timestamp + "\n" + nonce + "\n" + bodyHash;
        String expected = this.hmacSha256Hex(auth.secret(), signingContent);
        if (!this.constantTimeEquals(expected, signature.toLowerCase(Locale.ROOT))) {
            throw new ApiException("HMAC 签名校验失败");
        }
        String nonceKey = RedisKey.QUERY_TEMPLATE_NONCE.build(publish.getCode() + ":" + nonce);
        boolean accepted = this.redissonClient.getBucket(nonceKey).setIfAbsent("1", Duration.ofSeconds(SIGNATURE_TOLERANCE_SECONDS));
        if (!accepted) {
            throw new ApiException("HMAC 请求已被使用，请更换 X-Nonce");
        }
    }

    private AuthConfig resolveAuth(String storedSecret) {
        if (StrUtil.isBlank(storedSecret)) {
            return new AuthConfig(AUTH_PUBLIC, null, false);
        }
        if (storedSecret.startsWith(HMAC_PREFIX)) {
            return new AuthConfig(AUTH_HMAC_SHA256,
                    AesUtils.decryptGcm(storedSecret.substring(HMAC_PREFIX.length()), this.apiEncryptionKey), false);
        }
        if (storedSecret.startsWith(API_KEY_HASH_PREFIX)) {
            return new AuthConfig(AUTH_API_KEY, storedSecret.substring(API_KEY_HASH_PREFIX.length()), true);
        }
        if (storedSecret.startsWith(API_KEY_ENCRYPTED_PREFIX)) {
            return new AuthConfig(AUTH_API_KEY,
                    AesUtils.decryptGcm(storedSecret.substring(API_KEY_ENCRYPTED_PREFIX.length()), this.apiEncryptionKey), false);
        }
        // 兼容升级前直接存储的 X-Secret。
        return new AuthConfig(AUTH_API_KEY, storedSecret, false);
    }

    private String encodeAuth(String authType, String secret) {
        if (AUTH_PUBLIC.equals(authType)) {
            return null;
        }
        if (AUTH_API_KEY.equals(authType)) {
            return API_KEY_HASH_PREFIX + this.sha256Hex(secret);
        }
        return HMAC_PREFIX + AesUtils.encryptGcm(secret, this.apiEncryptionKey);
    }

    private String sha256Hex(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 计算失败", e);
        }
    }

    private String hmacSha256Hex(String secret, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC-SHA256 计算失败", e);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private void checkRateLimit(QueryTemplatePublish publish, String ip) {
        if (StrUtil.isBlank(publish.getEnableLimiting())
                || !Set.of(LIMIT_GLOBAL, LIMIT_IP).contains(publish.getEnableLimiting())) {
            return;
        }
        long rate = publish.getLimitRate() == null ? 10 : publish.getLimitRate();
        long interval = publish.getLimitRefreshInterval() == null ? 1 : publish.getLimitRefreshInterval();
        ChronoUnit unit = this.parseTimeUnit(publish.getLimitTimeUnit());
        String dimension = LIMIT_IP.equals(publish.getEnableLimiting()) ? ":ip:" + StrUtil.blankToDefault(ip, "unknown") : ":global";
        String key = RedisKey.QUERY_TEMPLATE_LIMIT.build(publish.getCode() + ":" + publish.getVersion() + dimension);
        RRateLimiter rateLimiter = this.redissonClient.getRateLimiter(key);
        if (!rateLimiter.isExists()) {
            rateLimiter.trySetRate(RateType.OVERALL, rate, Duration.of(interval, unit));
            rateLimiter.expire(Duration.of(10, ChronoUnit.DAYS));
        }
        if (!rateLimiter.tryAcquire()) {
            throw new ApiException("访问过于频繁，请稍后重试");
        }
    }

    private ChronoUnit parseTimeUnit(String unit) {
        if (StrUtil.isBlank(unit)) {
            return ChronoUnit.SECONDS;
        }
        return switch (unit.toUpperCase()) {
            case "MINUTES" -> ChronoUnit.MINUTES;
            case "HOURS" -> ChronoUnit.HOURS;
            default -> ChronoUnit.SECONDS;
        };
    }

    private String buildCacheKey(String code, String version, String method, Map<String, Object> params,
                                 int pageNum, int pageSize) {
        return code + "|" + version + "|" + method + "|" + JSON.toJSONString(params) + "|" + pageNum + "|" + pageSize;
    }

    private long extractNumber(QueryCallResponse response) {
        Object data = response.getData();
        if (data == null) {
            return 0L;
        }
        return switch (response.getMethod()) {
            case "count" -> ((Number) data).longValue();
            case "list" -> ((List<?>) data).size();
            case "page" -> {
                Object total = ((Map<?, ?>) data).get("total");
                yield total == null ? 0L : ((Number) total).longValue();
            }
            default -> 1L;
        };
    }

    private String truncate(String value) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        return value.length() > RESPONSE_ARG_LIMIT ? value.substring(0, RESPONSE_ARG_LIMIT) : value;
    }

    private Map<String, String> dataSourceNameMap(String workspaceCode, Set<String> dsCodes) {
        Map<String, String> dsNameMap = new HashMap<>();
        if (CollUtil.isEmpty(dsCodes)) {
            return dsNameMap;
        }
        this.dataSourceService.lambdaQuery()
                .select(DataSource::getCode, DataSource::getName)
                .eq(DataSource::getWorkspaceCode, workspaceCode)
                .in(DataSource::getCode, dsCodes)
                .list()
                .forEach(ds -> dsNameMap.put(ds.getCode(), ds.getName()));
        return dsNameMap;
    }

    private DataSource resolveDataSource(String dataSourceCode, String workspaceCode) {
        DataSource dataSource = this.dataSourceService.lambdaQuery()
                .eq(DataSource::getCode, dataSourceCode)
                .eq(DataSource::getWorkspaceCode, workspaceCode)
                .one();
        if (dataSource == null) {
            throw new ApiException("数据源不存在");
        }
        if (!Objects.equals(dataSource.getStatus(), Status.ENABLE.name())) {
            throw new ApiException("数据源非启用状态");
        }
        return dataSource;
    }

    private QueryTemplate resolveTemplate(Long id, String workspaceCode) {
        if (id == null || StrUtil.isBlank(workspaceCode)) {
            return null;
        }
        return this.lambdaQuery()
                .eq(QueryTemplate::getId, id)
                .eq(QueryTemplate::getWorkspaceCode, workspaceCode)
                .one();
    }

    private Object convertValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Timestamp || value instanceof java.sql.Date || value instanceof Time
                || value instanceof java.time.LocalDateTime || value instanceof java.time.LocalDate
                || value instanceof java.time.LocalTime) {
            return value.toString();
        }
        if (value instanceof byte[] bytes) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        if (value instanceof Blob blob) {
            try {
                return new String(blob.getBytes(1, (int) Math.min(blob.length(), 1024)), StandardCharsets.UTF_8);
            } catch (SQLException e) {
                return null;
            }
        }
        if (value instanceof Clob clob) {
            try {
                return clob.getSubString(1, (int) Math.min(clob.length(), 1024));
            } catch (SQLException e) {
                return null;
            }
        }
        return value;
    }

    private String rootMessage(Throwable throwable) {
        Throwable root = throwable;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getMessage();
    }

    /**
     * 解析后的 SQL 与绑定值
     */
    private record PreparedSql(String sql, List<Object> bindValues) {
    }

    private record AuthConfig(String type, String secret, boolean hashed) {
    }

}
