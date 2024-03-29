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

import java.util.Collection;

import com.dinstone.clutch.ServiceInstance;
import com.dinstone.loghub.Logger;
import com.dinstone.loghub.LoggerFactory;

public class ZookeeperServiceDiscoveryTest {

    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperServiceDiscoveryTest.class);

    public static void main(String[] args) {

        ZookeeperRegistryConfig config = new ZookeeperRegistryConfig().setZookeeperNodes("localhost:2181");

        ZookeeperServiceDiscovery discovery = new ZookeeperServiceDiscovery(config);
        ServiceInstance description = new ServiceInstance();
        String serviceName = "TestService";
        description.setServiceName(serviceName);
        description.setInstanceCode("service-consumer-1");
        description.setHost("localhost");
        description.setPort(0);

        try {
            discovery.listen(description);

            discovery.listen(description);
            //
            // description.setId("service-consumer-2");
            // discovery.listen(description);

            // description.setServiceName("TestService2");
            // description.setId("service-consumer-1");
            // discovery.listen(description);

            while (true) {
                Collection<ServiceInstance> plist = discovery.discovery(serviceName);
                if (plist != null && plist.size() > 0) {
                    for (ServiceInstance psd : plist) {
                        LOG.info(psd);
                    }
                    Thread.sleep(2000);
                } else {
                    LOG.info("empty");
                    Thread.sleep(1000);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            discovery.destroy();
        }

    }

}
