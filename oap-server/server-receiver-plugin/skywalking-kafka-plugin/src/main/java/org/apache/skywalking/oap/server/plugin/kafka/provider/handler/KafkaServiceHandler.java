package org.apache.skywalking.oap.server.plugin.kafka.provider.handler;

import com.google.gson.JsonObject;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.skywalking.oap.server.core.kafka.IKafkaSendRegister;
import org.apache.skywalking.oap.server.plugin.kafka.base.KafkaSend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * kafka服务发送
 *
 * @author QIANGLU on 2019/9/6
 */
public class KafkaServiceHandler implements IKafkaSendRegister {

    private static final Logger logger = LoggerFactory.getLogger(KafkaServiceHandler.class);

    private KafkaSend kafkaSend;

    public KafkaServiceHandler(KafkaSend kafkaSend) {
        this.kafkaSend = kafkaSend;
    }


    @Override
    public boolean serviceRegister(JsonObject msg) {
        try {
            send(msg.toString());
        } catch (Exception e) {
            logger.error("发送MQ信息出现异常:{}",e);
            return false;
        }
        return true;
    }

    private void send(String msg) {
        logger.info("发送基础信息:{}",msg);
        ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(kafkaSend.getTopic(), msg);
        kafkaSend.getProducer().send(producerRecord);
    }
}