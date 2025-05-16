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

package org.eclipse.ecsp.hivemq.dmportal.crashnotification;

import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.exceptions.ClientIdException;
import org.eclipse.ecsp.hivemq.exceptions.InvalidClientIdFormatException;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.hivemq.utils.UserManagementUtil;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.DELIMITER;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.MQTT_DELIMITER;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.PREFIX_DMPORTAL;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.USER_MQTT_SUBSCRIBE_TOPICS;
import static org.eclipse.ecsp.hivemq.utils.HivemqUtils.getPermission;

/**
 * This class provides default permission to dm portal user.
 */
public class DmPortalSubsTopicPermissions implements ClientTopicPermissions {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(DmPortalSubsTopicPermissions.class);
    private String topics;
    private UserManagementUtil userManagement;
    private static final int SPLIT_CLIENTID_SIZE = 2;

    public DmPortalSubsTopicPermissions() {
        init(PropertyLoader.getProperties());
        userManagement = new UserManagementUtil();
    }

    /**
     * provides permission for subscribing to topic for DMportal users.
     *
     * @param clientId - client id
     * @return list of topic permission for DMportal users.
     */
    @Override
    public List<TopicPermission> getTopicPermissions(String clientId) {
        List<TopicPermission> mqttTopicPermissions = new ArrayList<>();
        if (StringUtils.isEmpty(clientId)) {
            LOGGER.error("ClientId in the MQTT CONNECT message can not be empty.");
            throw new ClientIdException("ClientId in the MQTT CONNECT message can not be empty.");
        } else if (StringUtils.startsWithIgnoreCase(clientId, PREFIX_DMPORTAL)) {
            String userId = getUserId(clientId);
            LOGGER.debug("userId from clientId {} is {}", clientId, userId);
            String[] topicsArray = topics.split(DELIMITER);
            final String finalUserId = userId;
            if (topicsArray != null) {
                Arrays.stream(topicsArray).forEach(topic -> {
                    String topicName = getDmPortalFormattedUserTopicName(finalUserId, topic);
                    mqttTopicPermissions.add(getPermission(topicName, TopicPermission.MqttActivity.SUBSCRIBE));
                    LOGGER.debug("user {} got permission of subscribing to topic {}", userId, topicName);
                });
            }
        } else {
            LOGGER.debug("ClientId '{}' is not a DMPortal user so returning empty permissions list", clientId);
            return Collections.emptyList();
        }
        return mqttTopicPermissions;
    }

    /**
     * Retrieves the user ID from the given client ID.
     *
     * @param clientId The client ID from which to extract the user ID.
     * @return The extracted user ID.
     * @throws InvalidClientIdFormatException If the client ID is not in the proper format.
     */
    private String getUserId(String clientId) {
        String userId = "";
        String[] splittedClientId = clientId.split("_", SPLIT_CLIENTID_SIZE);
        if (splittedClientId.length < SPLIT_CLIENTID_SIZE || splittedClientId[1].lastIndexOf("_") < 0) {
            LOGGER.error("clientId {} is not in proper format", clientId);
            throw new InvalidClientIdFormatException(MessageFormatter
                    .format("clientId '{}' is not in proper format. The valid format is dmportal_userId_randomnumber",
                            clientId)
                    .getMessage());
        }
        userId = splittedClientId[1].substring(0, splittedClientId[1].lastIndexOf("_"));
        return userId;
    }

    /**
     * Method to get formatted topic for dm portal.
     *
     * @param userId - user id
     * @param topic - topic
     * @return builds the formatted topic.
     */
    private String getDmPortalFormattedUserTopicName(String userId, String topic) {
        return userManagement.getUserCountry(userId) + MQTT_DELIMITER + topic;
    }

    /**
     * Initializes the DmPortalSubsTopicPermissions object with the provided properties.
     *
     * @param prop the properties object containing the necessary configuration
     */
    @Override
    public void init(Properties prop) {
        topics = prop.getProperty(USER_MQTT_SUBSCRIBE_TOPICS);

    }

}
