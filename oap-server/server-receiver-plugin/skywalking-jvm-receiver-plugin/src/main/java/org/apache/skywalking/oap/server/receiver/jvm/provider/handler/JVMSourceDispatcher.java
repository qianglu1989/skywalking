/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.apache.skywalking.oap.server.receiver.jvm.provider.handler;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.skywalking.apm.network.common.CPU;
import org.apache.skywalking.apm.network.language.agent.GC;
import org.apache.skywalking.apm.network.language.agent.JVMMetric;
import org.apache.skywalking.apm.network.language.agent.Memory;
import org.apache.skywalking.apm.network.language.agent.MemoryPool;
import org.apache.skywalking.oap.server.core.Const;
import org.apache.skywalking.oap.server.core.CoreModule;
import org.apache.skywalking.oap.server.core.analysis.TimeBucket;
import org.apache.skywalking.oap.server.core.cache.ServiceInstanceInventoryCache;
import org.apache.skywalking.oap.server.core.kafka.IKafkaSendRegister;
import org.apache.skywalking.oap.server.core.register.ServiceInstanceInventory;
import org.apache.skywalking.oap.server.core.source.*;
import org.apache.skywalking.oap.server.library.module.ModuleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author wusheng
 */
public class JVMSourceDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(JVMSourceDispatcher.class);
    private final SourceReceiver sourceReceiver;
    private final ServiceInstanceInventoryCache instanceInventoryCache;
    private final IKafkaSendRegister iKafkaSendRegister;

    public JVMSourceDispatcher(ModuleManager moduleManager) {
        this.sourceReceiver = moduleManager.find(CoreModule.NAME).provider().getService(SourceReceiver.class);
        this.instanceInventoryCache = moduleManager.find(CoreModule.NAME).provider().getService(ServiceInstanceInventoryCache.class);
        this.iKafkaSendRegister = moduleManager.find("kafka").provider().getService(IKafkaSendRegister.class);

    }

    void sendMetric(int serviceInstanceId, long minuteTimeBucket, JVMMetric metrics) {
        ServiceInstanceInventory serviceInstanceInventory = instanceInventoryCache.get(serviceInstanceId);
        int serviceId;
        if (Objects.nonNull(serviceInstanceInventory)) {
            serviceId = serviceInstanceInventory.getServiceId();
        } else {
            logger.warn("Can't find service by service instance id from cache, service instance id is: {}", serviceInstanceId);
            return;
        }

        this.sendToCpuMetricProcess(serviceId, serviceInstanceId, minuteTimeBucket, metrics.getCpu());
        this.sendToMemoryMetricProcess(serviceId, serviceInstanceId, minuteTimeBucket, metrics.getMemoryList());
        this.sendToMemoryPoolMetricProcess(serviceId, serviceInstanceId, minuteTimeBucket, metrics.getMemoryPoolList());
        this.sendToGCMetricProcess(serviceId, serviceInstanceId, minuteTimeBucket, metrics.getGcList());
//        this.sendToJvmMetric(serviceInstanceInventory, minuteTimeBucket, metrics.getTime(), metrics.getCpu(),
//                metrics.getMemoryList(), metrics.getMemoryPoolList(), metrics.getGcList());
    }

    private void sendToJvmMetric(ServiceInstanceInventory serviceInstanceInventory, long minuteTimeBucket,
                                 long second, CPU cpu, List<Memory> memories, List<MemoryPool> memoryPools,
                                 List<GC> gcs) {
        try {


            long secondTimeBucket = TimeBucket.getSecondTimeBucket(second);
            Map<String, Object> jvmMetric = new HashMap<>();
            jvmMetric.put("name", serviceInstanceInventory.getName());
            jvmMetric.put("minuteTimeBucket", minuteTimeBucket);
            jvmMetric.put("secondTimeBucket", secondTimeBucket);

            //CPU
            Map<String, Object> cpuMetric = new HashMap<>();
            cpuMetric.put("usePercent", cpu.getUsagePercent());
            jvmMetric.put("cpu", cpuMetric);
            //Memory
            List<Map<String, Object>> memorys = new ArrayList<>();
            memories.forEach(memory -> {
                Map<String, Object> memoryMetric = new HashMap<>();
                memoryMetric.put("isHeap", memory.getIsHeap());
                memoryMetric.put("init", memory.getInit());
                memoryMetric.put("max", memory.getMax());
                memoryMetric.put("used", memory.getUsed());
                memoryMetric.put("committed", memory.getCommitted());
                memorys.add(memoryMetric);

            });
            jvmMetric.put("memory", memorys);

            //MemoryPool
            List<Map<String, Object>> memPools = new ArrayList<>();

            memoryPools.forEach(memoryPool -> {
                Map<String, Object> memoryPoolMetric = new HashMap<>();
                switch (memoryPool.getType()) {
                    case NEWGEN_USAGE:
                        memoryPoolMetric.put("memoryPoolType", MemoryPoolType.NEWGEN_USAGE);
                        break;
                    case OLDGEN_USAGE:
                        memoryPoolMetric.put("memoryPoolType", MemoryPoolType.OLDGEN_USAGE);
                        break;
                    case PERMGEN_USAGE:
                        memoryPoolMetric.put("memoryPoolType", MemoryPoolType.PERMGEN_USAGE);
                        break;
                    case SURVIVOR_USAGE:
                        memoryPoolMetric.put("memoryPoolType", MemoryPoolType.SURVIVOR_USAGE);
                        break;
                    case METASPACE_USAGE:
                        memoryPoolMetric.put("memoryPoolType", MemoryPoolType.METASPACE_USAGE);
                        break;
                    case CODE_CACHE_USAGE:
                        memoryPoolMetric.put("memoryPoolType", MemoryPoolType.CODE_CACHE_USAGE);
                        break;
                }
                memoryPoolMetric.put("init", memoryPool.getInit());
                memoryPoolMetric.put("max", memoryPool.getMax());
                memoryPoolMetric.put("used", memoryPool.getUsed());
                memoryPoolMetric.put("commited", memoryPool.getCommited());
                memPools.add(memoryPoolMetric);
            });
            jvmMetric.put("memoryPool", memPools);
            //GC
            List<Map<String, Object>> gcAll = new ArrayList<>();
            gcs.forEach(gc -> {
                int i = 1;
                Map<String, Object> gcPoolMetric = new HashMap<>();
                switch (gc.getPhrase()) {
                    case NEW:
                        gcPoolMetric.put("phrase", GCPhrase.NEW);
                        break;
                    case OLD:
                        gcPoolMetric.put("phrase", GCPhrase.OLD);
                        break;
                }
                gcPoolMetric.put("time", gc.getTime());
                gcPoolMetric.put("count", gc.getCount());
                gcAll.add(gcPoolMetric);


            });
            jvmMetric.put("gc", gcAll);
            JsonObject obj = new JsonObject();
            Gson gson = new Gson();
            String jvm = gson.toJson(jvmMetric);
            obj.addProperty("jvm", jvm);
            obj.addProperty("type", "jvm");
            iKafkaSendRegister.offermsg(obj);

        } catch (Exception e) {
            logger.error("处理jvm数据异常:{}", e.getMessage());
        }
    }

    private void sendToCpuMetricProcess(int serviceId, int serviceInstanceId, long timeBucket, CPU cpu) {
        ServiceInstanceJVMCPU serviceInstanceJVMCPU = new ServiceInstanceJVMCPU();
        serviceInstanceJVMCPU.setId(serviceInstanceId);
        serviceInstanceJVMCPU.setName(Const.EMPTY_STRING);
        serviceInstanceJVMCPU.setServiceId(serviceId);
        serviceInstanceJVMCPU.setServiceName(Const.EMPTY_STRING);
        serviceInstanceJVMCPU.setUsePercent(cpu.getUsagePercent());
        serviceInstanceJVMCPU.setTimeBucket(timeBucket);
        sourceReceiver.receive(serviceInstanceJVMCPU);
    }

    private void sendToGCMetricProcess(int serviceId, int serviceInstanceId, long timeBucket, List<GC> gcs) {
        gcs.forEach(gc -> {
            ServiceInstanceJVMGC serviceInstanceJVMGC = new ServiceInstanceJVMGC();
            serviceInstanceJVMGC.setId(serviceInstanceId);
            serviceInstanceJVMGC.setName(Const.EMPTY_STRING);
            serviceInstanceJVMGC.setServiceId(serviceId);
            serviceInstanceJVMGC.setServiceName(Const.EMPTY_STRING);

            switch (gc.getPhrase()) {
                case NEW:
                    serviceInstanceJVMGC.setPhrase(GCPhrase.NEW);
                    break;
                case OLD:
                    serviceInstanceJVMGC.setPhrase(GCPhrase.OLD);
                    break;
            }

            serviceInstanceJVMGC.setTime(gc.getTime());
            serviceInstanceJVMGC.setCount(gc.getCount());
            serviceInstanceJVMGC.setTimeBucket(timeBucket);
            sourceReceiver.receive(serviceInstanceJVMGC);
        });
    }

    private void sendToMemoryMetricProcess(int serviceId, int serviceInstanceId, long timeBucket,
                                           List<Memory> memories) {
        memories.forEach(memory -> {
            ServiceInstanceJVMMemory serviceInstanceJVMMemory = new ServiceInstanceJVMMemory();
            serviceInstanceJVMMemory.setId(serviceInstanceId);
            serviceInstanceJVMMemory.setName(Const.EMPTY_STRING);
            serviceInstanceJVMMemory.setServiceId(serviceId);
            serviceInstanceJVMMemory.setServiceName(Const.EMPTY_STRING);
            serviceInstanceJVMMemory.setHeapStatus(memory.getIsHeap());
            serviceInstanceJVMMemory.setInit(memory.getInit());
            serviceInstanceJVMMemory.setMax(memory.getMax());
            serviceInstanceJVMMemory.setUsed(memory.getUsed());
            serviceInstanceJVMMemory.setCommitted(memory.getCommitted());
            serviceInstanceJVMMemory.setTimeBucket(timeBucket);
            sourceReceiver.receive(serviceInstanceJVMMemory);
        });
    }

    private void sendToMemoryPoolMetricProcess(int serviceId, int serviceInstanceId, long timeBucket,
                                               List<MemoryPool> memoryPools) {

        memoryPools.forEach(memoryPool -> {
            ServiceInstanceJVMMemoryPool serviceInstanceJVMMemoryPool = new ServiceInstanceJVMMemoryPool();
            serviceInstanceJVMMemoryPool.setId(serviceInstanceId);
            serviceInstanceJVMMemoryPool.setName(Const.EMPTY_STRING);
            serviceInstanceJVMMemoryPool.setServiceId(serviceId);
            serviceInstanceJVMMemoryPool.setServiceName(Const.EMPTY_STRING);

            switch (memoryPool.getType()) {
                case NEWGEN_USAGE:
                    serviceInstanceJVMMemoryPool.setPoolType(MemoryPoolType.NEWGEN_USAGE);
                    break;
                case OLDGEN_USAGE:
                    serviceInstanceJVMMemoryPool.setPoolType(MemoryPoolType.OLDGEN_USAGE);
                    break;
                case PERMGEN_USAGE:
                    serviceInstanceJVMMemoryPool.setPoolType(MemoryPoolType.PERMGEN_USAGE);
                    break;
                case SURVIVOR_USAGE:
                    serviceInstanceJVMMemoryPool.setPoolType(MemoryPoolType.SURVIVOR_USAGE);
                    break;
                case METASPACE_USAGE:
                    serviceInstanceJVMMemoryPool.setPoolType(MemoryPoolType.METASPACE_USAGE);
                    break;
                case CODE_CACHE_USAGE:
                    serviceInstanceJVMMemoryPool.setPoolType(MemoryPoolType.CODE_CACHE_USAGE);
                    break;
            }

            serviceInstanceJVMMemoryPool.setInit(memoryPool.getInit());
            serviceInstanceJVMMemoryPool.setMax(memoryPool.getMax());
            serviceInstanceJVMMemoryPool.setUsed(memoryPool.getUsed());
            serviceInstanceJVMMemoryPool.setCommitted(memoryPool.getCommited());
            serviceInstanceJVMMemoryPool.setTimeBucket(timeBucket);
            sourceReceiver.receive(serviceInstanceJVMMemoryPool);
        });
    }
}
