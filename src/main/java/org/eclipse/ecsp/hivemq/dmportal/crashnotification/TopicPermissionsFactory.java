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

package org.eclipse.ecsp.hivemq.dmportal.crashnotification;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.lang.reflect.InvocationTargetException;

/**
 * The TopicPermissionsFactory class is responsible for creating and providing access to the singleton 
 * instance of the ClientTopicPermissions class. It loads the class dynamically based on the value 
 * specified in the configuration file.
 * This factory class creates and provide instance of configured dmportal topic formatter class.
 */
public class TopicPermissionsFactory {
    private TopicPermissionsFactory() {
    }
    
    private static ClientTopicPermissions topicPermissionInstance;
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(TopicPermissionsFactory.class);

    static {
        String className = StringUtils.EMPTY;
        className = PropertyLoader.getValue(ApplicationConstants.DMPORTAL_TOPIC_PERMISSION_CLASS);
        LOGGER.debug("Load TopicPermissionsFactory with classname {}", className);
        try {
            topicPermissionInstance = (ClientTopicPermissions) TopicPermissionsFactory.class.getClassLoader()
                    .loadClass(className).getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            LOGGER.error(className + "  is not available on the classpath", e);
            throw new IllegalArgumentException(className + "  is not available on the classpath");
        }
    }

    /**
     * Returns the singleton instance of the ClientTopicPermissions.
     *
     * @return the singleton instance of the ClientTopicPermissions
     */
    public static ClientTopicPermissions getInstance() {
        return topicPermissionInstance;
    }
}
