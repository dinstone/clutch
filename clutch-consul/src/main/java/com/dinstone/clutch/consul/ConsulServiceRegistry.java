/*
 * Copyright (C) 2014~2020 dinstone<dinstone@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dinstone.clutch.consul;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.dinstone.clutch.ServiceDescription;
import com.dinstone.clutch.ServiceRegistry;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;

public class ConsulServiceRegistry implements ServiceRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ConsulServiceRegistry.class);

    private ConsulClient client;

    private ConsulRegistryConfig config;

    private Map<String, ScheduledFuture<?>> serviceMap = new ConcurrentHashMap<>();

    private ServiceDescriptionSerializer serializer = new ServiceDescriptionSerializer();

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public ConsulServiceRegistry(ConsulRegistryConfig config) {
        this.config = config;
        this.client = new ConsulClient(config.getAgentHost(), config.getAgentPort());
    }

    @Override
    public void register(final ServiceDescription service) throws Exception {
        synchronized (serviceMap) {
            if (!serviceMap.containsKey(service.getCode())) {
                try {
                    register0(service);
                } catch (Exception e) {
                    retry(service);
                }
            }
        }
    }

    private void retry(final ServiceDescription service) {
        executorService.schedule(new Runnable() {

            @Override
            public void run() {
                try {
                    LOG.info("retry register service {}", service);

                    register0(service);

                } catch (Exception e) {
                    // ignore
                    retry(service);
                }
            }
        }, config.getInterval(), TimeUnit.SECONDS);
    }

    private void register0(final ServiceDescription service) throws Exception {
        NewService newService = new NewService();
        newService.setId(service.getCode());
        newService.setName(service.getName());
        newService.setAddress(service.getHost());
        newService.setPort(service.getPort());

        NewService.Check check = new NewService.Check();
        check.setTtl(config.getInterval() + "s");
        newService.setCheck(check);

        byte[] serialize = serializer.serialize(service);
        newService.setTags(Arrays.asList(new String(serialize, "utf-8")));

        client.agentServiceRegister(newService);

        long interval = config.getInterval();
        ScheduledFuture<?> future = executorService.scheduleAtFixedRate(new Runnable() {

            @Override
            public void run() {
                try {
                    client.agentCheckPass("service:" + service.getCode());
                } catch (Exception e) {
                    // ignore
                }
            }
        }, interval, interval, TimeUnit.SECONDS);

        serviceMap.put(service.getCode(), future);
    }

    @Override
    public void deregister(ServiceDescription service) throws Exception {
        synchronized (serviceMap) {
            ScheduledFuture<?> future = serviceMap.remove(service.getCode());
            if (future != null) {
                future.cancel(true);
            }
            client.agentServiceDeregister(service.getCode());
        }
    }

    @Override
    public void destroy() {
        executorService.shutdownNow();
    }

}
