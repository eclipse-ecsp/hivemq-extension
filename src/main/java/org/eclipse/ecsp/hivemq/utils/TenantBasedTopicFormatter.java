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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.util.Arrays;
import java.util.List;

/**
 * The topic formatter used to format the client topics for publish and
 * subscribe.
 *
 * <p>usage: following config needed in values.yaml mqqtt_uname_prefix:
 * tenant/env/ mqtt_topic_prefix: haa/tenant/env/ mqtt_topic_formatter:
 * org.eclipse.ecsp.hivemq.utils.TenantBasedTopicFormatter
 *
 * @author saadhikari
 */
public class TenantBasedTopicFormatter extends IgniteTopicFormatter {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(TenantBasedTopicFormatter.class);

    private String mqttUnamePrefix;
    private List<String> mqttUserPrefixList;

    /**
     * Constructor to load required properties on application startup.
     */
    public TenantBasedTopicFormatter() {
        mqttUnamePrefix = PropertyLoader.getValue(AuthConstants.MQTT_UNAME_PREFIX, false);
        mqttUserPrefixList = Arrays.asList(HivemqUtils.getPropertyArrayValue(AuthConstants.MQTT_USER_PREFIX));
    }

    /*
     * Publish topic format: <mqttTopicPrefix>/<clientId>/<mqttTopicInfix2C>/<topic>
     *
     */
    @Override
    public String formatPublishTopic(String clientId, String userName, String topic) {
        return getFormattedTopicName(clientId, getTopicPrefix(userName), MQTT_TOPIC_INFIX2C, topic);
    }

    /*
     * Subscribe topic format:
     * <mqttTopicPrefix>/<userIdWithoutPrefix>/<mqttTopicInfix2D>/<topic>
     */
    @Override
    public String formatSubscribeTopic(String clientId, String userName, String topic) {
        String user = getUserWithoutPrefix(userName);
        return getFormattedTopicName(user, getTopicPrefix(userName), MQTT_TOPIC_INFIX2D, topic);
    }

    /**
     * if user id starts with tenant/env i.e mqttUnamePrefix - topic format
     * topicPrefix/clientId/mqttTopicInfix/topic
     *
     * <p>if user id starts with mqttUserPrefix (i.e set to devices/dongle) - topic
     * format clientId/mqttTopicInfix/topic.
     *
     * <p>This method based on username decides topicPrefix should be empty or
     * configured one.
     *
     * @param userName - username
     * @return topic prefix
     */
    private String getTopicPrefix(String userName) {
        String topicPrefix = mqttTopicPrefix;

        if (StringUtils.isNotEmpty(userName)) {
            for (String userPrefix : mqttUserPrefixList) {
                if (userName.startsWith(userPrefix)) {
                    topicPrefix = StringUtils.EMPTY;
                    break;
                }
            }
        }

        LOGGER.debug("formatter UserName: {}, topicPrefix: {}", userName, topicPrefix);
        return topicPrefix;
    }

    /**
     * Returns the user name without the prefix.
     *
     * @param mqttUserName the MQTT user name
     * @return the user name without the prefix
     */
    private String getUserWithoutPrefix(String mqttUserName) {
        String userName = null;
        boolean isPrefixMatched = false;

        if (StringUtils.isEmpty(mqttUserName)) {
            return mqttUserName;
        }

        if (StringUtils.isNotEmpty(mqttUserName)) {
            for (String userPrefix : mqttUserPrefixList) {
                if (mqttUserName.startsWith(userPrefix)) {
                    userName = mqttUserName.replace(userPrefix, ApplicationConstants.BLANK);
                    isPrefixMatched = true;
                    break;
                }
            }
        }

        if (!isPrefixMatched) {
            userName = mqttUserName.replace(mqttUnamePrefix, ApplicationConstants.BLANK);
        }

        LOGGER.debug("userName without uname prefix: {}", userName);
        return userName;
    }

}
