package cn.datapilot.web.service.datasource.test;

import cn.datapilot.common.enums.DataSourceType;
import org.springframework.stereotype.Component;

/**
 * TiDB connection test through its MySQL-compatible JDBC endpoint.
 */
@Component
public class TiDBDataSourceTest extends MySQLDataSourceTest {

    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.TIDB;
    }
}
