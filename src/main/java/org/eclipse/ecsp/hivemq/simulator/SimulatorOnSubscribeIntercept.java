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

package org.eclipse.ecsp.hivemq.simulator;

import com.hivemq.extension.sdk.api.packets.subscribe.Subscription;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.analytics.stream.base.utils.ObjectUtils;
import org.eclipse.ecsp.domain.DeviceConnStatusV1_0.ConnectionStatus;
import org.eclipse.ecsp.hivemq.base.TopicMapper;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCache;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
import org.eclipse.ecsp.hivemq.callbacks.AbstractSubscribeInboundInterceptor;
import org.eclipse.ecsp.hivemq.callbacks.SubscriptionStatusHandler;
import org.eclipse.ecsp.hivemq.exceptions.IllegalTopicArgumentException;
import org.eclipse.ecsp.hivemq.exceptions.InvalidSubscriptionException;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.routing.TopicMapperFactory;
import org.eclipse.ecsp.hivemq.routing.TopicMapping;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class handles subscribe requests coming from clients.
 */
@Component
@ConditionalOnProperty(name = "ssdp.simulator", havingValue = "true")
public class SimulatorOnSubscribeIntercept extends AbstractSubscribeInboundInterceptor {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(SimulatorOnSubscribeIntercept.class);

    @Autowired
    private SubscriptionStatusHandler handler;

    public void setHandler(SubscriptionStatusHandler handler) {
        this.handler = handler;
    }

    private DeviceSubscriptionCache deviceSubscriptionCache;
    private HivemqSinkService hivemqSinkService;
    private List<String> globalSubTopics;
    private String externalPresenceTopic;
    private TopicMapper topicMapper;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private static final String VEHICLE_ID = " vehicleId: ";
    private static final String CLIENT_ID = "ClientId: ";
    private static final String IT_IS_A_PUBLISH_MQTT_TOPIC_SO_NO_NEED_TO_SEND_DEVICE_STATUS
            = "It is a publish mqtt topic {}, so no need to send device status.";
    private static final String DEVICE_STATUS_IS_NOT_REQUIRED_FOR_MQTT_TOPIC
            = "device status is not required for mqtt topic ";
    private static final String THE_TOPICS_FOR_WHICH_SUBSCRIPTION_WERE_REQUESTED_FROM_CLIENT_ID_ARE_INVALID
            = "The topics: {} for which subscription were requested from clientId: {} are invalid";

    /**
     * This constructor loads required properties and get required class instance on application startup.
     */
    public SimulatorOnSubscribeIntercept() {
        hivemqSinkService = HivemqSinkService.getInstance();
        deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        globalSubTopics = HivemqUtils.getGlobalSubTopics();
        externalPresenceTopic = PropertyLoader.getValue(ApplicationConstants.KAFKA_SINK_TOPIC_PRESENCE_MANAGER);
        topicMapper = TopicMapperFactory.getInstance();
        topicMapper.init(PropertyLoader.getProperties());
        ObjectUtils.requireNonNull(topicMapper, "Unable to initialize the topic mapper");
    }

    /**
     * Performs the subscription operation for a given client and subscription.
     * If the client is linked to a vehicle and is not suspicious, it prepares the local cache for SSDP.
     * If the cache is updated, it sends a subscribe event to Kafka.
     * Otherwise, it logs the subscription and handles it using the handler.
     *
     * @param clientId     the ID of the client
     * @param subscription the subscription to be performed
     */
    @Override
    protected void doSubscribe(String clientId, Subscription subscription) {
        DeviceSubscription deviceSubscription = deviceSubscriptionCache.getSubscription(clientId);
        Set<String> topicSet = new HashSet<>();
        topicSet.add(subscription.getTopicFilter());

        if (deviceSubscription != null && deviceSubscription.isSsdpVehicle()) {
            String vehicleId = deviceSubscription.getVehicleId();
            if (StringUtils.isEmpty(vehicleId)) {
                LOGGER.warn("A client which is not linked to any vehicle id has connected, use clientId: {} instead",
                        clientId);
                vehicleId = clientId;
            }

            if (globalSubTopics != null && !globalSubTopics.isEmpty()) {
                LOGGER.debug("Topic: {} are a global_subscribe_topic for clientId: {}", globalSubTopics, clientId);
                globalSubTopics.forEach(topicSet::remove);
            }
            StringBuilder topicsSubscriptionStatus = new StringBuilder();
            topicsSubscriptionStatus.append(CLIENT_ID).append(clientId).append(VEHICLE_ID).append(vehicleId);
            boolean isCacheUpdated = false;
            List<String> acceptableTopics = HivemqUtils.getAcceptableTopics();
            boolean isSuspicious = deviceSubscription.isSuspicious();
            boolean isRestrictedAccess = StringUtils.isEmpty(vehicleId) || isSuspicious;
            for (String mqttTopic : topicSet) {
                isCacheUpdated = prepareLocalCacheForSsdp(clientId, vehicleId, mqttTopic, acceptableTopics,
                        isRestrictedAccess, topicsSubscriptionStatus);
            }

            if (isCacheUpdated) {
                JSONObject subscribePayload = createSsdpSubscribeEvent(clientId);
                sendMessageToKafa(clientId, subscribePayload, externalPresenceTopic);
            }
        } else {
            LOGGER.info("Subscribe to topic: {}. Logging scope: E2E, DVI for clientId: {}", topicSet, clientId);
            handler.handle(clientId, topicSet, ConnectionStatus.ACTIVE);
        }
    }

    /**
     * Prepares the local cache for SSDP by adding topics to the cache based on the provided parameters.
     *
     * @param clientId                The client ID associated with the subscription.
     * @param vehicleId               The vehicle ID associated with the subscription.
     * @param mqttTopic               The MQTT topic of the subscription.
     * @param acceptableTopics        The list of acceptable topics for the subscription.
     * @param isRestrictedAccess      A flag indicating if the subscription has restricted access.
     * @param topicsSubscriptionStatus A StringBuilder to store the status of the topics' subscription.
     * @return                        A boolean indicating if the cache was updated successfully.
     * @throws InvalidSubscriptionException If the MQTT topic is not proper.
     */
    private boolean prepareLocalCacheForSsdp(String clientId, String vehicleId, String mqttTopic,
                                             List<String> acceptableTopics, boolean isRestrictedAccess,
                                             StringBuilder topicsSubscriptionStatus) {
        boolean isCacheUpdated = false;
        TopicMapping topicMapping;
        try {
            topicMapping = topicMapper.getTopicMapping(mqttTopic);
        } catch (IllegalTopicArgumentException ex) {
            LOGGER.error("Mqtt topic {} is not proper for {}", mqttTopic, clientId);
            throw new InvalidSubscriptionException("Mqtt topic is not proper. Mqtt topic: " + mqttTopic);
        }
        if (topicMapping == null) {
            LOGGER.error("Invalid mqttTopic {}, no service mapping found", mqttTopic);
            return isCacheUpdated;
        }
        boolean isPublishTopic = topicMapping.getRoute().equals(TopicMapping.Route.TO_CLOUD);
        if (isPublishTopic || !topicMapping.isDeviceStatusRequired()) {
            if (isPublishTopic) {
                LOGGER.debug(IT_IS_A_PUBLISH_MQTT_TOPIC_SO_NO_NEED_TO_SEND_DEVICE_STATUS, mqttTopic);
            }
            if (!topicMapping.isDeviceStatusRequired()) {
                topicsSubscriptionStatus.append(DEVICE_STATUS_IS_NOT_REQUIRED_FOR_MQTT_TOPIC).append(mqttTopic);
            }
            return isCacheUpdated;
        }

        if (isRestrictedAccess && !acceptableTopics.isEmpty()
                && !acceptableTopics.contains(topicMapping.getServiceId())) {
            LOGGER.warn(THE_TOPICS_FOR_WHICH_SUBSCRIPTION_WERE_REQUESTED_FROM_CLIENT_ID_ARE_INVALID, mqttTopic,
                    clientId);
            return isCacheUpdated;
        }
        // For ssdp simulator putting service name instead of full topic
        deviceSubscriptionCache.addTopic(clientId, vehicleId, topicMapping.getServiceName());
        return true;
    }

    /**
     * Create kafka event message for ssdp subscribe request.
     *
     * @param clientId - client id
     * @return json object of message
     */
    @SuppressWarnings("unchecked")
    public JSONObject createSsdpSubscribeEvent(String clientId) {
        JSONObject eventObj = new JSONObject();
        eventObj.put(SimulatorEventMetadataConstants.UIN, clientId);
        eventObj.put(SimulatorEventMetadataConstants.STATE, SimulatorEventMetadataConstants.ONLINE);

        String parsedDate = LocalDateTime.now().format(FORMATTER);
        eventObj.put(SimulatorEventMetadataConstants.UPDATED, parsedDate);
        DeviceSubscription deviceSubscription = deviceSubscriptionCache.getSubscription(clientId);
        JSONArray serviceArray = new JSONArray();
        JSONObject serviceObj = new JSONObject();
        for (String topic : deviceSubscription.getSubscriptionTopics()) {
            JSONObject subscriptionStatusObj = new JSONObject();
            subscriptionStatusObj.put(SimulatorEventMetadataConstants.STATE, SimulatorEventMetadataConstants.ONLINE);
            subscriptionStatusObj.put(SimulatorEventMetadataConstants.UPDATED, parsedDate);
            serviceObj.put(topic, subscriptionStatusObj);
        }
        serviceArray.add(serviceObj);
        eventObj.put(SimulatorEventMetadataConstants.SERVICES, serviceArray);
        return eventObj;
    }

    /**
     * Sends a message to Kafka for the specified client ID, message, and topic.
     *
     * @param clientId the ID of the client
     * @param message the message to send
     * @param topic the topic to send the message to
     */
    private void sendMessageToKafa(String clientId, JSONObject message, String topic) {
        hivemqSinkService.sendMsgToSink(clientId, message.toJSONString().getBytes(StandardCharsets.UTF_8), topic);
    }

}
