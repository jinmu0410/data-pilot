package cn.datapilot.web.service.sync.engine;

import java.util.List;

/**
 * 同步引擎抽象：DataX / SeaTunnel 等底层引擎的统一入口
 *
 * @author jinmu
 */
public interface SyncEngine {

    /**
     * 引擎标识 DATAX/SEATUNNEL
     */
    String type();

    /**
     * 生成配置文件名
     */
    String configFileName();

    /**
     * 生成引擎原生配置内容
     */
    String buildConfig(SyncEngineContext ctx);

    /**
     * 生成执行命令
     *
     * @param configPath 配置文件绝对路径
     */
    List<String> buildCommand(String configPath);
}
