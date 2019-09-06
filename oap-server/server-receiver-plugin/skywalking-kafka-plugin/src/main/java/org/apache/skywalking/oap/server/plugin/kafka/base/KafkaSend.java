package org.apache.skywalking.oap.server.plugin.kafka.base;

import org.apache.kafka.clients.producer.KafkaProducer;

import java.time.Duration;
import java.util.Properties;

/**
 * @author QIANGLU
 */
public class KafkaSend {

    private Properties props;

    private String topic;

    private volatile KafkaProducer producer;

    public Properties getProps() {
        return props;
    }

    public void setProps(Properties props) {
        this.props = props;
    }

    public String getTopic() {
        return topic;
    }

    public void close(Duration duration) {
        this.producer.close(duration);
    }

    public void close() {
        this.producer.close(Duration.ofSeconds(10));
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public KafkaProducer getProducer() {

        if (this.producer == null) {
            synchronized (this) {
                if (this.producer == null) {
                    this.producer = new KafkaProducer<>(props);
                }
            }
        }
        return this.producer;
    }

    public static KafkaBuilder builder() {
        return new KafkaBuilder();
    }

    public static class KafkaBuilder {

        private KafkaSend send = new KafkaSend();

        public KafkaBuilder properties(Properties props) {
            this.send.setProps(props);
            return this;
        }

        public KafkaBuilder topic(String topic) {
            this.send.setTopic(topic);
            return this;
        }

        public KafkaSend build() {
            return send;
        }

    }
}
