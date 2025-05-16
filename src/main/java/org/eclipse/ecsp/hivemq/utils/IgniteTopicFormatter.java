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

import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.MQTT_TOPIC_PREFIX;

/**
 * The topic formatter used to format the client topics for publish and
 * subscribe.
 *
 * @author saadhikari
 */
public class IgniteTopicFormatter implements TopicFormatter {

    protected String mqttTopicPrefix;

    public IgniteTopicFormatter() {
        mqttTopicPrefix = PropertyLoader.getValue(MQTT_TOPIC_PREFIX);
    }

    /*
     * Publish topic format: <mqttTopicPrefix>/<clientId>/<mqttTopicInfix2C>/<topic>
     */
    @Override
    public String formatPublishTopic(String clientId, String userName, String topic) {
        return getFormattedTopicName(clientId, mqttTopicPrefix, MQTT_TOPIC_INFIX2C, topic);
    }

    /*
     * Subscribe topic format:
     * <mqttTopicPrefix>/<userIdWithoutPrefix>/<mqttTopicInfix2D>/<topic>
     */
    @Override
    public String formatSubscribeTopic(String clientId, String userName, String topic) {
        String user = HivemqUtils.getUserWithoutPrefix(userName);
        return getFormattedTopicName(user, mqttTopicPrefix, MQTT_TOPIC_INFIX2D, topic);
    }

    /*
     * For user topic no need to add mqttTopicInfix, i.e mqttTopicInfix = null
     */
    @Override
    public String formatUserTopic(String userId, String topic) {
        return getFormattedTopicName(userId, mqttTopicPrefix, null, topic);
    }

    /**
     * For user, publish and subscribe topic.
     *
     * @param topicPrefix - topic prefix
     * @param userOrClientId - clientId, or in case of user topic user id
     * @param mqttTopicInfix - topic infix(2C/2D)
     * @param topic - topic name
     * @return formatted topic
     */
    public String getFormattedTopicName(String userOrClientId, String topicPrefix, String mqttTopicInfix,
            String topic) {
        StringBuilder formattedTopicName = new StringBuilder();
        formattedTopicName.append(topicPrefix).append(userOrClientId).append(AuthConstants.MQTT_DELIMITER);
        if (mqttTopicInfix != null) {
            formattedTopicName.append(mqttTopicInfix);
        }
        formattedTopicName.append(topic);
        return formattedTopicName.toString();
    }

}
