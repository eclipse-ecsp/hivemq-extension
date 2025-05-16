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
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.util.Properties;

/**
 * KeepAlive message processor.
 *
 * @author Binoy Mandal
 */
public class KeepAliveHandler {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(KeepAliveHandler.class);
    private TopicFormatter topicFormatter;

    /**
     * This method process keep alive messages.
     *
     * @param props - properties loaded by hivemq extension on application startup
     * @param clientId - client id
     * @param userName - username
     * @param mqttTopic - mqtt topic on which message was published
     * @return if its a keep alive massage then return true else false
     */
    public boolean doProcessKeepAliveMsg(final Properties props, String clientId, String userName, String mqttTopic) {
        if (isKeepAliveTopic(props, mqttTopic, clientId, userName)) {
            LOGGER.info("Keep alive message received topic: {}, clientId: {}", mqttTopic, clientId);
            return true;
        }
        return false;
    }

    /**
     * Checks if the given keep alive topic matches the configured keep alive topic.
     *
     * @param props The properties object containing the configuration.
     * @param incomingKeepAliveTopic The incoming keep alive topic to be checked.
     * @param clientId The client ID associated with the keep alive topic.
     * @param userName The username associated with the keep alive topic.
     * @return {@code true} if the given keep alive topic matches the configured keep alive topic, 
     *      {@code false} otherwise.
     */
    private boolean isKeepAliveTopic(final Properties props, final String incomingKeepAliveTopic, String clientId,
            String userName) {
        String keepAliveTopic = props.getProperty(AuthConstants.KEEP_ALIVE_TOPIC_NAME);
        if (StringUtils.isEmpty(keepAliveTopic)) {
            LOGGER.warn("Keep alive topic is not defined for clientId: {}", clientId);
            return false;
        }
        String formattedKeepaliveTopic = topicFormatter.formatPublishTopic(clientId, userName, keepAliveTopic);
        LOGGER.debug("Configured Keepalive topic: {} and incoming keepalive topic: {}  for clientId: {}",
                formattedKeepaliveTopic, incomingKeepAliveTopic, clientId);
        return formattedKeepaliveTopic.equals(incomingKeepAliveTopic);
    }

    /**
     * Sets the topic formatter for the KeepAliveHandler.
     *
     * @param topicFormatter the topic formatter to be set
     */
    public void setTopicFormatter(TopicFormatter topicFormatter) {
        this.topicFormatter = topicFormatter;
    }
}
