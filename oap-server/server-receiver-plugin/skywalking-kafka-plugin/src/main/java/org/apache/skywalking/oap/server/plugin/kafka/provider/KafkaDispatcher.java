package org.apache.skywalking.oap.server.plugin.kafka.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.skywalking.oap.server.core.analysis.SourceDispatcher;
import org.apache.skywalking.oap.server.core.source.Endpoint;
import org.apache.skywalking.oap.server.plugin.kafka.base.DataType;

/**
 * @ClassName KafkaDispatcher
 * @Author QIANGLU
 * @Date 2019/10/23 3:35 下午
 * @Version 1.0
 */
public class KafkaDispatcher implements SourceDispatcher<Endpoint> {
    @Override
    public void dispatch(Endpoint source) {
        Gson gson = new Gson();
        JsonElement element = gson.toJsonTree(source);
        element.getAsJsonObject().addProperty("dataType", DataType.ENDPOINT.getName());
        KafkaProcessor.getInstance().getKafkaSendRegister().offermsg(element);
    }
}
