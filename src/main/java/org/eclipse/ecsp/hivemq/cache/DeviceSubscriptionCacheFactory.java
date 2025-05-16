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

package org.eclipse.ecsp.hivemq.cache;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.lang.reflect.InvocationTargetException;

/**
 * This class prepares and return DeviceSubscriptionCache local cache instance.
 */
public class DeviceSubscriptionCacheFactory {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(DeviceSubscriptionCacheFactory.class);

    private static final Object LOCK = new Object();
    private static DeviceSubscriptionCache instance;
    
    private DeviceSubscriptionCacheFactory() {
    }

    /**
     * This method create instance of configured subscription local cachec class.
     *
     * @return DeviceSubscriptionCache
     */
    public static synchronized DeviceSubscriptionCache getInstance() {
        if (instance != null) {
            return instance;
        }
        synchronized (LOCK) {
            String className = StringUtils.EMPTY;
            try {
                className = PropertyLoader.getValue(ApplicationConstants.SUBSCRIPTION_CACHE_IMPL_CLASS);
                LOGGER.info("Loading subscription cache impl class {}", className);
                instance = (DeviceSubscriptionCache) DeviceSubscriptionCacheFactory.class.getClassLoader()
                        .loadClass(className).getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException
                    | InvocationTargetException | NoSuchMethodException | SecurityException e) {
                throw new IllegalArgumentException(ApplicationConstants.SUBSCRIPTION_CACHE_IMPL_CLASS
                        + " refers to a class [" + className + "] that is not available on the classpath");
            }
        }
        return instance;
    }
}
