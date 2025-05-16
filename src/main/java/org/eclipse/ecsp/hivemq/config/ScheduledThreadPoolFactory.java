/********************************************************************************

 * Copyright (c) 2023-24 Harman International 

 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License");

 * you may not use this file except in compliance with the License.

 * You may obtain a copy of the License at

 *
 *  <p>http://www.apache.org/licenses/LICENSE-2.0

 *     
 * <p>Unless required by applicable law or agreed to in writing, software

 * distributed under the License is distributed on an "AS IS" BASIS,

 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.

 * See the License for the specific language governing permissions and

 * limitations under the License.

 *
 * <p>SPDX-License-Identifier: Apache-2.0

 ********************************************************************************/

package org.eclipse.ecsp.hivemq.config;

import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * This class creates a custom executor service thread pool, which is used to submit disconnect tasks asynchronously.
 */
@Configuration
//@ComponentScan(basePackages = {"org..hivemq"})
public class ScheduledThreadPoolFactory {
    
    /**
     * Creates and returns a scheduled executor service for disconnect tasks.
     *
     * @return the scheduled executor service for disconnect tasks
     */
    @Bean("scheduledDisconnectThreadPool")
    public ScheduledExecutorService fixedDisconnectThreadPool() {
        int disconnectThread = Integer.parseInt(PropertyLoader.getValue(ApplicationConstants.DISCONNECT_THREAD, "32"));
        return Executors.newScheduledThreadPool(disconnectThread);
    }
}
