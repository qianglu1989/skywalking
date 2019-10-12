package org.apache.skywalking.oap.server.plugin.kafka.module;


import org.apache.skywalking.oap.server.core.kafka.IKafkaSendRegister;
import org.apache.skywalking.oap.server.library.module.ModuleDefine;

/**
 * 用于kafka数据对象传输
 *
 * @author QIANGLU on 2019/9/6
 */
public class KafkaModule extends ModuleDefine {

    public static final String NAME = "kafka";

    public KafkaModule() {
        super(NAME);
    }

    @Override
    public Class[] services() {
        return new Class[]{IKafkaSendRegister.class};
    }
}