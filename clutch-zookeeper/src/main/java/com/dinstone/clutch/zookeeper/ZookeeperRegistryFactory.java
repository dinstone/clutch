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

import com.dinstone.clutch.RegistryConfig;
import com.dinstone.clutch.RegistryFactory;
import com.dinstone.clutch.ServiceDiscovery;
import com.dinstone.clutch.ServiceRegistry;

public class ZookeeperRegistryFactory implements RegistryFactory {

    @Override
    public synchronized ServiceRegistry createServiceRegistry(RegistryConfig registryConfig) {
        return new ZookeeperServiceRegistry((ZookeeperRegistryConfig) registryConfig);
    }

    @Override
    public synchronized ServiceDiscovery createServiceDiscovery(RegistryConfig registryConfig) {
        return new ZookeeperServiceDiscovery((ZookeeperRegistryConfig) registryConfig);
    }

    @Override
    public boolean canApply(RegistryConfig registryConfig) {
        return registryConfig instanceof ZookeeperRegistryConfig;
    }

}
