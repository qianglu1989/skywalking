package java.org.apache.skywalking.oap.server.plugin.kafka.provider;

import org.apache.skywalking.oap.server.library.module.*;

import java.org.apache.skywalking.oap.server.plugin.kafka.base.KafkaSend;
import java.org.apache.skywalking.oap.server.plugin.kafka.module.KafkaModule;
import java.org.apache.skywalking.oap.server.plugin.kafka.provider.handler.KafkaServiceHandler;
import java.util.Properties;

/**
 * kafka服务初始化
 *
 * @author QIANGLU on 2019/9/6
 */
public class KafkaModuleProvider extends ModuleProvider {

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
        return null;
    }

    @Override
    public void prepare() throws ServiceNotProvidedException, ModuleStartException {
        //TODO init config

        Properties props = new Properties();
        props.put("bootstrap.servers", "localhost:9092");
        props.put("acks", "all");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        KafkaServiceHandler kafkaServiceHandler = new KafkaServiceHandler(KafkaSend.builder().properties(props).build());
        this.registerServiceImplementation(KafkaServiceHandler.class,kafkaServiceHandler);
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