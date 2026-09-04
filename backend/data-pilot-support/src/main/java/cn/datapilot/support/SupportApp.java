package cn.datapilot.support;

import cn.datapilot.support.config.PrometheusDiscoveryConfig;
import cn.hutool.extra.spring.SpringUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/1/17
 * @since 1.0.0
 */
@EnableConfigurationProperties({PrometheusDiscoveryConfig.class})
@EnableScheduling
@MapperScan({"cn.datapilot.support.store.mapper"})
@SpringBootApplication(scanBasePackages = {"cn.datapilot"},
        exclude = {
                ElasticsearchRestClientAutoConfiguration.class,
                FreeMarkerAutoConfiguration.class,
                MongoAutoConfiguration.class
        }
)
@Import({SpringUtil.class})
@EnableAspectJAutoProxy(exposeProxy = true)
public class SupportApp {

    public static void main(String[] args) {
        SpringApplication.run(SupportApp.class, args);
        System.out.println("""
                 __       ___          __            ___  ___  __   __       \s
                |  \\  /\\   |   /\\  __ |__) |     /\\   |  |__  /  \\ |__)  |\\/|\s
                |__/ /~~\\  |  /~~\\    |    |___ /~~\\  |  |    \\__/ |  \\  |  |\s
                """);
    }

}
