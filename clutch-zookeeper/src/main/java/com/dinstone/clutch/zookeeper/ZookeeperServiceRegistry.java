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
package com.dinstone.clutch.zookeeper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;

import com.dinstone.clutch.ServiceDescription;
import com.dinstone.clutch.ServiceRegistry;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class ZookeeperServiceRegistry implements ServiceRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperServiceRegistry.class);

    private final ServiceDescriptionSerializer serializer = new ServiceDescriptionSerializer();

    private final Map<String, ServiceDescription> services = new ConcurrentHashMap<>();

    private volatile ConnectionState connectionState = ConnectionState.LOST;

    private final String basePath;

    private final CuratorFramework client;

    private ConnectionStateListener connectionStateListener;

    public ZookeeperServiceRegistry(ZookeeperRegistryConfig registryConfig) {
        String zkNodes = registryConfig.getZookeeperNodes();
        if (zkNodes == null || zkNodes.length() == 0) {
            throw new IllegalArgumentException("zookeeper.node.list is empty");
        }

        String basePath = registryConfig.getConfigPath();
        if (basePath == null || basePath.length() == 0) {
            throw new IllegalArgumentException("basePath is empty");
        }
        this.basePath = basePath;

        // build CuratorFramework Object;
        this.client = CuratorFrameworkFactory.newClient(zkNodes,
                new ExponentialBackoffRetry(registryConfig.getBaseSleepTime(), registryConfig.getMaxRetries()));

        // add connection state change listener
        this.connectionStateListener = new ConnectionStateListener() {

            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                connectionState = newState;
                if ((newState == ConnectionState.RECONNECTED) || (newState == ConnectionState.CONNECTED)) {
                    try {
                        LOG.debug("Re-registering due to reconnection");
                        again();
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

    @Override
    public void register(ServiceDescription service) throws Exception {
        services.put(service.getCode(), service);
        if (connectionState == ConnectionState.CONNECTED) {
            internalRegister(service);
        }
    }

    @Override
    public void deregister(ServiceDescription service) throws Exception {
        String path = pathForProvider(service.getName(), service.getCode());
        try {
            client.delete().forPath(path);
        } catch (Exception ignore) {
            // ignore
        }
        services.remove(service.getCode());
    }

    public void destroy() {
        if (connectionState == ConnectionState.CONNECTED) {
            for (ServiceDescription service : services.values()) {
                try {
                    deregister(service);
                } catch (Exception e) {
                }
            }
        }
        services.clear();

        client.getConnectionStateListenable().removeListener(connectionStateListener);
        client.close();
    }

    protected void again() throws Exception {
        for (ServiceDescription service : services.values()) {
            internalRegister(service);
        }
    }

    protected void internalRegister(ServiceDescription service) throws Exception {
        service.setRtime(System.currentTimeMillis());
        byte[] bytes = serializer.serialize(service);
        String path = pathForProvider(service.getName(), service.getCode());

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
    }

    private String pathForProvider(String name, String id) {
        return ZKPaths.makePath(pathForService(name) + "/providers", id);
    }

    private String pathForService(String name) {
        return ZKPaths.makePath(basePath, name);
    }
}
