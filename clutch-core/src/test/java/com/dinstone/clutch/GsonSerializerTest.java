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
package com.dinstone.clutch;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GsonSerializerTest {

    @Test
    public void testSerialize() throws Exception {
        ServiceInstanceSerializer s = new ServiceInstanceSerializer();

        ServiceInstance description = new ServiceInstance();
        String serviceName = "TestService";
        description.setServiceName(serviceName);
        description.setInstanceCode("service-provider-" + System.currentTimeMillis());
        description.setHost("localhost");
        description.setPort(80);

        byte[] sb = s.serialize(description);

        ServiceInstance si = s.deserialize(sb);

        assertEquals(serviceName, si.getServiceName());
        System.out.println(si);
    }

}
