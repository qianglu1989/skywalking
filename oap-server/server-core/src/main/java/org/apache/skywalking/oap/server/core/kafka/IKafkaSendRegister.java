package org.apache.skywalking.oap.server.core.kafka;


import com.google.gson.JsonObject;
import org.apache.skywalking.oap.server.library.module.Service;

public interface IKafkaSendRegister extends Service {

    /**
     * 获取服务注册信息
     * @param msg
     * @return
     */
    boolean serviceRegister(JsonObject msg);

    boolean sendMsg(String msg,String topic);

    public void offermsg(JsonObject msg);


}
