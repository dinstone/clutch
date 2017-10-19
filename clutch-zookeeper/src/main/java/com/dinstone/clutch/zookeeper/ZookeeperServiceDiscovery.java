/*
 * Copyright (C) 2014~2017 dinstone<dinstone@163.com>
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

package com.dinstone.clutch.zookeeper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadFactory;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ThreadUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dinstone.clutch.ServiceDescription;

public class ZookeeperServiceDiscovery implements com.dinstone.clutch.ServiceDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperServiceDiscovery.class);

    private final ServiceDescriptionSerializer serializer = new ServiceDescriptionSerializer();

    private final ConcurrentHashMap<String, ServiceCache> serviceCacheMap = new ConcurrentHashMap<>();

    private final ConcurrentLinkedQueue<ServiceDescription> listenServices = new ConcurrentLinkedQueue<>();

    private volatile ConnectionState connectionState = ConnectionState.LOST;

    private final String basePath;

    private final CuratorFramework client;

    private ConnectionStateListener connectionStateListener;

    public ZookeeperServiceDiscovery(ZookeeperRegistryConfig discoveryConfig) {
        String zkNodes = discoveryConfig.getZookeeperNodes();
        if (zkNodes == null || zkNodes.length() == 0) {
            throw new IllegalArgumentException("zookeeper.node.list is empty");
        }

        String basePath = discoveryConfig.getBasePath();
        if (basePath == null || basePath.length() == 0) {
            throw new IllegalArgumentException("basePath is empty");
        }
        this.basePath = basePath;

        // build CuratorFramework Object;
        this.client = CuratorFrameworkFactory.newClient(zkNodes,
            new ExponentialBackoffRetry(discoveryConfig.getBaseSleepTime(), discoveryConfig.getMaxRetries()));

        // add connection state change listener
        this.connectionStateListener = new ConnectionStateListener() {

            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                connectionState = newState;
                if ((newState == ConnectionState.RECONNECTED) || (newState == ConnectionState.CONNECTED)) {
                    try {
                        watch();
                    } catch (Exception e) {
                        LOG.error("Could not re-register instances after reconnection", e);
                    }
                }
            }
        };
        this.client.getConnectionStateListenable().addListener(connectionStateListener);

        // start CuratorFramework service;
        this.client.start();
    }

    protected void watch() throws Exception {
        ServiceDescription serviceDescription = null;
        while ((serviceDescription = listenServices.peek()) != null) {
            internalListen(serviceDescription);

            listenServices.remove();
        }
    }

    @Override
    public void destroy() {
        for (ServiceCache serviceCache : serviceCacheMap.values()) {
            serviceCache.destroy();
        }
        serviceCacheMap.clear();

        client.close();
    }

    @Override
    public void cancel(ServiceDescription description) {
        ServiceCache serviceCache = serviceCacheMap.get(description.getName());
        if (serviceCache != null && serviceCache.removeConsumer(description) <= 0) {
            serviceCache.destroy();
            serviceCacheMap.remove(description.getName());
        }
    }

    @Override
    public void listen(ServiceDescription description) throws Exception {
        if (connectionState != ConnectionState.CONNECTED) {
            listenServices.add(description);
        } else {
            internalListen(description);
        }
    }

    protected void internalListen(ServiceDescription description) throws Exception {
        ServiceCache serviceCache = null;

        synchronized (serviceCacheMap) {
            serviceCache = serviceCacheMap.get(description.getName());
            if (serviceCache == null) {
                String providerPath = pathForProviders(description.getName());
                ThreadFactory threadFactory = ThreadUtils.newThreadFactory("ServiceDiscovery");
                serviceCache = new ServiceCache(client, providerPath, threadFactory).build();
                serviceCacheMap.put(description.getName(), serviceCache);
            }
        }

        serviceCache.addConsumer(description);
    }

    @Override
    public List<ServiceDescription> discovery(String name, String group) throws Exception {
        List<ServiceDescription> serviceProviders = new ArrayList<>();

        ServiceCache serviceCache = serviceCacheMap.get(name);
        if (serviceCache != null) {
            for (ServiceDescription serviceDescription : serviceCache.getProviders()) {
                if (group != null) {
                    if (group.equals(serviceDescription.getGroup())) {
                        serviceProviders.add(serviceDescription);
                    }
                } else {
                    if (group == serviceDescription.getGroup()) {
                        serviceProviders.add(serviceDescription);
                    }
                }
            }
        }

        return serviceProviders;
    }

    private String pathForConsumer(String name, String id) {
        return ZKPaths.makePath(pathForService(name) + "/consumers", id);
    }

    private String pathForProviders(String name) {
        return ZKPaths.makePath(pathForService(name) + "/providers", "");
    }

    private String pathForService(String name) {
        return ZKPaths.makePath(basePath, name);
    }

    private class ServiceCache implements PathChildrenCacheListener {

        private final ConcurrentHashMap<String, ServiceDescription> providers = new ConcurrentHashMap<String, ServiceDescription>();

        private final ConcurrentHashMap<String, ServiceDescription> consumers = new ConcurrentHashMap<String, ServiceDescription>();

        private PathChildrenCache pathCache;

        public ServiceCache(CuratorFramework client, String name, ThreadFactory threadFactory) {
            pathCache = new PathChildrenCache(client, name, true, threadFactory);
            pathCache.getListenable().addListener(this);
        }

        public Collection<ServiceDescription> getProviders() {
            return providers.values();
        }

        public ServiceCache build() throws Exception {
            pathCache.start(StartMode.BUILD_INITIAL_CACHE);
            // init cache data
            for (ChildData childData : pathCache.getCurrentData()) {
                addProvider(childData, true);
            }

            return this;
        }

        public int addConsumer(ServiceDescription service) throws Exception {
            synchronized (consumers) {
                if (consumers.containsKey(service.getId())) {
                    return consumers.size();
                }

                service.setRtime(System.currentTimeMillis());
                byte[] bytes = serializer.serialize(service);
                String path = pathForConsumer(service.getName(), service.getId());

                final int MAX_TRIES = 2;
                boolean isDone = false;
                for (int i = 0; !isDone && (i < MAX_TRIES); ++i) {
                    try {
                        client.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(path, bytes);
                        isDone = true;
                    } catch (KeeperException.NodeExistsException e) {
                        // must delete then re-create so that watchers fire
                        client.delete().forPath(path);
                    }
                }

                consumers.put(service.getId(), service);

                return consumers.size();
            }
        }

        public int removeConsumer(ServiceDescription service) {
            synchronized (consumers) {
                consumers.remove(service.getId());

                return consumers.size();
            }
        }

        private void addProvider(ChildData childData, boolean onlyIfAbsent) throws Exception {
            String instanceId = ZKPaths.getNodeFromPath(childData.getPath());
            ServiceDescription serviceInstance = serializer.deserialize(childData.getData());
            if (onlyIfAbsent) {
                providers.putIfAbsent(instanceId, serviceInstance);
            } else {
                providers.put(instanceId, serviceInstance);
            }
            pathCache.clearDataBytes(childData.getPath(), childData.getStat().getVersion());
        }

        @Override
        public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
            switch (event.getType()) {
                case CHILD_ADDED:
                case CHILD_UPDATED: {
                    addProvider(event.getData(), false);
                    break;
                }

                case CHILD_REMOVED: {
                    providers.remove(ZKPaths.getNodeFromPath(event.getData().getPath()));
                    break;
                }
                default:
                    break;
            }
        }

        public void destroy() {
            if (connectionState == ConnectionState.CONNECTED) {
                for (ServiceDescription consumer : consumers.values()) {
                    String path = pathForConsumer(consumer.getName(), consumer.getId());
                    try {
                        client.delete().forPath(path);
                    } catch (Exception e) {
                    }
                }
            }

            pathCache.getListenable().removeListener(this);
            try {
                pathCache.close();
            } catch (IOException e) {
            }
        }

    }

}
