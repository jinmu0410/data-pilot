package cn.dataplatform.open.query;

import cn.hutool.extra.spring.SpringUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.freemarker.FreeMarkerAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
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
@EnableScheduling
@MapperScan({"cn.dataplatform.open.query.store.mapper"})
@SpringBootApplication(scanBasePackages = {"cn.dataplatform"},
        exclude = {
                ElasticsearchRestClientAutoConfiguration.class,
                FreeMarkerAutoConfiguration.class,
                MongoAutoConfiguration.class
        }
)
@Import({SpringUtil.class})
@EnableAspectJAutoProxy(exposeProxy = true)
public class QueryApp {

    public static void main(String[] args) {
        SpringApplication.run(QueryApp.class, args);
        System.out.println("""
                 __       ___          __            ___  ___  __   __       \s
                |  \\  /\\   |   /\\  __ |__) |     /\\   |  |__  /  \\ |__)  |\\/|\s
                |__/ /~~\\  |  /~~\\    |    |___ /~~\\  |  |    \\__/ |  \\  |  |\s
                """);
    }

}
