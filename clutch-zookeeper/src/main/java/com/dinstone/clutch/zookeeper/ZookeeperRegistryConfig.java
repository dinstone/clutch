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

public class ZookeeperRegistryConfig implements RegistryConfig {

    private static final String DEFAULT_BASE_PATH = "/clutch/registry/";

    private String zookeeperNodes = "localhost:2181";
    private String configPath = DEFAULT_BASE_PATH;
    private int baseSleepTime = 1000;
    private int maxRetries = 3;

    public String getZookeeperNodes() {
        return zookeeperNodes;
    }

    public ZookeeperRegistryConfig setZookeeperNodes(String zookeeperNodes) {
        this.zookeeperNodes = zookeeperNodes;
        return this;
    }

    public String getConfigPath() {
        return configPath;
    }

    public ZookeeperRegistryConfig setConfigPath(String configPath) {
        this.configPath = configPath;
        return this;
    }

    public int getBaseSleepTime() {
        return baseSleepTime;
    }

    public ZookeeperRegistryConfig setBaseSleepTime(int baseSleepTime) {
        this.baseSleepTime = baseSleepTime;
        return this;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public ZookeeperRegistryConfig setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
        return this;
    }

    @Override
    public String getSchema() {
        return "zookeeper";
    }

}
