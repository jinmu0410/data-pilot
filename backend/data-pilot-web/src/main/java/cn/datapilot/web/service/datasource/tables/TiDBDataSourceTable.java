package cn.datapilot.web.service.datasource.tables;

import cn.datapilot.common.enums.DataSourceType;
import org.springframework.stereotype.Component;

/**
 * TiDB metadata is compatible with MySQL information_schema.
 */
@Component
public class TiDBDataSourceTable extends MySQLDataSourceTable {

    @Override
    public DataSourceType getDataSourceType() {
        return DataSourceType.TIDB;
    }
}
