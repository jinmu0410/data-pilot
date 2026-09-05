package cn.datapilot.flow;

import cn.datapilot.flow.config.ShardingProperties;
import cn.hutool.extra.spring.SpringUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 〈App〉
 *
 * @author jinmu
 * @since 1.0.0
 */
@EnableScheduling
@EnableConfigurationProperties(ShardingProperties.class)
@EnableAspectJAutoProxy(exposeProxy = true)
@MapperScan({"cn.datapilot.flow.store.mapper"})
@SpringBootApplication(
        scanBasePackages = {"cn.datapilot"},
        exclude = {
                ElasticsearchRestClientAutoConfiguration.class,
                FreeMarkerAutoConfiguration.class
        })
@Import(SpringUtil.class)
public class FlowApp {

    public static void main(String[] args) {
        SpringApplication.run(FlowApp.class, args);
        System.out.println("""
                 __       ___          __            ___  ___  __   __       \s
                |  \\  /\\   |   /\\  __ |__) |     /\\   |  |__  /  \\ |__)  |\\/|\s
                |__/ /~~\\  |  /~~\\    |    |___ /~~\\  |  |    \\__/ |  \\  |  |\s
                """);
    }

}