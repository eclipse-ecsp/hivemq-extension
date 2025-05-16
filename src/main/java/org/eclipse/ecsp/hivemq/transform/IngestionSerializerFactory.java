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

package org.eclipse.ecsp.hivemq.transform;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.serializer.IngestionSerializer;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.lang.reflect.InvocationTargetException;

/**
 * This class creates and provide instance of configured serializer class.
 */
public class IngestionSerializerFactory {
    private IngestionSerializerFactory() {
    }
    
    private static final IngestionSerializer INSTANCE;
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(IngestionSerializerFactory.class);

    static {
        String className = StringUtils.EMPTY;
        try {
            className = PropertyLoader.getValue(ApplicationConstants.IGNITE_EVENT_SERIALIZER_CLASS);
            LOGGER.debug("Load IngestionSerializerFactory with classname {}", className);
            INSTANCE = (IngestionSerializer) IngestionSerializerFactory.class.getClassLoader().loadClass(className)
                    .getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            LOGGER.error(className + "  is not available on the classpath");
            throw new IllegalArgumentException(className + "  is not available on the classpath");
        }
    }

    /**
     * Returns an instance of the IngestionSerializer.
     *
     * @return the instance of the IngestionSerializer
     */
    public static IngestionSerializer getInstance() {
        return INSTANCE;
    }
}