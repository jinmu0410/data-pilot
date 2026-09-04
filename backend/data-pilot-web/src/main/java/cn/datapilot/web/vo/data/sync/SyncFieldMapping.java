package cn.datapilot.web.vo.data.sync;

import lombok.Data;

/**
 * 同步字段映射项 source -> target
 *
 * @author jinmu
 */
@Data
public class SyncFieldMapping {

    private String source;

    private String target;
}
