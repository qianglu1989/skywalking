package java.org.apache.skywalking.oap.server.plugin.kafka.provider.handler;

import com.google.gson.JsonObject;
import org.apache.skywalking.oap.server.core.kafka.IKafkaSendRegister;

import java.org.apache.skywalking.oap.server.plugin.kafka.base.KafkaSend;

/**
 * kafka服务发送
 *
 * @author QIANGLU on 2019/9/6
 */
public class KafkaServiceHandler implements IKafkaSendRegister {

    private KafkaSend kafkaSend;

    public KafkaServiceHandler(KafkaSend kafkaSend) {
        this.kafkaSend = kafkaSend;
    }

    @Override
    public boolean sendMessage(JsonObject msg) {
        System.out.println(msg);
        kafkaSend.getProducer().send(null);
        return false;
    }
}