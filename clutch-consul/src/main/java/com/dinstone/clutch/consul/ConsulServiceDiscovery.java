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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.dinstone.clutch.ServiceDescription;
import com.dinstone.clutch.ServiceDiscovery;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;

public class ConsulServiceDiscovery implements ServiceDiscovery {

    public class ServiceCache {

        private ConsulRegistryConfig config;

        private ServiceDescription description;

        private ScheduledFuture<?> scheduledFuture;

        private AtomicInteger reference = new AtomicInteger();

        private ServiceDescriptionSerializer serializer = new ServiceDescriptionSerializer();

        private ConcurrentHashMap<String, ServiceDescription> providers = new ConcurrentHashMap<>();

        public ServiceCache(ServiceDescription description, ConsulRegistryConfig config) {
            this.config = config;
            this.description = description;
        }

        public ServiceCache build() {
            this.scheduledFuture = executorService.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    try {
                        freshProvidors();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }, config.getInterval(), config.getInterval(), TimeUnit.SECONDS);

            try {
                freshProvidors();
            } catch (Exception e) {
                // ignore
            }

            return this;
        }

        protected void freshProvidors() throws Exception {
            HealthServicesRequest hr = HealthServicesRequest.newBuilder().setPassing(true).build();
            List<HealthService> healthServices = client.getHealthServices(description.getName(), hr).getValue();
            for (HealthService healthService : healthServices) {
                List<String> tags = healthService.getService().getTags();
                ServiceDescription description = null;
                if (tags != null && tags.size() > 0) {
                    description = serializer.deserialize(tags.get(0).getBytes("utf-8"));
                }

                if (description != null) {
                    providers.put(description.getCode(), description);
                }
            }
        }

        public Collection<ServiceDescription> getProviders() {
            return providers.values();
        }

        public int increment() {
            return reference.incrementAndGet();
        }

        public int decrement() {
            return reference.decrementAndGet();
        }

        public void destroy() {
            if (scheduledFuture != null) {
                scheduledFuture.cancel(false);
            }
        }

    }

    private ConsulClient client;

    private ConsulRegistryConfig config;

    private Map<String, ServiceCache> serviceCacheMap = new ConcurrentHashMap<>();

    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    public ConsulServiceDiscovery(ConsulRegistryConfig config) {
        this.config = config;
        this.client = new ConsulClient(config.getAgentHost(), config.getAgentPort());
    }

    @Override
    public void destroy() {
        synchronized (serviceCacheMap) {
            for (ServiceCache serviceCache : serviceCacheMap.values()) {
                serviceCache.destroy();
            }
            serviceCacheMap.clear();
        }

        executorService.shutdownNow();
    }

    @Override
    public void cancel(ServiceDescription description) {
        synchronized (serviceCacheMap) {
            ServiceCache serviceCache = serviceCacheMap.get(description.getName());
            if (serviceCache != null && serviceCache.decrement() <= 0) {
                serviceCache.destroy();
                serviceCacheMap.remove(description.getName());
            }
        }
    }

    @Override
    public void listen(ServiceDescription description) throws Exception {
        synchronized (serviceCacheMap) {
            ServiceCache serviceCache = serviceCacheMap.get(description.getName());
            if (serviceCache == null) {
                serviceCache = new ServiceCache(description, config).build();
                serviceCacheMap.put(description.getName(), serviceCache);
            }
            serviceCache.increment();
        }
    }

    @Override
    public Collection<ServiceDescription> discovery(String name) throws Exception {
        ServiceCache serviceCache = serviceCacheMap.get(name);
        if (serviceCache != null) {
            return serviceCache.getProviders();
        }
        return null;
    }

}
