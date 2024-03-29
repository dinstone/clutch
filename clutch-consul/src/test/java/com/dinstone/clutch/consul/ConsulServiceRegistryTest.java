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

import com.dinstone.clutch.ServiceInstance;

public class ConsulServiceRegistryTest {

    public static void main(String[] args) {
        ServiceInstance description = new ServiceInstance();
        String serviceName = "TestService";
        description.setServiceName(serviceName);
        description.setInstanceCode("service-provider-1");
        description.setHost("localhost");
        description.setPort(80);

        ConsulRegistryConfig config = new ConsulRegistryConfig().setAgentHost("127.0.0.1").setAgentPort(8500);
        ConsulServiceRegistry registry = new ConsulServiceRegistry(config);
        try {
            registry.register(description);

            description.setInstanceCode("service-provider-2");
            description.setHost("localhost");
            description.setPort(81);
            registry.register(description);

            System.in.read();

            registry.deregister(description);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            registry.destroy();
        }
    }

}
