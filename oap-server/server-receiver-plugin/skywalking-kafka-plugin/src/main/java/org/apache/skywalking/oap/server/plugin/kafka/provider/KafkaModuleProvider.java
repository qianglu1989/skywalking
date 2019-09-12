package org.apache.skywalking.oap.server.plugin.kafka.provider;

import org.apache.skywalking.oap.server.core.kafka.IKafkaSendRegister;
import org.apache.skywalking.oap.server.library.module.*;

import org.apache.skywalking.oap.server.plugin.kafka.base.KafkaSend;
import org.apache.skywalking.oap.server.plugin.kafka.module.KafkaModule;
import org.apache.skywalking.oap.server.plugin.kafka.module.KafkaMoudleConfig;
import org.apache.skywalking.oap.server.plugin.kafka.provider.handler.KafkaServiceHandler;
import java.util.Properties;

/**
 * kafka服务初始化
 *
 * @author QIANGLU on 2019/9/6
 */
public class KafkaModuleProvider extends ModuleProvider {

    private KafkaMoudleConfig config;

    public KafkaModuleProvider(){
        this.config = new KafkaMoudleConfig();
    }
    @Override
    public String name() {
        return "default";
    }

    @Override
    public Class<? extends ModuleDefine> module() {
        return KafkaModule.class;
    }

    @Override
    public ModuleConfig createConfigBeanIfAbsent() {
        return config;
    }

    @Override
    public void prepare() throws ServiceNotProvidedException, ModuleStartException {

        Properties props = new Properties();
        props.put("bootstrap.servers", config.getServer());
        props.put("acks", "all");
        props.put("retries", 0);
        props.put("batch.size", 16384);
        props.put("linger.ms", 2000);
        props.put("buffer.memory", 33554432);
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        KafkaServiceHandler kafkaServiceHandler = new KafkaServiceHandler(KafkaSend.builder().topic(config.getTopic()).properties(props).build(),config.getQueueSize());
        this.registerServiceImplementation(IKafkaSendRegister.class,kafkaServiceHandler);
    }

    @Override
    public void start() throws ServiceNotProvidedException, ModuleStartException {

    }

    @Override
    public void notifyAfterCompleted() throws ServiceNotProvidedException, ModuleStartException {

    }

    @Override
    public String[] requiredModules() {
        return new String[0];
    }
}