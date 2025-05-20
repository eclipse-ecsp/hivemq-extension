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

package org.eclipse.ecsp.hivemq.utils;

import lombok.Getter;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Currently hive mq uses its own implementation of Redission to communicate
 * with redis for its cluster discovery. Because of property conflicting this
 * new class is been used instead of already existing ProperytLoader class to
 * load the properties. Once HiveMq also uses ignite cache for cluster discovery
 * then we can delete this class.
 *
 * @author Vkoul
 */
public class RedisPropLoader {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(RedisPropLoader.class);
    private String filePath = "cache-enabler.properties";
    @Getter
    private Map<String, String> propertiesMap = new HashMap<>();

    public void load(String folderPath) {
        load(new File(folderPath, filePath));
    }

    /**
     * This method loads all redis related properties.
     *
     * @param propFile - property file
     */
    public void load(File propFile) {
        Properties properties = new Properties();
        LOGGER.info("Loading properties file: {}", propFile.getAbsoluteFile());
        try (InputStream is = new BufferedInputStream(new FileInputStream(propFile))) {
            LOGGER.info("Property file path: " + propFile);
            properties.load(is);
            Enumeration<?> e = properties.propertyNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                String val = properties.getProperty(key);
                propertiesMap.put(key, val);
            }
            LOGGER.debug("List of Key and its Values Map {} ", propertiesMap.toString());
            LOGGER.info("cache-enabler.properties file successfully loaded..");
        } catch (FileNotFoundException e) {
            LOGGER.error("File not found exception from path: " + filePath, e);
        } catch (IOException e) {
            LOGGER.error("Could not load the property file from path: " + filePath, e);
        }
    }

}