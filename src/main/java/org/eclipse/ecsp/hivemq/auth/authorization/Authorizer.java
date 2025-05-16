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

package org.eclipse.ecsp.hivemq.auth.authorization;

import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission.MqttActivity;
import com.hivemq.extension.sdk.api.client.parameter.InitializerInput;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.analytics.stream.base.utils.ObjectUtils;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.base.AbstractAuthorization;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCache;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
import org.eclipse.ecsp.hivemq.dmportal.crashnotification.ClientTopicPermissions;
import org.eclipse.ecsp.hivemq.dmportal.crashnotification.TopicPermissionsFactory;
import org.eclipse.ecsp.hivemq.exceptions.EmptyOrNotAvailablePropertyException;
import org.eclipse.ecsp.hivemq.exceptions.RequiredCriteriaNotMatchException;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.hivemq.utils.TopicFormatter;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.DEVICE_MQTT_PUBLISH_TOPICS;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.DEVICE_MQTT_SUBSCRIBE_TOPICS;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.HASH;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.IS_ALLOWED_ALL_TOPIC;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.MQTT_DELIMITER;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.MQTT_TOPIC_PREFIX;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.MQTT_USER_PREFIX;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.ONE;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.PROFILE_CHECK_DISABLED_TOPICS;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.USER_MQTT_SUBSCRIBE_TOPICS;
import static org.eclipse.ecsp.hivemq.utils.HivemqUtils.getPermission;

/**
 * This class is implementation of AbstractAuthorization class and prepare default permission for
 * whitelisted client, portal clients and devices.
 */
@Component
public class Authorizer extends AbstractAuthorization {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(Authorizer.class);

    protected Map<String, List<String>> authorizeProperties = null;
    protected Map<String, List<String>> reloadableAuthorizeProperties = null;
    private static Map<String, List<TopicPermission>> permissionMap = new ConcurrentHashMap<>();
    private String mqttTopicPrefix;
    private List<String> mqttUserPrefixList;
    protected boolean includeAllowedTopics;
    private DeviceSubscriptionCache deviceSubscriptionCache;
    private boolean allowedAllTopic;
    private static final String DOT = ".";
    private ClientTopicPermissions topicPermissionsFactory;

    /**
     * This constructor loads all required properties on class loading time.
     *
     * @throws IOException - throws exception when not able to load property file
     */
    public Authorizer() throws EmptyOrNotAvailablePropertyException {
        try {
            reloadableAuthorizeProperties = PropertyLoader.getPropertiesMap();
            ObjectUtils.requireNonEmpty(reloadableAuthorizeProperties, "Property Map should not be empty.");
        } catch (RequiredCriteriaNotMatchException e) {
            LOGGER.error("Property file not able to reload", e);
        }
        authorizeProperties = PropertyLoader.getPropertiesMap();
        mqttUserPrefixList = Arrays.asList(HivemqUtils.getPropertyArrayValue(AuthConstants.MQTT_USER_PREFIX));
        if (mqttUserPrefixList.isEmpty()) {
            LOGGER.error("Empty MQTT_USER_PREFIX {} ", MQTT_USER_PREFIX);
            throw new EmptyOrNotAvailablePropertyException("Empty MQTT_USER_PREFIX");
        }
        mqttTopicPrefix = PropertyLoader.getValue(MQTT_TOPIC_PREFIX);
        LOGGER.info("mqttUserPrefix: {}, mqttTopicPrefix: {}", mqttUserPrefixList, mqttTopicPrefix);
        includeAllowedTopics = true;
        deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        allowedAllTopic = Boolean.parseBoolean(PropertyLoader.getValue(IS_ALLOWED_ALL_TOPIC));
        topicPermissionsFactory = TopicPermissionsFactory.getInstance();
    }

    /**
     * Authorizes the client based on the provided initializer input.
     * This method returns a list of topic permissions for the client.
     *
     * @param initializerInput The initializer input containing client information and connection attributes.
     * @return A list of topic permissions for the client.
     */
    @Override
    public List<TopicPermission> authorize(InitializerInput initializerInput) {
        List<TopicPermission> mqttTopicPermissions = new ArrayList<>();

        String clientId = initializerInput.getClientInformation().getClientId();
        final Optional<String> usernameOptional = initializerInput.getConnectionInformation()
                .getConnectionAttributeStore().getAsString(AuthConstants.USERNAME);
        String userName = usernameOptional.isPresent() ? usernameOptional.get() : StringUtils.EMPTY;
        if (userName.isEmpty()) {
            LOGGER.warn("Username is empty for clientId: {}", clientId);
            return mqttTopicPermissions;
        }
        String user = HivemqUtils.getUserWithoutPrefix(userName);
        String[] userTokens = userName.split(MQTT_DELIMITER);
        LOGGER.info("clientId: {}, mqttUserName: {}, mqttUserPrefix: {}  user: {}", clientId, userName,
                mqttUserPrefixList, user);
        if (HivemqUtils.isWhiteListedUser(user)) {
            mqttTopicPermissions = getWhiteUserPermission(userName, clientId);
        } else if (userTokens.length == ONE) {
            // for portal user
            mqttTopicPermissions = getUserPortalPermission(userName, clientId);
            mqttTopicPermissions.addAll(topicPermissionsFactory.getTopicPermissions(clientId));
        } else {
            // for device
            mqttTopicPermissions = getDevicePermission(userName, clientId, false);
        }

        LOGGER.info("clientId: {} username: {} MqttTopicPermission: {}", clientId, userName,
                mqttTopicPermissions.stream().map(TopicPermission::getTopicFilter).collect(Collectors.joining(", ")));

        return mqttTopicPermissions;
    }

    /**
     * Extra topic mapping.
     *
     * @return list of topic permission
     */
    protected List<TopicPermission> getCustomPermissions() {
        return Collections.emptyList();
    }

    /**
     * if associated vehicle and has VIN and deviceType provide access to configured
     * topics based on deviceType else - if no associated vehicle, take clientId as
     * vehicleId --if associated and no deviceType --if suspicious provide access to
     * device topics, but publish and subscribe to anything except accepted topics
     * should be ignored
     * 
     * <p>Every state change needs a device reconnect, where disconnect initiated by
     * publishing to disconnect topic. For swap/replacement scenario disconnect
     * message sent by commcheck n for factory feed by vehicle lifecycle.
     *
     * @param userName - username
     * @param clientId - clientId
     * @param reassign - if initial default permissions were incorrect, then pass this flag as true
     * @return - list of topic permissions for device
     */
    public List<TopicPermission> getDevicePermission(String userName, String clientId, boolean reassign) {
        LOGGER.debug("It is a device: {}, clientId: {}", userName, clientId);

        // if already cached, return
        List<TopicPermission> mqttTopicPermissions = permissionMap.get(clientId);
        if (!reassign && mqttTopicPermissions != null && !mqttTopicPermissions.isEmpty()) {
            LOGGER.info("Returning default permissions from cache for clientId: {}", clientId);
            return mqttTopicPermissions;
        }

        boolean isSuspiciousDevice = isSuspiciousEcu(clientId);
        DeviceSubscription ds = deviceSubscriptionCache.getSubscription(clientId);
        String deviceType = StringUtils.EMPTY;
        if (ds != null) {
            deviceType = ds.getDeviceType().orElse(StringUtils.EMPTY).toLowerCase();
            ds.setSuspicious(isSuspiciousDevice);
        }

        // These lists should not be updated.
        List<String> configuredMqttPublishTopics = null;
        List<String> configuredMqttSubscribeTopics = null;
        LOGGER.debug("ClientId: {} , deviceType: {}", clientId, deviceType);
        if (deviceType.isEmpty() || allowedAllTopic) {
            configuredMqttPublishTopics = reloadableAuthorizeProperties.get(DEVICE_MQTT_PUBLISH_TOPICS);
            configuredMqttSubscribeTopics = reloadableAuthorizeProperties.get(DEVICE_MQTT_SUBSCRIBE_TOPICS);
        } else {
            String publishTopicsKey = new StringBuilder().append(deviceType).append(DOT)
                    .append(DEVICE_MQTT_PUBLISH_TOPICS).toString();
            String subscribeTopicsKey = new StringBuilder().append(deviceType).append(DOT)
                    .append(DEVICE_MQTT_SUBSCRIBE_TOPICS).toString();
            LOGGER.debug("deviceMqttPublishTopics key : {} ,deviceMqttSubscribeTopics key : {}", publishTopicsKey,
                    subscribeTopicsKey);
            configuredMqttPublishTopics = reloadableAuthorizeProperties.get(publishTopicsKey);
            configuredMqttSubscribeTopics = reloadableAuthorizeProperties.get(subscribeTopicsKey);
        }

        mqttTopicPermissions = prepareDevicePermissionList(configuredMqttPublishTopics, configuredMqttSubscribeTopics,
                clientId, userName);
        addCustomTopicPermissions(mqttTopicPermissions);

        // add to cache
        permissionMap.put(clientId, mqttTopicPermissions);

        return mqttTopicPermissions;
    }

    /**
     * Prepares the list of device permissions based on the configured MQTT publish and subscribe topics,
     * client ID, and user name.
     *
     * @param configuredMqttPublishTopics   The list of configured MQTT publish topics.
     * @param configuredMqttSubscribeTopics The list of configured MQTT subscribe topics.
     * @param clientId                      The client ID.
     * @param userName                      The user name.
     * @return The list of topic permissions.
     */
    private List<TopicPermission> prepareDevicePermissionList(List<String> configuredMqttPublishTopics,
            List<String> configuredMqttSubscribeTopics, String clientId, String userName) {
        List<String> deviceMqttPublishTopics = new ArrayList<>();
        List<String> deviceMqttSubscribeTopics = new ArrayList<>();

        if (!Objects.isNull(configuredMqttPublishTopics) && !configuredMqttPublishTopics.isEmpty()) {
            deviceMqttPublishTopics.addAll(configuredMqttPublishTopics);
        }

        if (!Objects.isNull(configuredMqttSubscribeTopics) && !configuredMqttSubscribeTopics.isEmpty()) {
            deviceMqttSubscribeTopics.addAll(configuredMqttSubscribeTopics);
        }

        List<String> allowedTopics = null;
        if (includeAllowedTopics) {
            // A collection of topics which does not requires vehicle id
            allowedTopics = Optional.ofNullable(reloadableAuthorizeProperties.get(PROFILE_CHECK_DISABLED_TOPICS))
                    .orElse(new ArrayList<>());
            LOGGER.debug("clientId: {}, Allowed topics: {}", clientId, allowedTopics);
            if (!allowedTopics.isEmpty()) {
                deviceMqttPublishTopics.addAll(allowedTopics);
                deviceMqttSubscribeTopics.addAll(allowedTopics);
            }
        }

        if (deviceMqttPublishTopics.isEmpty()) {
            LOGGER.error("deviceMqttPublishTopics: is not valid for clientId:{}", clientId);
        }
        if (deviceMqttSubscribeTopics.isEmpty()) {
            LOGGER.error("deviceMqttSubscribeTopics is not valid for clientId:{}", clientId);
        }

        String keepAliveTopic = PropertyLoader.getValue(AuthConstants.KEEP_ALIVE_TOPIC_NAME);
        if (StringUtils.isNotEmpty(keepAliveTopic)) {
            deviceMqttPublishTopics.add(keepAliveTopic);
        }

        LOGGER.debug("mqttTopicPrefix: {} mqttUserPrefix: {} mqttUserName: {} clientId: {}", mqttTopicPrefix,
                mqttUserPrefixList, userName, clientId);
        List<TopicPermission> mqttTopicPermissions = null;
        mqttTopicPermissions = getSubscribeTopicPermissions(clientId, userName, deviceMqttSubscribeTopics);
        mqttTopicPermissions.addAll(getPublishTopicPermissions(clientId, userName, deviceMqttPublishTopics));

        return mqttTopicPermissions;
    }

    /**
     * Adds custom topic permissions to the given list of MQTT topic permissions.
     *
     * @param mqttTopicPermissions the list of MQTT topic permissions to add custom permissions to
     */
    private void addCustomTopicPermissions(List<TopicPermission> mqttTopicPermissions) {
        List<TopicPermission> extraTopics = getCustomPermissions();
        if (Objects.nonNull(extraTopics) && !extraTopics.isEmpty()) {
            mqttTopicPermissions.addAll(extraTopics);
        }
    }

    /**
     * Method will prepare default publish/subscribe permission list for whitelisted/service users.
     *
     * @param userName - username
     * @param clientId - client id
     * @return List of TopicPermission
     */
    public List<TopicPermission> getWhiteUserPermission(String userName, String clientId) {
        ArrayList<TopicPermission> mqttTopicPermissions = new ArrayList<>();
        StringBuilder topicName = new StringBuilder();
        topicName.append(mqttTopicPrefix).append(HASH);
        mqttTopicPermissions.add(getPermission(topicName.toString(), TopicPermission.MqttActivity.ALL));
        LOGGER.info("Whitelisted user: {}, clientId: {} , got all permission to topic {}", userName, clientId,
                topicName);
        return mqttTopicPermissions;
    }

    /**
     * Method prepares subscribe topic list for device client.
     *
     * @param clientId - client id
     * @param userName - username
     * @param mqttTopics - list of unformatted subscribe topics
     * @return List of subscribe TopicPermission
     */
    public List<TopicPermission> getSubscribeTopicPermissions(String clientId, String userName,
            List<String> mqttTopics) {
        List<TopicPermission> mqttTopicPermissions = new ArrayList<>();
        if (Objects.isNull(mqttTopics)) {
            return mqttTopicPermissions;
        }
        LOGGER.debug("clientId: {} , subscribe topicList: {}", clientId, mqttTopics);

        for (String topic : mqttTopics) {
            String topicName = topicFormatter.formatSubscribeTopic(clientId, userName, topic);
            mqttTopicPermissions.add(getPermission(topicName, MqttActivity.SUBSCRIBE));
        }

        addGlobalTopicPermissions(mqttTopicPermissions);

        StringBuilder topicPermissions = new StringBuilder();
        mqttTopicPermissions.forEach(mqttTopicPermission -> topicPermissions
                .append(mqttTopicPermission.getTopicFilter()).append(StringUtils.SPACE));
        LOGGER.debug("clientId: {} got subscribe permission to topics {}", clientId, topicPermissions);

        return mqttTopicPermissions;
    }

    /**
     * Adds global topic permissions to the provided list of MQTT topic permissions.
     * Global topic permissions are retrieved using HivemqUtils.getGlobalSubTopics() method,
     * and each topic is mapped to a TopicPermission object with the permission type set to SUBSCRIBE.
     * The resulting list of global topic permissions is then added to the provided list of MQTT topic permissions.
     *
     * @param mqttTopicPermissions the list of MQTT topic permissions to which the global topic 
     *      permissions will be added
     */
    private void addGlobalTopicPermissions(List<TopicPermission> mqttTopicPermissions) {
        // Add global_sub_topic
        List<TopicPermission> globalSubTopics = HivemqUtils.getGlobalSubTopics().stream()
                .map(topic -> HivemqUtils.getPermission(topic, MqttActivity.SUBSCRIBE)).toList();
        if (Objects.nonNull(globalSubTopics)) {
            mqttTopicPermissions.addAll(globalSubTopics);
        }
    }

    /**
     * Retrieves the list of topic permissions for a portal user.
     *
     * @param userName The username of the portal user.
     * @param clientId The client ID associated with the user.
     * @return The list of topic permissions for the user.
     */
    protected List<TopicPermission> getUserPortalPermission(String userName, String clientId) {
        List<TopicPermission> mqttTopicPermissions = new ArrayList<>();
        LOGGER.debug("It is a portal user : {}", userName);
        List<String> topicList = reloadableAuthorizeProperties.get(USER_MQTT_SUBSCRIBE_TOPICS);

        String user = HivemqUtils.getUserWithoutPrefix(userName);
        LOGGER.debug("clientId: {}, mqttUserName: {}, mqttTopicPrefix: {}, subscription topicList: {}", clientId,
                userName, mqttTopicPrefix, topicList);
        for (String topic : topicList) {
            String topicName = topicFormatter.formatUserTopic(user, topic);
            mqttTopicPermissions.add(getPermission(topicName, MqttActivity.SUBSCRIBE));
            LOGGER.debug("user {} got permission of subscribing to topic {}", user, topicName);
        }
        return mqttTopicPermissions;
    }

    /**
     * Method prepares publish topic list for device client.
     *
     * @param clientId - client id
     * @param userName - username
     * @param mqttTopics - list of unformatted subscribe topics
     * @return List of publish TopicPermission
     */
    public List<TopicPermission> getPublishTopicPermissions(String clientId, String userName, List<String> mqttTopics) {
        List<TopicPermission> mqttTopicPermissions = new ArrayList<>();
        if (Objects.isNull(mqttTopics)) {
            return mqttTopicPermissions;
        }
        LOGGER.debug("clientId: {} , publish topicList: {}", clientId, mqttTopics);

        for (String topic : mqttTopics) {
            String topicName = topicFormatter.formatPublishTopic(clientId, userName, topic);
            mqttTopicPermissions.add(getPermission(topicName, MqttActivity.PUBLISH));
        }

        StringBuilder topicPermissions = new StringBuilder();
        mqttTopicPermissions.forEach(mqttTopicPermission -> topicPermissions
                .append(mqttTopicPermission.getTopicFilter()).append(StringUtils.SPACE));
        LOGGER.debug("clientId: {} got publish permission to topics {}", clientId, topicPermissions);

        return mqttTopicPermissions;
    }

    /**
     * Sets the topic formatter instance for the authorizer.
     *
     * @param topicFormatterInstance the topic formatter instance to be set
     */
    @Override
    public void setTopicFormatter(TopicFormatter topicFormatterInstance) {
        topicFormatter = topicFormatterInstance;
    }

    /**
     * This method removes default permission a client from local cache.
     *
     * @param clientId - client id
     */
    public static void removeFromPermissionMap(String clientId) {
        LOGGER.debug("Remove permissions for clientId: {}", clientId);

        if (clientId == null || permissionMap.get(clientId) == null) {
            return;
        }

        try {
            permissionMap.remove(clientId);
        } catch (Exception e) {
            LOGGER.error("Error while removing cached permissionmap for clientId: " + clientId, e);
        }
    }

}
