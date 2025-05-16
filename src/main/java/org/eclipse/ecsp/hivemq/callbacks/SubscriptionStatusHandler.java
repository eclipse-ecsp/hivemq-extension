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

package org.eclipse.ecsp.hivemq.callbacks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.analytics.stream.base.utils.ObjectUtils;
import org.eclipse.ecsp.cache.DeleteMapOfEntitiesRequest;
import org.eclipse.ecsp.cache.GetMapOfEntitiesRequest;
import org.eclipse.ecsp.cache.PutMapOfEntitiesRequest;
import org.eclipse.ecsp.cache.redis.IgniteCacheRedisImpl;
import org.eclipse.ecsp.domain.DeviceConnStatusV1_0;
import org.eclipse.ecsp.domain.DeviceConnStatusV1_0.ConnectionStatus;
import org.eclipse.ecsp.domain.EventID;
import org.eclipse.ecsp.domain.Version;
import org.eclipse.ecsp.entities.IgniteEventImpl;
import org.eclipse.ecsp.entities.dma.VehicleIdDeviceIdMapping;
import org.eclipse.ecsp.hivemq.base.TopicMapper;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCache;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
import org.eclipse.ecsp.hivemq.exceptions.ConnectionStatusUnknownException;
import org.eclipse.ecsp.hivemq.exceptions.IllegalTopicArgumentException;
import org.eclipse.ecsp.hivemq.exceptions.InvalidSubscriptionException;
import org.eclipse.ecsp.hivemq.redis.constants.RedisConstants;
import org.eclipse.ecsp.hivemq.routing.TopicMapperFactory;
import org.eclipse.ecsp.hivemq.routing.TopicMapping;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.key.IgniteStringKey;
import org.eclipse.ecsp.transform.IgniteKeyTransformer;
import org.eclipse.ecsp.transform.IgniteKeyTransformerStringImpl;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * It sends the device mqtt topic subscription status to stream. It has a API
 * which send the status which is used by UnSubscriptionCallback and
 * ClientDisconnect plugin
 */
@Component
public class SubscriptionStatusHandler {

    private static final String REQUEST_SUBSCRIPTION_FOR_CLIENT_ID_VEHICLE_ID_TOPICS_STATUS = "Request "
            + "subscription for clientId: {}, vehicleId: {}, topics: {}, status: {}";
    private static final String THE_TOPICS_FOR_WHICH_SUBSCRIPTION_WERE_REQUESTED_FROM_CLIENT_ID_ARE_INVALID = 
            "The topics: {} for which subscription were requested from clientId: {} are invalid";
    private static final String STATUS_PAYLOAD_AND_SEND_TO_STREAM_TOPIC = "Status Payload {} "
            + "and send to stream topic {}";
    private static final String REDIS_KEY_FOR_CLIENT_ID_VEHICLE_ID_IS = "Redis key for clientId: {}, "
            + "vehicleId: {} is: {}";
    private static final String FOR_CLIENT_ID = "For clientId : ";
    private static final String DEVICE_STATUS_COULD_NOT_BE_PUSHED_TO_SERVICE = ", device status could not be "
            + "pushed to service : ";
    private static final String DEVICE_STATUS_COULD_NOT_BE_PUSHED_TO_SERVICE_LOGGING_SCOPE_E2E_DVI_EXCEPTION_IS = 
            "Device status could not be pushed to service: {}, clientId: {}, vehicleId: {} "
            + ".Logging scope: E2E, DVI. Exception is : ";
    private static final String UPDATED_IN_REDIS_AS = ", updated in redis as ";
    private static final String UPDATED_IN_KAFKA = ", updated in kafka ";
    private static final String DEVICE_STATUS = " , deviceStatus: ";
    private static final String SERVICE = " service: ";
    private static final String VEHICLE_ID = " vehicleId: ";
    private static final String CLIENT_ID = "ClientId: ";
    private static final String NEXT_LINE = System.getProperty("line.separator");
    private static final String IT_IS_A_PUBLISH_MQTT_TOPIC_SO_NO_NEED_TO_SEND_DEVICE_STATUS = "It is a "
            + "publish mqtt topic {}, so no need to send device status.";
    private static final String DEVICE_STATUS_IS_NOT_REQUIRED_FOR_MQTT_TOPIC = "device status "
            + "is not required for mqtt topic ";
    private static final String UPDATED_IN_DEVICE_SUBSCRIPTION_CACHE_AS = ", updated in DeviceSubscription cache as ";
    private static final String LOGGING_SCOPE_E2E_DVI = ".Logging scope: E2E, DVI";
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(SubscriptionStatusHandler.class);
    private static final String VEHICLE_DEVICE_MAPPING = RedisConstants.VEHICLE_DEVICE_MAPPING + RedisConstants.COLON;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static IgniteCacheRedisImpl igniteCacheRedisImpl = null;

    private HivemqSinkService hivemqSinkService = null;
    private TopicMapper topicMapper;
    private IgniteKeyTransformer<String> keyTransformer;
    private DeviceSubscriptionCache deviceSubscriptionCache;
    private List<String> globalSubTopics;

    /**
     * This constructor loads required properties and get required class objects on startup.
     */
    public SubscriptionStatusHandler() {
        Map<String, String> props = PropertyLoader.getRedisPropertiesMap();
        ObjectUtils.requireNonNull(props, "Received null properties, can't initialize redis. Aborting..");
        globalSubTopics = HivemqUtils.getGlobalSubTopics();
        hivemqSinkService = HivemqSinkService.getInstance();
        ObjectUtils.requireNonNull(hivemqSinkService, "Unable to initialize the hive sink");
        topicMapper = TopicMapperFactory.getInstance();
        topicMapper.init(PropertyLoader.getProperties());
        ObjectUtils.requireNonNull(topicMapper, "Unable to initialize the topic mapper");
        keyTransformer = new IgniteKeyTransformerStringImpl();
        deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        MAPPER.setFilterProvider(new SimpleFilterProvider().setFailOnUnknownId(false));
    }

    /**
     * Sets the instance of IgniteCacheRedisImpl to be used by the SubscriptionStatusHandler.
     *
     * @param igniteCacheRedisImpl The IgniteCacheRedisImpl instance to be set.
     */
    public static void setIgniteCacheRedisImpl(IgniteCacheRedisImpl igniteCacheRedisImpl) {
        SubscriptionStatusHandler.igniteCacheRedisImpl = igniteCacheRedisImpl;
    }

    /**
     * This method handles subscription requests from client.
     *
     * @param clientId - client id
     * @param topicList - subscribed topic list
     * @param connectionStatus - active/inactive
     * @throws InvalidSubscriptionException - if subscription is on invalid topic
     */
    public void handle(String clientId, Set<String> topicList, ConnectionStatus connectionStatus)
            throws InvalidSubscriptionException {
        DeviceSubscription deviceSubscription = deviceSubscriptionCache.getSubscription(clientId);
        String vehicleId = Objects.nonNull(deviceSubscription) ? deviceSubscription.getVehicleId() : null;
        boolean isSuspicious = Objects.nonNull(deviceSubscription) && deviceSubscription.isSuspicious();
        LOGGER.debug("clientId: {} and its vehicleId {} , deviceSubscription: {}", clientId, vehicleId,
                deviceSubscription);
        handle(clientId, vehicleId, isSuspicious, topicList, connectionStatus);
    }

    /**
     * This is to handle subscriptions.
     *
     * <p>if no linked vehicle or suspicious device, ignore subscription requests to
     * non permitted topics (no disconnect). only subscribe to acceptableTopics
     */
    public void handle(String clientId, String vehicleId, boolean isSuspicious, Set<String> topicSet,
            ConnectionStatus connectionStatus) throws InvalidSubscriptionException {

        LOGGER.debug(REQUEST_SUBSCRIPTION_FOR_CLIENT_ID_VEHICLE_ID_TOPICS_STATUS, clientId, vehicleId, topicSet,
                connectionStatus);

        if (StringUtils.isEmpty(vehicleId)) {
            LOGGER.warn("A client which is not linked to any vehicle id has connected, use clientId: {} instead",
                    clientId);
            vehicleId = clientId;
        }
        removeGlobalTopicFromList(topicSet, clientId, connectionStatus);

        StringBuilder topicsSubscriptionStatus = new StringBuilder();
        topicsSubscriptionStatus.append(CLIENT_ID).append(clientId).append(VEHICLE_ID).append(vehicleId);

        LOGGER.debug("clientId: {} and its vehicleId in cache {}", clientId, vehicleId);
        TopicMapping topicMapping;
        for (String mqttTopic : topicSet) {
            LOGGER.debug("Current Mqtt topic {} ", mqttTopic);
            Optional<TopicMapping> optTopicMapping = getTopicMapping(mqttTopic, clientId);
            if (optTopicMapping.isPresent()) {
                topicMapping = optTopicMapping.get();
            } else {
                LOGGER.error("Invalid mqttTopic {}, no service mapping found", mqttTopic);
                return;
            }

            boolean isPublishTopicValid = validateIsPublishOrDeviceStatusRequired(topicMapping,
                    topicsSubscriptionStatus, mqttTopic);
            if (isPublishTopicValid) {
                continue;
            }

            List<String> acceptableTopics = HivemqUtils.getAcceptableTopics();
            boolean isRestrictedAccess = StringUtils.isEmpty(vehicleId) || isSuspicious;

            if (isRestrictedAccess && !acceptableTopics.isEmpty()
                    && !acceptableTopics.contains(topicMapping.getServiceId())) {
                LOGGER.warn(THE_TOPICS_FOR_WHICH_SUBSCRIPTION_WERE_REQUESTED_FROM_CLIENT_ID_ARE_INVALID, topicSet,
                        clientId);
                return;
            }

            topicsSubscriptionStatus.append(SERVICE).append(mqttTopic).append(DEVICE_STATUS).append(connectionStatus);
            try {
                sendSubscriptionAlertToKafkaAndUpdateRedis(clientId, vehicleId, connectionStatus, topicMapping,
                        mqttTopic, topicsSubscriptionStatus);
            } catch (Exception e) {
                // If error log successful subscription status till now
                topicsSubscriptionStatus.append(LOGGING_SCOPE_E2E_DVI);
                LOGGER.info(topicsSubscriptionStatus.toString());
                LOGGER.error(
                        DEVICE_STATUS_COULD_NOT_BE_PUSHED_TO_SERVICE_LOGGING_SCOPE_E2E_DVI_EXCEPTION_IS,
                        mqttTopic, clientId, vehicleId, e);
                throw new InvalidSubscriptionException(
                        FOR_CLIENT_ID + clientId + DEVICE_STATUS_COULD_NOT_BE_PUSHED_TO_SERVICE + mqttTopic, e);
            }

        }
        topicsSubscriptionStatus.append(LOGGING_SCOPE_E2E_DVI);
        LOGGER.info(topicsSubscriptionStatus.toString());

    }
    
    /**
     * This method removes global topic form topicSet as global topic status not required to be saved
     * in redis or kafka.
     *
     * @param topicSet set of subscribed topic.
     * @param clientId The clientId.
     * @param connectionStatus connection status of device ACTIVE/INACTIVE
     */
    private void removeGlobalTopicFromList(Set<String> topicSet, final String clientId, 
            final ConnectionStatus connectionStatus) {
        if (globalSubTopics != null && !globalSubTopics.isEmpty()) {
            for (String globalTopic : globalSubTopics) {
                if (topicSet.contains(globalTopic)) {
                    LOGGER.info("Topic: {} is a global_subscribe_topic for clientId: {}, connection status: {}", 
                            globalTopic, clientId, connectionStatus);
                    topicSet.remove(globalTopic);
                }
            }
        }
    }
    
    /**
     * Retrieves the topic mapping for the given MQTT topic and client ID.
     *
     * @param mqttTopic The MQTT topic to retrieve the mapping for.
     * @param clientId The client ID associated with the MQTT topic.
     * @return An optional {@link TopicMapping} object representing the mapping for the given MQTT topic,
     *      or an empty optional if no mapping is found.
     * @throws InvalidSubscriptionException If the MQTT topic is not proper.
     */
    private Optional<TopicMapping> getTopicMapping(String mqttTopic, String clientId) {
        Optional<TopicMapping> topicMapping;
        try {
            topicMapping = Optional.ofNullable(topicMapper.getTopicMapping(mqttTopic));
        } catch (IllegalTopicArgumentException ex) {
            LOGGER.error("Mqtt topic {} is not proper for {}", mqttTopic, clientId);
            throw new InvalidSubscriptionException("Mqtt topic is not proper. Mqtt topic: " + mqttTopic);
        }

        return topicMapping;
    }

    /**
     * Validates whether the given MQTT topic requires publishing or device status.
     *
     * @param topicMapping The topic mapping object containing the route and device status requirement.
     * @param topicsSubscriptionStatus The StringBuilder object to store the subscription status.
     * @param mqttTopic The MQTT topic to be validated.
     * @return true if the MQTT topic is a publish topic or device status is not required, false otherwise.
     */
    private boolean validateIsPublishOrDeviceStatusRequired(TopicMapping topicMapping,
            StringBuilder topicsSubscriptionStatus, String mqttTopic) {
        boolean isPublishTopic = topicMapping.getRoute().equals(TopicMapping.Route.TO_CLOUD);
        if (isPublishTopic || !topicMapping.isDeviceStatusRequired()) {
            if (isPublishTopic) {
                LOGGER.debug(IT_IS_A_PUBLISH_MQTT_TOPIC_SO_NO_NEED_TO_SEND_DEVICE_STATUS, mqttTopic);
            }
            if (!topicMapping.isDeviceStatusRequired()) {
                topicsSubscriptionStatus.append(DEVICE_STATUS_IS_NOT_REQUIRED_FOR_MQTT_TOPIC).append(mqttTopic);
            }
            return true;
        }
        return false;
    }

    /**
     * Sends a subscription alert to Kafka and updates Redis cache.
     *
     * @param clientId                the client ID
     * @param vehicleId               the vehicle ID
     * @param connectionStatus        the connection status
     * @param topicMapping            the topic mapping
     * @param mqttTopic               the MQTT topic
     * @param topicsSubscriptionStatus the subscription status of topics
     * @throws JsonProcessingException if an error occurs while processing JSON
     */
    private void sendSubscriptionAlertToKafkaAndUpdateRedis(String clientId, String vehicleId,
            ConnectionStatus connectionStatus, TopicMapping topicMapping, String mqttTopic,
            StringBuilder topicsSubscriptionStatus) throws JsonProcessingException {
        IgniteStringKey key = new IgniteStringKey(vehicleId);
        byte[] pdidKey = keyTransformer.toBlob(key);
        IgniteEventImpl eventImpl = getSubscriptionEvent(clientId, vehicleId, connectionStatus,
                topicMapping.getServiceId());
        String payload = MAPPER.writeValueAsString(eventImpl);
        LOGGER.debug(STATUS_PAYLOAD_AND_SEND_TO_STREAM_TOPIC, payload, topicMapping.getStreamStatusTopic());
        hivemqSinkService.sendMsgToSink(pdidKey, payload.getBytes(StandardCharsets.UTF_8),
                topicMapping.getStreamStatusTopic());
        topicsSubscriptionStatus.append(UPDATED_IN_KAFKA);
        // redisKey format:
        // VEHICLE_DEVICE_MAPPING:<ServiceName><VehicleId>
        String mapKey = VEHICLE_DEVICE_MAPPING + topicMapping.getServiceName();
        LOGGER.debug(REDIS_KEY_FOR_CLIENT_ID_VEHICLE_ID_IS, clientId, vehicleId, mapKey);
        updateRedisCache(clientId, mapKey, connectionStatus, vehicleId);
        boolean isActive = connectionStatus.equals(ConnectionStatus.ACTIVE);
        topicsSubscriptionStatus.append(mqttTopic).append(UPDATED_IN_REDIS_AS).append(isActive);
        if (isActive) {
            deviceSubscriptionCache.addTopic(clientId, vehicleId, mqttTopic);
        } else {
            deviceSubscriptionCache.removeTopic(clientId, mqttTopic);
        }
        topicsSubscriptionStatus.append(UPDATED_IN_DEVICE_SUBSCRIPTION_CACHE_AS).append(connectionStatus)
                .append(NEXT_LINE);
    }

    /**
     * Creates and returns an instance of IgniteEventImpl representing a subscription event.
     *
     * @param deviceId          The ID of the device.
     * @param vehicleId         The ID of the vehicle.
     * @param connectionStatus  The connection status of the device.
     * @param serviceId         The ID of the service.
     * @return                  An instance of IgniteEventImpl representing the subscription event.
     */
    private IgniteEventImpl getSubscriptionEvent(String deviceId, String vehicleId, ConnectionStatus connectionStatus,
            String serviceId) {
        DeviceConnStatusV1_0 connStatus = new DeviceConnStatusV1_0();
        connStatus.setConnStatus(connectionStatus);
        connStatus.setServiceName(serviceId);

        IgniteEventImpl eventImpl = new IgniteEventImpl();
        eventImpl.setEventData(connStatus);
        eventImpl.setVehicleId(vehicleId);
        eventImpl.setSourceDeviceId(deviceId);
        eventImpl.setVersion(Version.V1_0);
        eventImpl.setTimestamp(System.currentTimeMillis());
        eventImpl.setEventId(EventID.DEVICE_CONN_STATUS);
        return eventImpl;
    }

    // This method is used to
    // cascade data to ignite cache.
    // ensure kafka and redis update is atomic and handle exceptions from kafka
    // and redis
    /**
     * Updates the Redis cache with the given client ID, map key, connection status, and vehicle ID.
     *
     * @param clientId         The client ID.
     * @param mapKey           The map key.
     * @param connectionStatus The connection status.
     * @param vehicleId        The vehicle ID.
     */
    private void updateRedisCache(String clientId, String mapKey, ConnectionStatus connectionStatus, String vehicleId) {

        if (igniteCacheRedisImpl == null) {
            return;
        }
        LOGGER.debug("Updating cache for clientId: {} , vehicleId: {} , mapKey: {} with connection status: {}",
                clientId, vehicleId, mapKey, connectionStatus);

        GetMapOfEntitiesRequest getReq = new GetMapOfEntitiesRequest();
        getReq.withKey(mapKey);
        Set<String> fields = new HashSet<>();
        fields.add(vehicleId);
        getReq.withFields(fields);

        Map<String, VehicleIdDeviceIdMapping> valueMap = igniteCacheRedisImpl.getMapOfEntities(getReq);
        VehicleIdDeviceIdMapping vehicleIdDeviceIdMapping = null;
        if (valueMap != null && valueMap.containsKey(vehicleId)) {
            vehicleIdDeviceIdMapping = valueMap.get(vehicleId);
        }

        switch (connectionStatus) {
            case ACTIVE:
                if (null == vehicleIdDeviceIdMapping) {
                    vehicleIdDeviceIdMapping = new VehicleIdDeviceIdMapping();
                }
                vehicleIdDeviceIdMapping.addDeviceId(clientId);
                break;
            case INACTIVE:
                if (null != vehicleIdDeviceIdMapping && vehicleIdDeviceIdMapping.getDeviceIds().contains(clientId)) {
                    vehicleIdDeviceIdMapping.deleteDeviceId(clientId);
                }
                // If we removed a deviceId (== clientId)
                // and if that happens to
                // be the last element in map
                // for a given key, then we delete the
                // entire entry from Cache.
                if (null != vehicleIdDeviceIdMapping && vehicleIdDeviceIdMapping.getDeviceIds().isEmpty()) {
                    DeleteMapOfEntitiesRequest delete = new DeleteMapOfEntitiesRequest();
                    delete.withKey(mapKey);
                    delete.withFields(fields);
                    igniteCacheRedisImpl.deleteMapOfEntities(delete);
                }
                break;

            default:
                throw new ConnectionStatusUnknownException("Unknown Connection Status " + connectionStatus);
        }

        if (null != vehicleIdDeviceIdMapping && !vehicleIdDeviceIdMapping.getDeviceIds().isEmpty()) {
            PutMapOfEntitiesRequest<VehicleIdDeviceIdMapping> putMapEntityRequest = new PutMapOfEntitiesRequest<>();
            putMapEntityRequest.withKey(mapKey);
            valueMap = new HashMap<>();
            valueMap.put(vehicleId, vehicleIdDeviceIdMapping);
            putMapEntityRequest.withValue(valueMap);
            LOGGER.debug("Updating cache for mapKey: {} and vehicleId: {} ", mapKey, vehicleId);
            igniteCacheRedisImpl.putMapOfEntities(putMapEntityRequest);
        }
    }

    /**
     * This method removes subscription details from local cache and redis and sends inactive
     * device status kafka messages for subscribed topics.
     *
     * @param clientId - client id
     * @param removeFromRedis - true, if required to remove from redis
     */
    public void removeSubscription(String clientId, boolean removeFromRedis) {
        // local cache
        DeviceSubscription deviceSubscription = deviceSubscriptionCache.removeSubscription(clientId);
        if (Objects.nonNull(deviceSubscription) && removeFromRedis) {
            LOGGER.debug("Sending Inactive status for clientId: {}, topic: {}", clientId, deviceSubscription);
            try {
                handle(clientId, deviceSubscription.getVehicleId(), deviceSubscription.isSuspicious(),
                        deviceSubscription.getSubscriptionTopics(), ConnectionStatus.INACTIVE);
            } catch (InvalidSubscriptionException e) {
                LOGGER.warn("Issue while removing subscription for clientId: {} , topic: {} ", clientId);
                // Add Retry logic because status to sp components will be shown as ACTIVE
                // in case of cache update while connect need to handle
            }
        }
    }
}
