package org.apache.skywalking.oap.server.plugin.kafka.base;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 默认线程
 *
 * @author QIANGLU on 2019/9/12
 */
public class DefaultSecooThreadFactory implements ThreadFactory {
    private static final AtomicInteger BOOT_SERVICE_SEQ = new AtomicInteger(0);
    private final AtomicInteger threadSeq = new AtomicInteger(0);
    private final String namePrefix;
    public DefaultSecooThreadFactory(String name) {
        namePrefix = "Matrix-" + BOOT_SERVICE_SEQ.incrementAndGet() + "-" + name + "-";
    }
    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(r,namePrefix + threadSeq.getAndIncrement());
        t.setDaemon(true);
        return t;
    }
}