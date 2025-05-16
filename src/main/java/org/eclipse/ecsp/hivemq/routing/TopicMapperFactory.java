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

package org.eclipse.ecsp.hivemq.routing;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.base.TopicMapper;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.lang.reflect.InvocationTargetException;

/**
 * The `TopicMapperFactory` class is responsible for creating and providing a singleton instance of the `TopicMapper`.
 * It loads the implementation class specified in the configuration and instantiates it using reflection.
 */
public class TopicMapperFactory {
    private TopicMapperFactory() {
    }

    private static final TopicMapper INSTANCE;
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(TopicMapperFactory.class);

    static {
        String className = StringUtils.EMPTY;
        try {
            className = PropertyLoader.getValue(ApplicationConstants.HIVEMQ_PLUGIN_TOPIC_MAPPER_IMPL_CLASS);
            LOGGER.info("Loading Topic Mapper class {}", className);
            INSTANCE = (TopicMapper) TopicMapperFactory.class.getClassLoader().loadClass(className)
                    .getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException(
                    ApplicationConstants.HIVEMQ_PLUGIN_TOPIC_MAPPER_IMPL_CLASS + " refers to a class [" + className
                            + "] that is not available on the classpath");
        }
    }

    /**
     * Returns the singleton instance of the TopicMapper.
     *
     * @return the singleton instance of the TopicMapper
     */
    public static TopicMapper getInstance() {
        return INSTANCE;
    }
}
