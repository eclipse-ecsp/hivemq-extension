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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundOutput;
import org.eclipse.ecsp.analytics.stream.base.utils.ObjectUtils;
import org.eclipse.ecsp.domain.DeviceConnStatusV1_0.ConnectionStatus;
import org.eclipse.ecsp.hivemq.base.TopicMapper;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCache;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
import org.eclipse.ecsp.hivemq.callbacks.SubscriptionStatusHandler;
import org.eclipse.ecsp.hivemq.exceptions.InvalidSubscriptionException;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.routing.TopicMapperFactory;
import org.eclipse.ecsp.hivemq.routing.TopicMapping;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class intercept all unsubscribe requests.
 */
@Component
@ConditionalOnProperty(name = "ssdp.simulator", havingValue = "true")
public class SimulatorUnsubscribeInboundIntercept implements UnsubscribeInboundInterceptor {

    private static final IgniteLogger LOGGER 
        = IgniteLoggerFactory.getLogger(SimulatorUnsubscribeInboundIntercept.class);

    @Autowired
    private SubscriptionStatusHandler handler;
    private DeviceSubscriptionCache deviceSubscriptionCache;
    private TopicMapper topicMapper;
    private HivemqSinkService hivemqSinkService;
    private String externalPresenceTopic;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    /**
     * This constructor loads all required properties and get class instances on application startup. 
     */
    public SimulatorUnsubscribeInboundIntercept() {
        deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        topicMapper = TopicMapperFactory.getInstance();
        topicMapper.init(PropertyLoader.getProperties());
        ObjectUtils.requireNonNull(topicMapper, "Unable to initialize the topic mapper");
        hivemqSinkService = HivemqSinkService.getInstance();
        ObjectUtils.requireNonNull(hivemqSinkService, "Unable to initialize the hive sink");
        externalPresenceTopic = PropertyLoader.getValue(ApplicationConstants.KAFKA_SINK_TOPIC_PRESENCE_MANAGER);
    }

    /**
     * This method is called when an unsubscribe packet is received from a client.
     * It processes the unsubscribe request and performs the necessary actions based on the client's subscription.
     *
     * @param unsubscribeInboundInput  The input object containing information about the unsubscribe request.
     * @param unsubscribeInboundOutput The output object used to control the behavior of the unsubscribe process.
     */
    @Override
    public void onInboundUnsubscribe(@NotNull UnsubscribeInboundInput unsubscribeInboundInput,
            @NotNull UnsubscribeInboundOutput unsubscribeInboundOutput) {
        String clientId = unsubscribeInboundInput.getClientInformation().getClientId();
        Set<String> topicSet = unsubscribeInboundInput.getUnsubscribePacket().getTopicFilters().stream()
                .collect(Collectors.toSet());
        LOGGER.info("Unsubscription clientId: {} , topics: {}", clientId, topicSet);
        DeviceSubscription subscriptionCache = deviceSubscriptionCache.getSubscription(clientId);
        if (subscriptionCache.isSsdpVehicle()) {
            Set<String> topicService = new HashSet<>();
            boolean isDeviceStatusRequired = false;
            for (String topic : topicSet) {
                TopicMapping topicMapping = topicMapper.getTopicMapping(topic);
                if (topicMapping.isDeviceStatusRequired()) {
                    topicService.add(topicMapping.getServiceName());
                    isDeviceStatusRequired = true;
                }
            }
            if (isDeviceStatusRequired) {
                JSONObject unsubscribePayload = createSsdpUnsubscribeEvent(clientId, topicService);
                sendMessageToKafka(clientId, unsubscribePayload, externalPresenceTopic);
            }
        } else {
            try {
                handler.handle(clientId, topicSet, ConnectionStatus.INACTIVE);
            } catch (InvalidSubscriptionException e) {
                LOGGER.error("Unsubscribe tried by a client, clientId: " + clientId + " without a vehicleId ", e);
            }
        }
    }

    /**
     * This method prepares kafka event for ssdp client unsubscribe request.
     *
     * @param clientId - client id
     * @param topicService - set of topics
     * @return - json object payload
     */
    @SuppressWarnings("unchecked")
    public JSONObject createSsdpUnsubscribeEvent(String clientId, Set<String> topicService) {
        JSONObject eventObj = new JSONObject();
        eventObj.put(SimulatorEventMetadataConstants.UIN, clientId);
        eventObj.put(SimulatorEventMetadataConstants.STATE, SimulatorEventMetadataConstants.ONLINE);

        String parsedDate = LocalDateTime.now().format(FORMATTER);
        eventObj.put(SimulatorEventMetadataConstants.UPDATED, parsedDate);

        JSONArray serviceArray = new JSONArray();
        JSONObject serviceObj = new JSONObject();
        Set<String> subscribedTopic = deviceSubscriptionCache.getSubscription(clientId).getSubscriptionTopics();
        Set<String> subscribedTopicCopy = new HashSet<>(subscribedTopic);
        for (String topic : subscribedTopicCopy) {
            JSONObject subscriptionStatusObj = new JSONObject();

            if (topicService.contains(topic)) {
                subscriptionStatusObj.put(SimulatorEventMetadataConstants.STATE,
                        SimulatorEventMetadataConstants.OFFLINE);
                deviceSubscriptionCache.getSubscription(clientId).removeSusbcription(topic);
            } else {
                subscriptionStatusObj.put(SimulatorEventMetadataConstants.STATE,
                        SimulatorEventMetadataConstants.ONLINE);
            }
            subscriptionStatusObj.put(SimulatorEventMetadataConstants.UPDATED, parsedDate);
            serviceObj.put(topic, subscriptionStatusObj);
        }
        if (!serviceObj.isEmpty()) {
            serviceArray.add(serviceObj);
            eventObj.put(SimulatorEventMetadataConstants.SERVICES, serviceArray);
        } else {
            eventObj.put(SimulatorEventMetadataConstants.SERVICES, null);
        }
        return eventObj;
    }

    /**
     * Sends a message to Kafka for the specified client ID, message, and topic.
     *
     * @param clientId the ID of the client
     * @param message the message to send
     * @param topic the topic to send the message to
     */
    private void sendMessageToKafka(String clientId, JSONObject message, String topic) {
        hivemqSinkService.sendMsgToSink(clientId, message.toJSONString().getBytes(StandardCharsets.UTF_8), topic);
    }
}
