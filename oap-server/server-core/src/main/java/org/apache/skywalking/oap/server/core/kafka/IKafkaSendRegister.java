package org.apache.skywalking.oap.server.core.kafka;


import com.google.gson.JsonObject;
import org.apache.skywalking.oap.server.library.module.Service;

public interface IKafkaSendRegister extends Service {

    boolean sendMessage(JsonObject msg);
}
