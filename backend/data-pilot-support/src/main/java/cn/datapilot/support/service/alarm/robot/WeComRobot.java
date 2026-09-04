package cn.datapilot.support.service.alarm.robot;


import cn.datapilot.common.exception.ApiException;
import cn.datapilot.common.exception.LimitException;
import cn.datapilot.support.service.alarm.robot.content.Content;
import cn.datapilot.support.service.alarm.robot.content.TextContent;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;


/**
 * <a href="https://developer.work.weixin.qq.com/document/path/91770">文档</a>
 *
 * @author jinmu
 * @date 2025/2/21
 * @since 1.0.0
 */
@Slf4j
@Component
public class WeComRobot implements Robot {

    @Value("${dp.alarm.we-com.url:https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=}")
    private String url;

    @Resource
    private RestTemplate restTemplate;

    /**
     * 发送消息到机器人
     *
     * @param content 内容
     */
    @Override
    public void send(String token, Content content) {
        Map<String, Object> map = new HashMap<>(10);
        map.put("msgtype", "markdown");
        Map<String, String> contentMap = new HashMap<>(2);
        String contentText = ((TextContent) content).getContent();
        contentMap.put("content", StrUtil.maxLength(contentText, 4000));
        map.put("markdown", contentMap);
        URI uri = URI.create(this.url.concat(token));
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8");
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(map, headers);
        ResponseEntity<LinkedHashMap<String, String>> responseEntity = this.restTemplate.exchange(uri,
                HttpMethod.POST, entity, new ParameterizedTypeReference<>() {
                });
        Map<String, String> entityBody = responseEntity.getBody();
        if (CollUtil.isEmpty(entityBody)) {
            throw new ApiException("发送机器人消息失败");
        }
        // 被限流
        if (Objects.equals(entityBody.get("errcode"), "45009")) {
            throw new LimitException();
        }
        if (!Objects.equals(entityBody.get("errcode"), "0")) {
            throw new ApiException("发送机器人消息失败: " + entityBody.get("errmsg"));
        }
    }

}
