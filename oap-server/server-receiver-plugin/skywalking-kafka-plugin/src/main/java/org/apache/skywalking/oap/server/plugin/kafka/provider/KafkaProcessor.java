package org.apache.skywalking.oap.server.plugin.kafka.provider;

import lombok.Getter;
import lombok.Setter;
import org.apache.skywalking.oap.server.core.kafka.IKafkaSendRegister;

/**
 * @ClassName KafkaProcessor
 * @Author QIANGLU
 * @Date 2019/10/23 5:05 下午
 * @Version 1.0
 */
public class KafkaProcessor {

    private static final KafkaProcessor KAFKA_PROCESSOR = new KafkaProcessor();

    @Getter
    @Setter
    private IKafkaSendRegister kafkaSendRegister;

    public static KafkaProcessor getInstance() {
        return KAFKA_PROCESSOR;
    }


}
