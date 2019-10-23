package org.apache.skywalking.oap.server.plugin.kafka.base;

/**
 * @ClassName DataType
 * @Author QIANGLU
 * @Date 2019/10/23 5:12 下午
 * @Version 1.0
 */
public enum DataType {
    REGISTER("register"), PING("ping"), SEGMENT("segment"), ENDPOINT("endpoint");

    private String name;

    private DataType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
