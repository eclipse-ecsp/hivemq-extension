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
import org.eclipse.ecsp.analytics.stream.base.utils.ObjectUtils;
import org.eclipse.ecsp.hivemq.base.AbstractTopicMapper;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;

import java.util.Properties;

/**
 * It has mapping the HiveMQ topic to Kafka topic. Whatever message are coming
 * to Hivemq topic will be forward to mapped Kafka topic. Here, CloudMobile
 * means Kafka stream, not cloud streams like AWS Kinesis.
 */
public class TopicMapperCloudMobile extends AbstractTopicMapper {

    private String kafkaSinkTopicEvents;
    private String kafkaSinkTopicAlerts;

    private String kafkaSinkTopcConnect;
    private String kafkaSinkTopicDisconnect;

    private static final String SHOULD_NOT_BE_EMPTY = " should not be empty.";

    /**
    * Initializes the TopicMapperCloudMobile with the provided properties.
    *
    * @param prop the properties containing the configuration values
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

        kafkaSinkTopicEvents = prop.getProperty(ApplicationConstants.KAFKA_SINK_TOPIC_EVENTS);
        ObjectUtils.requireNonEmpty(kafkaSinkTopicEvents,
                ApplicationConstants.KAFKA_SINK_TOPIC_EVENTS + SHOULD_NOT_BE_EMPTY);

        kafkaSinkTopicAlerts = prop.getProperty(ApplicationConstants.KAFKA_SINK_TOPIC_ALERTS);
        ObjectUtils.requireNonEmpty(kafkaSinkTopicAlerts,
                ApplicationConstants.KAFKA_SINK_TOPIC_ALERTS + SHOULD_NOT_BE_EMPTY);

        kafkaSinkTopcConnect = prop.getProperty(ApplicationConstants.KAFKA_SINK_TOPIC_CONNECT);
        ObjectUtils.requireNonEmpty(kafkaSinkTopcConnect,
                ApplicationConstants.KAFKA_SINK_TOPIC_CONNECT + SHOULD_NOT_BE_EMPTY);

        kafkaSinkTopicDisconnect = prop.getProperty(ApplicationConstants.KAFKA_SINK_TOPIC_DISCONNECT);
        ObjectUtils.requireNonEmpty(kafkaSinkTopicDisconnect,
                ApplicationConstants.KAFKA_SINK_TOPIC_DISCONNECT + SHOULD_NOT_BE_EMPTY);

    }

    /**
     * Retrieves the topic mapping for the given MQTT topic.
     *
     * @param mqttTopic The MQTT topic to retrieve the mapping for.
     * @return The topic mapping containing the device ID, service ID, and stream topic.
     */
    @Override
    public TopicMapping getTopicMapping(String mqttTopic) {

        TopicMapping topicMapping;
        String deviceId = StringUtils.EMPTY;
        String streamTopic = StringUtils.EMPTY;
        String serviceId = StringUtils.EMPTY;

        mqttTopic = mqttTopic.replace(mqttTopicPrefix, ApplicationConstants.BLANK);
        if (mqttTopic.endsWith(mqttTopicEventsSuffix)) {
            deviceId = mqttTopic.replace(mqttTopicEventsSuffix, ApplicationConstants.BLANK);
            streamTopic = kafkaSinkTopicEvents;
            serviceId = mqttTopicEventsSuffix.replace(ApplicationConstants.MQTT_DELIMITER,
                    ApplicationConstants.BLANK);
        } else if (mqttTopic.endsWith(mqttTopicAlertsSuffix)) {
            deviceId = mqttTopic.replace(mqttTopicAlertsSuffix, ApplicationConstants.BLANK);
            streamTopic = kafkaSinkTopicAlerts;
            serviceId = mqttTopicAlertsSuffix.replace(ApplicationConstants.MQTT_DELIMITER,
                    ApplicationConstants.BLANK);
        }

        topicMapping = new TopicMapping.TopicMappingBuilder().deviceId(deviceId).serviceId(serviceId)
                .streamTopic(streamTopic).build();
        return topicMapping;
    }

    /**
     * Returns the connect topic for the Kafka sink.
     *
     * @return the connect topic for the Kafka sink
     */
    @Override
    public String getConnectTopic() {
        return kafkaSinkTopcConnect;
    }

    /**
     * Returns the topic for disconnect messages.
     *
     * @return The topic for disconnect messages.
     */
    @Override
    public String getDisconnectTopic() {
        return kafkaSinkTopicDisconnect;
    }

}
