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
import org.eclipse.ecsp.hivemq.base.AbstractTopicMapper;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;

import java.util.Properties;

/**
 * It has the HiveMQ topic to Kinesis topic mapping.
 */
public class TopicMapperCloudNative extends AbstractTopicMapper {

    private String kinesisSinkTopicEvents;
    private String kinesisSinkTopicAlerts;

    private String kinesisSinkTopicConnect;
    private String kinesisSinkTopicDisconnect;

    /**
     * Initializes the TopicMapperCloudNative with the provided properties.
     *
     * @param prop the properties containing the configuration values
     */
    @Override
    public void init(Properties prop) {
        mqttTopicEventsSuffix = prop.getProperty(ApplicationConstants.MQTT_TOPIC_EVENTS_SUFFIX);
        mqttTopicAlertsSuffix = prop.getProperty(ApplicationConstants.MQTT_TOPIC_ALERTS_SUFFIX);
        mqttTopicPrefix = prop.getProperty(ApplicationConstants.MQTT_TOPIC_PREFIX);

        kinesisSinkTopicEvents = prop.getProperty(ApplicationConstants.KINESIS_SINK_TOPIC_EVENTS);
        kinesisSinkTopicAlerts = prop.getProperty(ApplicationConstants.KINESIS_SINK_TOPIC_ALERTS);

        kinesisSinkTopicConnect = prop.getProperty(ApplicationConstants.KINESIS_SINK_TOPIC_CONNECT);
        kinesisSinkTopicDisconnect = prop.getProperty(ApplicationConstants.KINESIS_SINK_TOPIC_DISCONNECT);
    }

    /**
     * Retrieves the topic mapping for the given MQTT topic.
     *
     * @param mqttTopic The MQTT topic to retrieve the mapping for.
     * @return The topic mapping containing the device ID, service ID, and stream topic.
     */
    @Override
    public TopicMapping getTopicMapping(String mqttTopic) {
        String deviceId = StringUtils.EMPTY;
        String streamTopic = StringUtils.EMPTY;
        String serviceId = StringUtils.EMPTY;
        mqttTopic = mqttTopic.replace(mqttTopicPrefix, ApplicationConstants.BLANK);
        if (mqttTopic.endsWith(mqttTopicEventsSuffix)) {
            deviceId = mqttTopic.replace(mqttTopicEventsSuffix, ApplicationConstants.BLANK);
            streamTopic = kinesisSinkTopicEvents;
            serviceId = mqttTopicEventsSuffix.replace(ApplicationConstants.MQTT_DELIMITER,
                    ApplicationConstants.BLANK);
        } else if (mqttTopic.endsWith(mqttTopicAlertsSuffix)) {
            deviceId = mqttTopic.replace(mqttTopicAlertsSuffix, ApplicationConstants.BLANK);
            streamTopic = kinesisSinkTopicAlerts;
            serviceId = mqttTopicAlertsSuffix.replace(ApplicationConstants.MQTT_DELIMITER,
                    ApplicationConstants.BLANK);
        }

        return TopicMapping.builder().deviceId(deviceId).serviceId(serviceId).streamTopic(streamTopic).build();
    }

    /**
     * Returns the connect topic for the Kinesis sink.
     *
     * @return the connect topic for the Kinesis sink
     */
    @Override
    public String getConnectTopic() {
        return kinesisSinkTopicConnect;
    }

    /**
     * Returns the topic for disconnect events.
     *
     * @return the topic for disconnect events
     */
    @Override
    public String getDisconnectTopic() {
        return kinesisSinkTopicDisconnect;
    }

}
