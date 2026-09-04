package cn.datapilot.web;

import cn.hutool.extra.spring.SpringUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启动类
 *
 * @author jinmu
 * @date 2025/1/2
 * @since 1.0.0
 */
@EnableAsync
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan({"cn.datapilot.web.store.mapper"})
@SpringBootApplication(scanBasePackages = {"cn.datapilot"},
        exclude = {
                ElasticsearchRestClientAutoConfiguration.class,
                FreeMarkerAutoConfiguration.class
        }
)
@Import({SpringUtil.class})
public class WebApp {

    public static void main(String[] args) {
        SpringApplication.run(WebApp.class, args);
        System.out.println("""
                 __       ___          __            ___  ___  __   __       \s
                |  \\  /\\   |   /\\  __ |__) |     /\\   |  |__  /  \\ |__)  |\\/|\s
                |__/ /~~\\  |  /~~\\    |    |___ /~~\\  |  |    \\__/ |  \\  |  |\s
                """);
    }

}
