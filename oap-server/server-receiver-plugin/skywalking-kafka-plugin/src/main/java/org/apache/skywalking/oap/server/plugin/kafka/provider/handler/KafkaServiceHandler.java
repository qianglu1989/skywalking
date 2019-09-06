package org.apache.skywalking.oap.server.plugin.kafka.provider.handler;

import com.google.gson.JsonObject;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.skywalking.oap.server.core.kafka.IKafkaSendRegister;
import org.apache.skywalking.oap.server.plugin.kafka.base.KafkaSend;

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
    public boolean serviceRegister(JsonObject msg) {
        try {
            send(msg.toString());
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void send(String msg) {
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(kafkaSend.getTopic(), msg);
        kafkaSend.getProducer().send(producerRecord);
    }
}