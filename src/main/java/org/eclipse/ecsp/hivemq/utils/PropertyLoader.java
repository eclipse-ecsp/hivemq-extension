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

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.domain.AbstractBlobEventData.Encoding;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.exceptions.TopicAlreadyConfiguredException;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.StringTokenizer;
import static org.eclipse.ecsp.hivemq.kafka.ApplicationConstants.REDIS_ENABLED;

/**
 * This class load properties from property file on application startup and put them in a map.
 */
public class PropertyLoader {
    private PropertyLoader() {
    }

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PropertyLoader.class);

    private static final String TENANT_CONFIG_FILE_PREFIX = "hivemq-plugin";
    private static final String CONFIG_FILE_EXTENSION = ".properties";
    private static final String BASE_FILE_NAME = "hivemq-plugin-base.properties";
    private static final Map<String, List<String>> PROPERTIES_MAP = new HashMap<>();
    private static final String FOLDER_PATH = AuthConstants.PROPERTY_FILE_PATH;
    private static final Properties PROPERTIES = new Properties();
    private static final Object LOCK = new Object();
    private static final Map<String, ServiceMap> SERVICEID_TO_SERVICE_MAP = new HashMap<>();
    private static final RedisPropLoader REDIS_PROP_LOADER = new RedisPropLoader();
    private static Map<String, String> mqttTopicToEventSourceMap;
    private static Map<String, Encoding> mqttTopicToEncodingMap;
    private static volatile boolean isInitialized = false;

    /**
     * reload properties from given filepath.
     *
     * @param filePath - path of property file
     * @return properties
     */
    public static Properties reload(File filePath) {
        synchronized (LOCK) {
            isInitialized = false;
            load(filePath);
        }
        return PROPERTIES;
    }

    /**
     * Loads the properties from the specified file.
     *
     * @param propFile the file containing the properties to be loaded
     */
    private static void load(File propFile) {
        LOGGER.info("Loading properties file: {}", propFile.getAbsoluteFile());
        try (InputStream is = new BufferedInputStream(new FileInputStream(propFile))) {
            LOGGER.info("Property file path:{} ", propFile);
            PROPERTIES.load(is);
            Enumeration<?> e = PROPERTIES.propertyNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                List<String> propValues = new ArrayList<>();
                if (key != null) {
                    String val = PROPERTIES.getProperty(key);
                    if (val == null) {
                        LOGGER.info("Key {} is not set in configuration file.", key);
                        continue;
                    }
                    StringTokenizer str = new StringTokenizer(val, AuthConstants.DELIMITER);
                    while (str.hasMoreElements()) {
                        String value = str.nextElement().toString();
                        if (!propValues.contains(value)) {
                            propValues.add(value);
                        }
                    }
                }
                PROPERTIES_MAP.put(key, propValues);
            }
            loadSeviceMap(PROPERTIES.getProperty(AuthConstants.CONF_SERVICEID_SERVICE_MAPPING));
            LOGGER.debug("List of Key and its Values Map {} ", PROPERTIES_MAP.toString());
            loadMqttTopicBlobSourceTypeMapper(PROPERTIES);
            isInitialized = true;
            LOGGER.info("hivemq-plugin.properties file successfully loaded..");
        } catch (FileNotFoundException e) {
            LOGGER.error("File not found exception from path: " + BASE_FILE_NAME, e);
        } catch (IOException e) {
            LOGGER.error("Could not load the property file from path: " + BASE_FILE_NAME, e);
        }
    }

    /**
     * Loads the necessary property files for the hivemq plugin.
     * This method loads the base hivemq plugin property file and checks if there are any tenant and environment 
     * specific hivemq plugin property files. If found, it loads those property files as well, adding new 
     * properties and overriding existing ones. Additionally, if the Redis feature is enabled, 
     * it loads the Redis properties.
     *
     * @see File
     * @see StringBuilder
     * @see LOGGER
     * @see PROPERTIES
     * @see REDIS_ENABLED
     * @see REDIS_PROP_LOADER
     * @see isInitialized
     * @see PROPERTIES_MAP
     */
    private static void load() {
        // Loading the base hivemq plugin property file.
        load(new File(FOLDER_PATH, BASE_FILE_NAME));
        StringBuilder tanentEnvConfFileName = new StringBuilder();
        tanentEnvConfFileName.append(FOLDER_PATH);
        tanentEnvConfFileName.append(TENANT_CONFIG_FILE_PREFIX);
        tanentEnvConfFileName.append(CONFIG_FILE_EXTENSION);
        LOGGER.info("Client specific hivemq-plugin file path {}", tanentEnvConfFileName);
        File tanentEnvConfFile = new File(tanentEnvConfFileName.toString());
        // Check any tanent and env specific hivemq plugin propertry file exist
        // or not. If exist then load the property file too which will add the
        // new properties and also override existing property.
        if (tanentEnvConfFile.exists()) {
            LOGGER.info("Client specific hivemq-plugin file is found. Path {}", tanentEnvConfFileName);
            load(tanentEnvConfFile);
        }
        if (Boolean.parseBoolean(PROPERTIES.getProperty(REDIS_ENABLED))) {
            REDIS_PROP_LOADER.load(FOLDER_PATH);
        }
        isInitialized = true;
        LOGGER.debug("Loaded properties: {}", PROPERTIES_MAP);
    }

    /**
     * This method loads service map from property.
     *
     * @param mapValues - service mapping
     */
    private static void loadSeviceMap(String mapValues) {
        if (mapValues == null) {
            LOGGER.info("serviceId to serviceName mapping is not found.");
            return;
        }
        String[] allServiceMapping = mapValues.split(AuthConstants.COLON);
        SERVICEID_TO_SERVICE_MAP.clear();
        for (String mappingLine : allServiceMapping) {
            ServiceMap serviceMap = new ServiceMap(mappingLine);
            SERVICEID_TO_SERVICE_MAP.put(serviceMap.getServiceId(), serviceMap);
        }
        LOGGER.info("ServiceId to ServiceMap {}", SERVICEID_TO_SERVICE_MAP);
    }

    /**
     * Map MqttTopic to BlobSourceType.
     *
     * @param prop initialized property which is loaded from property file.
     */
    private static void loadMqttTopicBlobSourceTypeMapper(Properties prop) {
        mqttTopicToEventSourceMap = new HashMap<>();
        mqttTopicToEncodingMap = new HashMap<>();
        String blobSources = prop.getProperty(ApplicationConstants.ALLOWED_BLOB_SOURCES);
        String blobEncodings = prop.getProperty(ApplicationConstants.ALLOWED_BLOB_ENCODINGS);
        StringTokenizer sources = new StringTokenizer(blobSources, ApplicationConstants.DELIMITER);
        StringTokenizer types;

        while (sources.hasMoreTokens()) {
            String curSource = sources.nextToken();
            types = new StringTokenizer(blobEncodings, ApplicationConstants.DELIMITER);
            while (types.hasMoreTokens()) {
                String curType = types.nextToken();
                StringBuilder builder = new StringBuilder();
                String propertyKey = builder.append(ApplicationConstants.PREFIX).append(ApplicationConstants.DOT)
                        .append(curSource).append(ApplicationConstants.DOT).append(curType).toString();
                String allTopicsValue = prop.getProperty(propertyKey);
                LOGGER.info("propertyKey: {} value: {}", propertyKey, allTopicsValue);

                prepareBlobSourceMap(allTopicsValue, curSource, curType, propertyKey);
            }
        }
        LOGGER.info("Loaded mqttTopic to EventSource Mapping: {} ", mqttTopicToEventSourceMap);
        LOGGER.info("Loaded mqttTopic to Encoding Mapping: {} ", mqttTopicToEncodingMap);
    }

    /**
     * Prepares the blob source map by mapping topics to their corresponding event sources and encoding types.
     * If the topic is already configured, a TopicAlreadyConfiguredException is thrown.
     *
     * @param allTopicsValue The string containing all the topics separated by a delimiter.
     * @param curSource The current event source.
     * @param curType The current encoding type.
     * @param propertyKey The key of the property.
     */
    private static void prepareBlobSourceMap(String allTopicsValue, String curSource, String curType,
            String propertyKey) {
        if (Objects.nonNull(allTopicsValue)) {
            StringTokenizer topics = new StringTokenizer(allTopicsValue, ApplicationConstants.DELIMITER);
            while (topics.hasMoreTokens()) {
                String curTopic = topics.nextToken();
                if (mqttTopicToEventSourceMap.containsKey(curTopic)) {
                    throw new TopicAlreadyConfiguredException("Topic is already configured. Topic: " + curTopic);
                }

                LOGGER.info("Topic {} and its source: {} and its type: {}", curTopic, curSource, curType);
                mqttTopicToEventSourceMap.put(curTopic, curSource);
                mqttTopicToEncodingMap.put(curTopic, Encoding.valueOf(curType.toUpperCase()));
            }
        } else {
            LOGGER.info("No configuration present for propertyKey: {} ", propertyKey);
        }
    }

    /**
     * This method loads properties from given property file.
     *
     * @param propFilePath - file path
     * @return properties
     */
    public static Properties getProperties(String propFilePath) {
        synchronized (LOCK) {
            load(new File(propFilePath));
        }
        return PROPERTIES;
    }

    /**
     * Method returns already loaded properties, if not loaded then it first loads them and then return.
     *
     * @return properties
     */
    public static Properties getProperties() {
        if (!isInitialized) {
            synchronized (LOCK) {
                if (!isInitialized) {
                    load();
                }
            }
        }
        return PROPERTIES;
    }

    /**
     * Method returns value of given property key.
     *
     * @param key - key for value is needed
     * @return String value of property
     */
    public static String getValue(String key) {
        if (!isInitialized) {
            getProperties();
        }
        return PROPERTIES.getProperty(key);
    }

    /**
     * Method returns value of given property key, and if key is not available then returns default value.
     *
     * @param key - key for value is needed
     * @param defaultValue - default value in case property is not available
     * @return String value of property
     */
    public static String getValue(String key, String defaultValue) {
        if (!isInitialized) {
            getProperties();
        }
        String value = PROPERTIES.getProperty(key);
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    /**
     * Method returns value of given property key.
     *
     * @param key - key for value is needed
     * @param optional - true if property is optional else false
     * @return String value of property
     */
    public static String getValue(String key, boolean optional) {
        if (!isInitialized) {
            getProperties();
        }
        String value = PROPERTIES.getProperty(key);
        if (!optional && StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException("Property " + key + " value is not defined.");
        }
        return value;
    }

    /**
     * This methods masks any input/serect with md5 algorithm.
     *
     * @param secret - value to be masked
     * @return masked value
     */
    public static String mask(String secret) {
        if (StringUtils.isNotEmpty(secret)) {
            return DigestUtils.md5Hex(secret);
        } else {
            return "";
        }
    }

    /**
     * Method returns Map of properties, where key will be String and values will be list of String.
     *
     * @return Map of properties
     */
    public static Map<String, List<String>> getPropertiesMap() {
        if (!isInitialized) {
            getProperties();
        }
        return PROPERTIES_MAP;
    }

    /**
     * Returns the folder path.
     *
     * @return the folder path
     */
    public static String getFolderPath() {
        return PropertyLoader.FOLDER_PATH;
    }

    /**
     * Method returns service mapping property.
     *
     * @return map of string and service
     */
    public static Map<String, ServiceMap> getSeviceMap() {
        if (!isInitialized) {
            getProperties();
        }
        return SERVICEID_TO_SERVICE_MAP;
    }

    /**
     * Method returns Map of topic to event source mapping.
     *
     * @return Map of topic to event source
     */
    public static Map<String, String> getMqttTopicToEventSourceMap() {
        if (!isInitialized) {
            getProperties();
        }
        return mqttTopicToEventSourceMap;
    }

    /**
     * Method returns mqtt topic to encoding mapping.
     *
     * @return map of topic to encoding
     */
    public static Map<String, Encoding> getMqttTopicToEncodingMap() {
        if (!isInitialized) {
            getProperties();
        }
        return mqttTopicToEncodingMap;
    }

    /**
     * Retrieves the Redis properties map.
     *
     * @return the Redis properties map
     */
    public static Map<String, String> getRedisPropertiesMap() {
        return REDIS_PROP_LOADER.getPropertiesMap();
    }

    // For supporting test cases
    /**
     * Loads the Redis properties from the specified folder path.
     *
     * @param folderPath the path of the folder containing the Redis properties
     */
    public static void loadRedisProperties(String folderPath) {
        REDIS_PROP_LOADER.load(folderPath);
    }

}
