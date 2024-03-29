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

/**
 * Service Registry
 *
 * @author dinstone
 *
 * @version 1.0.0
 */
public interface ServiceRegistry {

    /**
     * Register / Re-register a service instance
     *
     * @param service
     *            the service instance description
     *
     * @throws Exception
     *             errors
     */
    public void register(ServiceInstance service) throws Exception;

    /**
     * Deregister / Remove a service instance
     *
     * @param service
     *            the service instance description
     *
     * @throws Exception
     *             errors
     */
    public void deregister(ServiceInstance service) throws Exception;

    /**
     * resource release
     */
    public void destroy();

}
