package org.apache.skywalking.oap.server.plugin.kafka.provider.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.skywalking.apm.util.RunnableWithExceptionProtection;
import org.apache.skywalking.apm.util.StringUtil;
import org.apache.skywalking.oap.server.core.kafka.IKafkaSendRegister;
import org.apache.skywalking.oap.server.plugin.kafka.base.DefaultSecooThreadFactory;
import org.apache.skywalking.oap.server.plugin.kafka.base.KafkaSend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * kafka服务发送
 *
 * @author QIANGLU on 2019/9/6
 */
public class KafkaServiceHandler implements IKafkaSendRegister {

    private static final Logger logger = LoggerFactory.getLogger(KafkaServiceHandler.class);

    private KafkaSend kafkaSend;

    private MatrixSender matrixSender;
    private LinkedBlockingQueue<JsonObject> queue;
    private volatile ScheduledFuture<?> sendMetricFuture;

    public KafkaServiceHandler(KafkaSend kafkaSend, int queueSize) {
        this.kafkaSend = kafkaSend;
        queue = new LinkedBlockingQueue<>(queueSize);
        matrixSender = new MatrixSender();
        init();
    }

    private void init() {
        sendMetricFuture = Executors
                .newSingleThreadScheduledExecutor(new DefaultSecooThreadFactory("MQ-sender") {
                })
                .scheduleAtFixedRate(new RunnableWithExceptionProtection(matrixSender, new RunnableWithExceptionProtection.CallbackWhenException() {
                    @Override
                    public void handle(Throwable t) {
                        logger.error("MQ-sender  and upload failure.", t);
                    }
                }
                ), 0, 1, TimeUnit.SECONDS);
    }


    @Override
    public boolean serviceRegister(JsonObject msg) {
        try {
            send(msg.toString(), null);
        } catch (Exception e) {
            logger.error("exwarn: 发送注册信息:{}", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean sendMsg(String msg, String topic) {
        try {
            send(msg, topic);
        } catch (Exception e) {
            logger.error("exwarn:发送MQ信息出现异常:{}", e);
            return false;
        }
        return true;
    }


    @Override
    public void offermsg(JsonObject msg) {
        if (!queue.offer(msg)) {
            queue.poll();
            queue.offer(msg);
        }

    }

    public void send(Object msg, String topic) {
        try {

            ProducerRecord<String, Object> producerRecord = new ProducerRecord<>(StringUtil.isEmpty(topic) ? kafkaSend.getTopic() : topic, msg);
            kafkaSend.getProducer().send(producerRecord);
        } catch (Exception e) {
            kafkaSend.close();
        }

    }


    private class MatrixSender implements Runnable {

        @Override
        public void run() {
            if (kafkaSend != null && kafkaSend.getProducer() != null) {
                try {
                    LinkedList<JsonObject> buffer = new LinkedList<>();
                    queue.drainTo(buffer);
                    if (buffer.size() > 0) {
                        String data = dispose(buffer);
                        send(data, null);
                        logger.info("exwarn MatrixSender send buffer data:{}", buffer.size());
                    }
                } catch (Exception e) {
                    logger.error("send MQ metrics to Collector fail.{}", e);
                }
            }
        }

        private String dispose(LinkedList<JsonObject> buffer) {
            JsonArray array = new JsonArray();
            buffer.forEach(obj -> {
                array.add(obj);
            });

            return array.toString();
        }


    }
}