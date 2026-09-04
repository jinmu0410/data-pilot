package cn.datapilot.web.service.datasource.test;

import cn.datapilot.common.enums.DataSourceType;
import cn.datapilot.web.service.datasource.test.MySQLDataSourceTest;
import org.springframework.stereotype.Component;

/**
 * 〈一句话功能简述〉<br>
 * 〈〉
 *
 * @author jinmu
 * @date 2025/3/15
 * @since 1.0.0
 */
@Component
public class DorisDataSourceTest extends MySQLDataSourceTest {

    /**
     * 获取数据源类型
     *
     * @return 数据源类型
     */
    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.DORIS;
    }

}
