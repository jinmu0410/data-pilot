package cn.datapilot.common.source;

import cn.datapilot.common.enums.DataSourceType;

/**
 * TiDB uses the MySQL wire protocol and JDBC driver.
 */
public class TiDBDataSource extends MySQLDataSource {

    @Override
    public DataSourceType type() {
        return DataSourceType.TIDB;
    }
}
