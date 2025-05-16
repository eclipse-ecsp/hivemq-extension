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

import org.eclipse.ecsp.analytics.stream.base.utils.ObjectUtils;
import org.eclipse.ecsp.hivemq.base.AbstractTopicMapper;
import org.eclipse.ecsp.hivemq.exceptions.IllegalTopicArgumentException;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.hivemq.utils.ServiceMap;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;

import java.util.Properties;

/**
 * This class provides different fragments of a formatted topics, like deviceId, stream status topic, service name etc.
 */
public class TopicMapperIgniteServiceBased extends AbstractTopicMapper {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(TopicMapperIgniteServiceBased.class);
    private String kafkaSinkTopicConnect;
    private String kafkaSinkTopicDisconnect;
    private static final String MQTT_DELIMITER = "/";
    private static final String MQTT_DELIMITER_PLUS_TO_CLOUD = MQTT_DELIMITER + TopicMapping.Route.TO_CLOUD.toString();
    private static final String MQTT_DELIMITER_PLUS_TO_DEVICE = MQTT_DELIMITER
            + TopicMapping.Route.TO_DEVICE.toString();

    private static final String SPACE = "";
    private static final int CLIENT_ID_IDX = 0;
    private static final int SERVICE_ID_IDX = 1;
    private static final String SHOULD_NOT_BE_EMPTY = " should not be empty.";
    private static final int TWO = 2;

    /**
     * Retrieves the topic mapping for the given MQTT topic.
     *
     * @param mqttTopic The MQTT topic to retrieve the mapping for.
     * @return The topic mapping object containing the device ID, service ID, stream topic, route, 
     *      device status requirement, and service name.
     * @throws IllegalTopicArgumentException If the MQTT topic is not in the proper format.
     * @throws IllegalArgumentException If the service mapping is not found for the given service ID.
     */
    @Override
    public TopicMapping getTopicMapping(String mqttTopic) throws IllegalTopicArgumentException {
        TopicMapping.Route route = TopicMapping.Route.TO_CLOUD;
        LOGGER.debug("Supplied mqtt topic {} and checking the route {}", mqttTopic,
                TopicMapping.Route.TO_DEVICE.toString());

        mqttTopic = mqttTopic.replace(mqttTopicPrefix, SPACE);
        if (mqttTopic.contains(MQTT_DELIMITER_PLUS_TO_DEVICE)) {
            route = TopicMapping.Route.TO_DEVICE;
        }
        mqttTopic = mqttTopic.replace(MQTT_DELIMITER_PLUS_TO_CLOUD, MQTT_DELIMITER);
        mqttTopic = mqttTopic.replace(MQTT_DELIMITER_PLUS_TO_DEVICE, MQTT_DELIMITER);

        LOGGER.debug("mqtt topic after removing topic prefix:{} and mqtt topic {}", mqttTopicPrefix, mqttTopic);
        String[] topicToken = mqttTopic.split(MQTT_DELIMITER, TWO);
        if (topicToken.length < TWO) {
            throw new IllegalTopicArgumentException("Mqtt topic is not proper. Mqtt topic: " + mqttTopic);
        }

        String serviceId = topicToken[SERVICE_ID_IDX].toLowerCase();
        ServiceMap serviceMap = PropertyLoader.getSeviceMap().get(serviceId);
        if (serviceMap == null) {
            throw new IllegalArgumentException("Service Mapping is not found for ServiceId: " + serviceId);
        }

        String streamTopic = serviceMap.getStreamTopic();
        LOGGER.debug("serviceId {} route to sink topic {}", serviceId, streamTopic);

        String deviceId = topicToken[CLIENT_ID_IDX];
        LOGGER.debug("device id {}", deviceId);

        return TopicMapping.builder().deviceId(deviceId).serviceId(serviceId).streamTopic(streamTopic).route(route)
                .deviceStatusRequired(serviceMap.isDeviceStatusRequired()).serviceName(serviceMap.getServiceName())
                .build();
    }

    /**
     * Initializes the TopicMapperIgniteServiceBased with the provided properties.
     *
     * @param prop the properties to initialize the service with
     */
    @Override
    public void init(Properties prop) {
        mqttTopicEventsSuffix = prop.getProperty(ApplicationConstants.MQTT_TOPIC_EVENTS_SUFFIX);
        ObjectUtils.requireNonEmpty(mqttTopicEventsSuffix,
                ApplicationConstants.MQTT_TOPIC_EVENTS_SUFFIX + SHOULD_NOT_BE_EMPTY);

        mqttTopicAlertsSuffix = prop.getProperty(ApplicationConstants.MQTT_TOPIC_ALERTS_SUFFIX);
        ObjectUtils.requireNonEmpty(mqttTopicAlertsSuffix,
                ApplicationConstants.MQTT_TOPIC_ALERTS_SUFFIX + SHOULD_NOT_BE_EMPTY);

        mqttTopicPrefix = prop.getProperty(ApplicationConstants.MQTT_TOPIC_PREFIX);

        kafkaSinkTopicConnect = prop.getProperty(ApplicationConstants.KAFKA_SINK_TOPIC_CONNECT);
        ObjectUtils.requireNonEmpty(kafkaSinkTopicConnect,
                ApplicationConstants.KAFKA_SINK_TOPIC_CONNECT + SHOULD_NOT_BE_EMPTY);

        kafkaSinkTopicDisconnect = prop.getProperty(ApplicationConstants.KAFKA_SINK_TOPIC_DISCONNECT);
        ObjectUtils.requireNonEmpty(kafkaSinkTopicDisconnect,
                ApplicationConstants.KAFKA_SINK_TOPIC_DISCONNECT + SHOULD_NOT_BE_EMPTY);
    }

    /**
        * Returns the Kafka sink topic for connecting clients.
        *
        * @return the Kafka sink topic for connecting clients
        */
    @Override
    public String getConnectTopic() {
        return kafkaSinkTopicConnect;
    }

    /**
     * Returns the topic used for disconnect events.
     *
     * @return The disconnect topic.
     */
    @Override
    public String getDisconnectTopic() {
        return kafkaSinkTopicDisconnect;
    }
}
