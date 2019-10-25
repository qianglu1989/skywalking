package org.apache.skywalking.oap.server.plugin.kafka.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.skywalking.oap.server.core.analysis.SourceDispatcher;
import org.apache.skywalking.oap.server.core.source.Endpoint;
import org.apache.skywalking.oap.server.core.source.Segment;
import org.apache.skywalking.oap.server.plugin.kafka.base.DataType;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName KafkaDispatcher
 * @Author QIANGLU
 * @Date 2019/10/23 3:35 下午
 * @Version 1.0
 */
public class KafkaEndpointDispatcher implements SourceDispatcher<Endpoint> {

    private final ConcurrentHashMap<String, String> endpoint = new ConcurrentHashMap<>();

    private long lastCheckTime = 0;

    @Override
    public void dispatch(Endpoint source) {

        endpoint.put(source.getServiceName(), String.valueOf(source.getServiceId()));
        Gson gson = new Gson();
        JsonObject object = new JsonObject();

        long curr = System.currentTimeMillis();
        if (curr - lastCheckTime > 5000) {
            object.addProperty("dataType", DataType.ENDPOINT.getName());
            object.addProperty("services", gson.toJson(endpoint));
            KafkaProcessor.getInstance().getKafkaSendRegister().offermsg(object);
            lastCheckTime = curr;
        }

    }
}
