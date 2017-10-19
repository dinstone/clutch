# Clutch
Clutch is a service registry and discovery component, that provides an infrastructure to publish and discover various resources, such as service proxies, HTTP endpoints, data sources.

## Introduction
A service provider can:
* publish a service record
* un-publish a published record
* update the status of a published service (down, out of service…​)

A service consumer can:
* lookup services
* bind to a selected service and use it
* release the service once the consumer is done with it
* listen for arrival, departure and modification of services.

## Quick Start
### add dependency:
```xml
<dependency>
	<groupId>com.dinstone.clutch</groupId>
	<artifactId>clutch-zookeeper</artifactId>
	<version>1.1.0</version>
</dependency>
```
### service registry
```java
    ZookeeperRegistryConfig config = new ZookeeperRegistryConfig().setZookeeperNodes("localhost:2181");
    ZookeeperServiceRegistry registry = new ZookeeperServiceRegistry(config);
    
    ServiceDescription description = new ServiceDescription();
    String serviceName = "TestService";
    description.setName(serviceName);
    description.setId("service-provider-" + System.currentTimeMillis());
    description.setHost("localhost");
    description.setPort(80);
    registry.register(description);
```
### service discovery
```java
    ZookeeperRegistryConfig config = new ZookeeperRegistryConfig().setZookeeperNodes("localhost:2181");
    ZookeeperServiceDiscovery discovery = new ZookeeperServiceDiscovery(config);
    
    ServiceDescription description = new ServiceDescription();
    String serviceName = "TestService";
    description.setName(serviceName);
    description.setId("service-consumer-1");
    description.setHost("localhost");
    description.setPort(0);

	discovery.listen(description);
	List<ServiceDescription> plist = discovery.discovery(serviceName, null);
    for (ServiceDescription psd : plist) {
    		System.out.println(psd);
    }
```


