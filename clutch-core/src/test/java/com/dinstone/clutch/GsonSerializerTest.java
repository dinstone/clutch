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
