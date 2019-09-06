package org.apache.skywalking.oap.server.plugin.kafka.module;

import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.library.module.ModuleConfig;

/**
 * 配置信息
 *
 * @author QIANGLU on 2019/9/6
 */
public class KafkaMoudleConfig extends ModuleConfig {

    @Getter
    @Setter
    private String server;

    @Getter
    @Setter
    private String topic;
}