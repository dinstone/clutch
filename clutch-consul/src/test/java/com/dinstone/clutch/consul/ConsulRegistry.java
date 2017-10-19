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

package com.dinstone.clutch.consul;

import java.util.Arrays;
import java.util.List;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.model.HealthService;

public class ConsulRegistry {

    public static void main(String[] args) {
        ConsulClient client = new ConsulClient("182.92.5.75", 8500);
        client.agentServiceDeregister("myapp_01");
        client.agentServiceDeregister("service-provider-1");
        client.agentServiceDeregister("service-provider-1508226803226");
        client.agentServiceDeregister("service-provider-1508227579635");

        client.agentCheckDeregister("myapp_01");
        client.agentCheckDeregister("service-provider-1");
        client.agentCheckDeregister("service-provider-1508226803226");
        client.agentCheckDeregister("service-provider-1508227579635");

        NewService newService = new NewService();
        newService.setId("myapp_01");
        newService.setName("myapp");
        newService.setTags(Arrays.asList("EU-West", "EU-East"));
        newService.setPort(8080);

        NewService.Check check = new NewService.Check();
        check.setTtl("30s");
        newService.setCheck(check);

        client.agentServiceRegister(newService);

        showServices(client, newService);

        while (true) {
            client.agentCheckPass("service:" + newService.getId());
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            showServices(client, newService);
        }

    }

    private static void showServices(ConsulClient client, NewService newService) {
        // 获取所有服务
        List<HealthService> response = client.getHealthServices(newService.getName(), true, null).getValue();
        for (HealthService service : response) {
            for (com.ecwid.consul.v1.health.model.Check c : service.getChecks()) {
                System.out.println(service.getService() + ": " + c);
            }
        }
    }
}
