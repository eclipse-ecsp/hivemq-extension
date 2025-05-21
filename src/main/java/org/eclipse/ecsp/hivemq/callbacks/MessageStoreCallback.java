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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.analytics.stream.base.PropertyNames;
import org.eclipse.ecsp.analytics.stream.base.metrics.reporter.CumulativeLogger;
import org.eclipse.ecsp.analytics.stream.base.utils.ObjectUtils;
import org.eclipse.ecsp.domain.BlobDataV1_0;
import org.eclipse.ecsp.entities.IgniteBlobEvent;
import org.eclipse.ecsp.entities.IgniteDeviceAwareBlobEvent;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.base.TopicMapper;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCache;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
import org.eclipse.ecsp.hivemq.d2v.DeviceToVehicleMapper;
import org.eclipse.ecsp.hivemq.d2v.DeviceToVehicleMapperFactory;
import org.eclipse.ecsp.hivemq.exceptions.IllegalTopicArgumentException;
import org.eclipse.ecsp.hivemq.exceptions.VehicleProfileResponseNotFoundException;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.kafka.util.CompressionJack;
import org.eclipse.ecsp.hivemq.routing.TopicMapperFactory;
import org.eclipse.ecsp.hivemq.routing.TopicMapping;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.eclipse.ecsp.hivemq.transform.IngestionSerializerFactory;
import org.eclipse.ecsp.hivemq.transform.MqttTopicBlobSourceTypeMapper;
import org.eclipse.ecsp.hivemq.utils.HivemqServiceProvider;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.hivemq.utils.KeepAliveHandler;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.hivemq.utils.TopicFormatter;
import org.eclipse.ecsp.key.IgniteStringKey;
import org.eclipse.ecsp.serializer.IngestionSerializer;
import org.eclipse.ecsp.transform.IgniteKeyTransformer;
import org.eclipse.ecsp.transform.IgniteKeyTransformerStringImpl;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Class to store the messages received on publish in Kafka/Kinesis.
 */
@Component
public class MessageStoreCallback extends AbstractOnPublishReceivedCallback {
    private static final @NotNull IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(MessageStoreCallback.class);
    protected static final @NotNull CumulativeLogger CLOGGER = CumulativeLogger.getLogger();

    private static final String MESSAGE_ON_DISCONNECT_TOPIC_TOPIC_FOR_CLIENT_ID_DISCONNECT = "Message "
            + "on DisconnectTopic topic for clientId: {} . Disconnect.....";
    private static final String NULL_PAYLOAD_RECEIVED_AFTER_DECOMPRESSION_NOT_DOING_ANYTHING = "Null "
            + "payload received after decompression.Not doing anything";
    private static final String CLIENT_ID_DEVICE_ID_VEHICLE_ID_FORMATTED_MQTT_TOPIC_KAFKA_TOPIC_SERVICE_ID_SERVICE_NAME
        = "clientId {} , deviceId {} - vehicleId {} - formattedMqttTopic {} - kafkaTopic {} "
                + "- serviceId {} - serviceName {} ";
    private static final String VEHICLEID_EMPTY = "For client {} message is published to topic {} ,"
            + "which does not require a vehicleId. For publish vehicleId is taken as clientId";
    private static final String FOR_CLIENT_MESSAGE_IS_PUBLISHED_TO_TOPIC = "For client {} "
            + "message is published to topic {} ";
    private static final String MESSAGE_PUBLISHED_TO_NON_PERMITTED_TOPIC_FOR_CLIENT_WITH_NO_ASSOCIATED_VEHICLE 
        = "Message published to non permitted topic {} for clientId: {} with no associated vehicle";
    private static final String MESSAGE_PUBLISHED_TO_NON_PERMITTED_TOPIC_FOR_CLIENT_WITH_NO_SUSPICIOUS_VEHICLE 
        = "Message published to non permitted topic {} for clientId: {} with suspicious vehicle";
    protected static final String ERROR_WHILE_GETTING_VEHICLE_ID_FROM_DEVICE_SUBSCRIPTION_CACHE_CLIENT_ID_DEVICE_ID
        = "Error while getting vehicleId from deviceSubscriptionCache, clientId: {}, deviceId: {}";
    private static final String QOS_CHANGE_ENABLED_FOR_CLIENT_FOR_ROUTE = "QosChangeEnabled: {} "
            + "for client: {} for route: {}";
    protected static final String SERVICE_ID_IS_MISSING_SERVICE_ID_FOR_CLIENT_ID = "serviceId is missing. "
            + "serviceId: {} for clientId: {}";
    private static final String TOPIC_MAPPING_IS_MISSING_FOR_CLIENT_ID_TOPIC = "Topic mapping "
            + "is missing for clientId: {} , topic {}";
    private static final String DISCONNECT_REQUEST_RECEIVED_FOR_CLEAR_CACHE_AND_DISCONNECT = "Disconnect "
            + "request received for : {} . Clear cache and Disconnect ...";
    protected static final String MESSAGE_SHOULD_BE_SENT_VIA_DEVICE_MESSAGING = "This message should be sent via"
            + " DeviceMessaging for clientId : {} , mqttTopic {} , messageId: {} , published message qos {} .";
    private static final String TOPIC_IS_REGISTERED_AS_GLOBAL_SUB_TOPIC = "Topic : {} is registered as "
            + "globalSubTopic. This message should be sent via DeviceMessaging for clientId : {} .";
    protected static final String RECEIVED_PATTERN = "Received message on {} with pdid: {}";
    protected static final String SENT_PATTERN1 = "Sent message for clientId: {} "
            + "and deviceId_topicId: {} to sink topic: {} with messageSize: {}";

    protected static final String ALERT = "alert";
    protected static final String EVENT = "events";
    protected TopicFormatter topicFormatter;
    private CompressionJack compressionUtil = new CompressionJack();
    private boolean decompressionEnabled = true;
    protected boolean wrapWithIgniteEventEnabled = false;
    protected HivemqSinkService hivemqSinkService = null;
    protected boolean logPerPdid;
    private final TopicMapper topicMapper;
    protected IngestionSerializer transformer;
    protected IgniteKeyTransformer<String> keyTransformer;
    protected final DeviceSubscriptionCache deviceSubscriptionCache;
    private final KeepAliveHandler keepAliveHandler;
    private List<String> globalSubTopics = HivemqUtils.getGlobalSubTopics();
    private final boolean qosChangeEnabled;
    private final String qosValueFor2C;
    private final String qosValueFor2D;
    private final DeviceToVehicleMapper deviceToVehicleMapper;
    protected static final int TWO = 2;
    protected static final int THREE = 3;
    protected static final int FOUR = 4;
    protected static final int FIVE = 5;
    protected static final int SIX = 6;
    protected static final int SEVEN = 7;

    /**
     * This constructor prepares class with all required objects and properties.
     */
    public MessageStoreCallback() {
        keepAliveHandler = new KeepAliveHandler();
        topicMapper = TopicMapperFactory.getInstance();
        topicMapper.init(PropertyLoader.getProperties());
        decompressionEnabled = Boolean.parseBoolean(PropertyLoader.getValue(ApplicationConstants.DECOMPRESS_ENABLED));
        wrapWithIgniteEventEnabled = Boolean
                .parseBoolean(PropertyLoader.getValue(ApplicationConstants.WRAP_WITH_IGNITE_EVENT_ENABLED));
        if (wrapWithIgniteEventEnabled) {
            initIgniteEventWrapperComponents();
        }
        HivemqSinkService.initializeSinkService();
        hivemqSinkService = HivemqSinkService.getInstance();
        ObjectUtils.requireNonNull(hivemqSinkService, "Uninitialized hivemq sink service.");
        CumulativeLogger.init(PropertyLoader.getProperties());
        logPerPdid = Boolean.parseBoolean(PropertyLoader.getValue(PropertyNames.LOG_PER_PDID));
        deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();

        qosChangeEnabled = Boolean.parseBoolean(PropertyLoader.getValue(AuthConstants.QOS_LEVEL_ENABLED));
        qosValueFor2C = PropertyLoader.getValue(AuthConstants.QOS_LEVEL_2C_VALUE);
        qosValueFor2D = PropertyLoader.getValue(AuthConstants.QOS_LEVEL_2D_VALUE);

        deviceToVehicleMapper = DeviceToVehicleMapperFactory.getInstance();
        deviceToVehicleMapper.init(PropertyLoader.getProperties());
    }

    /**
     * Handles the received publish message.
     *
     * @param publishInboundInput  The input containing information about the received publish message.
     * @param publishInboundOutput The output for handling the publish message.
     */
    @Override
    public @NotNull CompletableFuture<Void> doPublishReceived(
            final @NotNull PublishInboundInput publishInboundInput,
            final @NotNull PublishInboundOutput publishInboundOutput) {
        final String clientId = publishInboundInput.getClientInformation().getClientId();
        try {
            String userName = publishInboundInput.getConnectionInformation()
                    .getConnectionAttributeStore().getAsString(AuthConstants.USERNAME).orElse(StringUtils.EMPTY);
            String mqttTopic = publishInboundInput.getPublishPacket().getTopic();
            if (logPerPdid) {
                CLOGGER.incrementByOne(mqttTopic);
            }

            Optional<TopicMapping> optTopicMapping = validateAndGetTopicMapping(mqttTopic, clientId, userName);
            final TopicMapping topicMapping;
            if (optTopicMapping.isPresent()) {
                topicMapping = optTopicMapping.get();
            } else {
                return CompletableFuture.completedFuture(null);
            }
            
            updatePublishInboundOutputQosLevel(topicMapping, publishInboundOutput, clientId);
            final String serviceId = topicMapping.getServiceId();
            if (StringUtils.isEmpty(serviceId)) {
                LOGGER.error(SERVICE_ID_IS_MISSING_SERVICE_ID_FOR_CLIENT_ID, serviceId, clientId);
                return CompletableFuture.completedFuture(null);
            }
            if (topicMapping.getRoute().equals(TopicMapping.Route.TO_DEVICE)) {
                LOGGER.info(
                        MESSAGE_SHOULD_BE_SENT_VIA_DEVICE_MESSAGING,
                        clientId, mqttTopic, publishInboundInput.getPublishPacket().getPacketId(),
                        publishInboundInput.getPublishPacket().getQos());
                return CompletableFuture.completedFuture(null);
            }

            final String deviceId = topicMapping.getDeviceId();
            if (StringUtils.isEmpty(deviceId)) {
                LOGGER.error("deviceId is missing. mqttTopic: {}", mqttTopic);
                return CompletableFuture.completedFuture(null);
            }
            return CompletableFuture.completedFuture(deviceSubscriptionCache.getSubscription(deviceId))
                    .thenCompose(deviceSubscription ->
                        recreateCacheIfEmpty(deviceSubscription, publishInboundInput, deviceId, clientId)
                    )
                    .exceptionally(throwable -> {
                        LOGGER.error(
                                ERROR_WHILE_GETTING_VEHICLE_ID_FROM_DEVICE_SUBSCRIPTION_CACHE_CLIENT_ID_DEVICE_ID,
                                clientId,
                                deviceId);
                        return null;
                    })
                    .thenAccept(deviceSubscription ->
                    processPublishMessage(deviceSubscription, mqttTopic, clientId, topicMapping,
                                deviceId, serviceId, publishInboundInput)
                    )
                    .whenComplete((unused, throwable) -> {
                        if (throwable != null) {
                            LOGGER.error("clientId: " + clientId + " ,Error while publishing to stream.", throwable);
                        }
                    });
        } catch (final Exception e) {
            LOGGER.error("clientId: " + clientId + " ,Error while publishing to stream.", e);
            return CompletableFuture.completedFuture(null);
        }
    }

    /**
     * Recreates the subscription cache for a given device if it's null.
     *
     * @param deviceSubscription local subscription cache for device
     * @param publishInboundInput The input containing information about the received publish message.
     * @param deviceId The device Id.
     * @param clientId The client Id.
     * @return The recreated device subscription.
     */
    private @NotNull CompletableFuture<@NotNull DeviceSubscription> recreateCacheIfEmpty(
            DeviceSubscription deviceSubscription, PublishInboundInput publishInboundInput,
            String deviceId, String clientId) {
        // need to refresh, in case ecu linked to vehicle
        if (deviceSubscription == null) {
            return recreateSubscriptionCache(deviceId,
                    publishInboundInput.getConnectionInformation()
                            .getConnectionAttributeStore()).thenApply(newDeviceSubscription -> {
                                LOGGER.info("recreating subscription cache, "
                                    + "disconnecting client and clearing cache");
                                clearCacheAndDisconnect(clientId);
                                return newDeviceSubscription;
                            });
        }
        return CompletableFuture.completedFuture(deviceSubscription);
    }

    /**
     * This method processes publish messages from device to cloud.
     *
     * @param deviceSubscription local subscription cache for device
     * @param mqttTopic The MQTT topic.
     * @param clientId The client ID.
     * @param topicMapping The topic mapping.
     * @param deviceId The device Id.
     * @param serviceId The name of the service.
     * @param publishInboundInput The input containing information about the received publish message.
     */
    private void processPublishMessage(DeviceSubscription deviceSubscription, String mqttTopic, String clientId,
            TopicMapping topicMapping, String deviceId, String serviceId, PublishInboundInput publishInboundInput) {
        String[] vehicleIdEcuType = validateTopicGetVehicleId(mqttTopic,
                clientId, topicMapping, deviceSubscription);
        if (vehicleIdEcuType.length == 0) {
            return;
        }
        String vehicleId = vehicleIdEcuType[0];
        String ecuType = vehicleIdEcuType[1];
        // mqttTopic non formatted
        String serviceName = topicMapping.getServiceName();
        final String streamTopic = topicMapping.getStreamTopic();
        LOGGER.debug(CLIENT_ID_DEVICE_ID_VEHICLE_ID_FORMATTED_MQTT_TOPIC_KAFKA_TOPIC_SERVICE_ID_SERVICE_NAME,
                clientId,
                deviceId,
                vehicleId,
                mqttTopic,
                streamTopic,
                serviceId,
                serviceName);

        byte[] payload = decompressAndGetPayload(publishInboundInput, deviceId);
        if (payload != null && payload.length > 0) {
            sendPayLoadToKafka(publishInboundInput, payload, serviceId, deviceId, vehicleId, ecuType, serviceName,
                    mqttTopic, streamTopic, clientId);
        } else {
            LOGGER.warn(NULL_PAYLOAD_RECEIVED_AFTER_DECOMPRESSION_NOT_DOING_ANYTHING);
        }
    }

    /**
     * Validates the topic and retrieves the vehicle ID and ECU type associated with the given parameters.
     *
     * @param mqttTopic The MQTT topic.
     * @param clientId The client ID.
     * @param topicMapping The topic mapping.
     * @param deviceSubscription local subscription cache for device
     * @return An array containing the vehicle ID and ECU type.
     */
    private String[] validateTopicGetVehicleId(
            String mqttTopic, String clientId, TopicMapping topicMapping, DeviceSubscription deviceSubscription) {
        String vehicleId;
        final String ecuType;
        final boolean isSuspicious;
        if (deviceSubscription == null) {
            vehicleId = null;
            ecuType = null;
            isSuspicious = false;
        } else {
            vehicleId = deviceSubscription.getVehicleId();
            ecuType = deviceSubscription.getDeviceType().orElse(StringUtils.EMPTY);
            isSuspicious = deviceSubscription.isSuspicious();
        }
        // if empty vehicleId or devicetype or suspicious device - only
        // commcheck allowed,
        // others ignored. no disconnect
        // Check ecuType empty not allowing whitelisted clients to publish.
        // (like perf simulator)
        if (StringUtils.isEmpty(vehicleId) || isSuspicious) {
            final List<String> acceptableTopics = HivemqUtils.getAcceptableTopics();
            if (!acceptableTopics.isEmpty() && !acceptableTopics.contains(topicMapping.getServiceId())) {
                if (!isSuspicious) {
                    LOGGER.error(MESSAGE_PUBLISHED_TO_NON_PERMITTED_TOPIC_FOR_CLIENT_WITH_NO_ASSOCIATED_VEHICLE,
                            mqttTopic, clientId);
                } else {
                    LOGGER.error(MESSAGE_PUBLISHED_TO_NON_PERMITTED_TOPIC_FOR_CLIENT_WITH_NO_SUSPICIOUS_VEHICLE,
                            mqttTopic, clientId);
                }
                return new String[0];
            }

            if (StringUtils.isEmpty(vehicleId)) {
                LOGGER.warn(VEHICLEID_EMPTY, clientId, mqttTopic);
                vehicleId = clientId;
            } else {
                LOGGER.info(
                        FOR_CLIENT_MESSAGE_IS_PUBLISHED_TO_TOPIC, clientId, mqttTopic);
            }
        }
        return new String[]{vehicleId, ecuType};
    }

    /**
     * Sends the payload to Kafka.
     *
     * @param payload       The payload to be sent.
     * @param payloadParams The parameters related to the payload.
     */
    protected void sendPayLoadToKafka(PublishInboundInput publishInboundInput,
            byte[] payload, String... payloadParams) {
        byte[] pdidKey;
        String serviceId = payloadParams[0];
        String deviceId = payloadParams[1];
        String vehicleId = payloadParams[TWO];
        String ecuType = payloadParams[THREE];
        String serviceName = payloadParams[FOUR];
        String mqttTopic = payloadParams[FIVE];
        String streamTopic = payloadParams[SIX];

        if (wrapWithIgniteEventEnabled) {
            payload = transformToIgniteEvent(deviceId, vehicleId, ecuType, serviceName, serviceId, payload,
                    null);
            IgniteStringKey key = new IgniteStringKey();
            key.setKey(vehicleId);
            pdidKey = keyTransformer.toBlob(key);
        } else {
            pdidKey = vehicleId.getBytes();
        }
        if (mqttTopic.contains(EVENT)) {
            LOGGER.info(RECEIVED_PATTERN, EVENT, vehicleId);
        } else if (mqttTopic.contains(ALERT)) {
            LOGGER.info(RECEIVED_PATTERN, ALERT, vehicleId);
        } else {
            LOGGER.info(RECEIVED_PATTERN, mqttTopic, vehicleId);
        }
        final String deviceIdAndTopicId = vehicleId + "_" + streamTopic;
        hivemqSinkService.sendMsgToSink(pdidKey, payload, streamTopic);
        final int payloadLength = (payload == null) ? 0 : payload.length;
        String clientId = payloadParams[SEVEN];
        LOGGER.info(SENT_PATTERN1, clientId, deviceIdAndTopicId, streamTopic, payloadLength);
    }

    /**
     * Decompresses the payload of a PublishInboundInput and returns the decompressed payload as a byte array.
     *
     * @param publishInboundInput The PublishInboundInput object containing the payload to be decompressed.
     * @param deviceId The ID of the device associated with the payload.
     * @return The decompressed payload as a byte array, or null if the payload is empty or cannot be decompressed.
     */
    private byte[] decompressAndGetPayload(PublishInboundInput publishInboundInput, String deviceId) {
        byte[] payload = null;
        Optional<ByteBuffer> byteValue = publishInboundInput.getPublishPacket().getPayload();
        if (byteValue.isPresent()) {
            String msg = StandardCharsets.ISO_8859_1.decode(byteValue.get()).toString();
            byte[] publishPayload = msg.getBytes(StandardCharsets.ISO_8859_1);
            payload = decompressPayload(deviceId, publishPayload);
        }
        return payload;
    }

    /**
     * Validates and retrieves the topic mapping for the given MQTT topic, client ID, 
     * user name, and publish inbound input.
     *
     * @param mqttTopic The MQTT topic to validate and retrieve the topic mapping for.
     * @param clientId The client ID associated with the MQTT topic.
     * @param userName The user name associated with the MQTT topic.
     * @return An optional TopicMapping object if the validation is successful and the topic 
     *      mapping exists, otherwise an empty optional.
     */
    private Optional<TopicMapping> validateAndGetTopicMapping(String mqttTopic, String clientId, String userName) {
        if (globalSubTopics.contains(mqttTopic)) {
            LOGGER.info(
                    TOPIC_IS_REGISTERED_AS_GLOBAL_SUB_TOPIC,
                    mqttTopic, clientId);
            return Optional.empty();
        }
        if (keepAliveHandler.doProcessKeepAliveMsg(PropertyLoader.getProperties(), clientId, userName, mqttTopic)) {
            return Optional.empty();
        }
        final Optional<TopicMapping> topicMapping;
        try {
            topicMapping = Optional.ofNullable(topicMapper.getTopicMapping(mqttTopic));
        } catch (final IllegalTopicArgumentException ex) {
            LOGGER.error("Mqtt topic is not proper. for clientId: {}  and Mqtt topic: {}", clientId, mqttTopic);
            return Optional.empty();
        }
        if (topicMapping.isEmpty()) {
            LOGGER.error(TOPIC_MAPPING_IS_MISSING_FOR_CLIENT_ID_TOPIC, clientId, mqttTopic);
            return Optional.empty();
        }

        if (topicMapping.get().getRoute().equals(TopicMapping.Route.TO_DEVICE)
                && isDisconnectRequest(mqttTopic, topicMapping.get().getDeviceId())) {
            LOGGER.info(DISCONNECT_REQUEST_RECEIVED_FOR_CLEAR_CACHE_AND_DISCONNECT, topicMapping.get().getDeviceId());
            clearCacheAndDisconnect(topicMapping.get().getDeviceId());
            return Optional.empty();
        }
        return topicMapping;
    }

    /**
     * Updates the QoS level of the PublishInboundOutput based on the TopicMapping and client ID.
     *
     * @param topicMapping The TopicMapping object containing the route information.
     * @param publishInboundOutput The PublishInboundOutput object to be updated.
     * @param clientId The client ID associated with the PublishInboundOutput.
     */
    private void updatePublishInboundOutputQosLevel(TopicMapping topicMapping,
            PublishInboundOutput publishInboundOutput, String clientId) {
        if (qosChangeEnabled) {
            if (topicMapping.getRoute().equals(TopicMapping.Route.TO_CLOUD)) {
                publishInboundOutput.getPublishPacket().setQos(Qos.valueOf(qosValueFor2C));
            }
            if (topicMapping.getRoute().equals(TopicMapping.Route.TO_DEVICE)) {
                publishInboundOutput.getPublishPacket().setQos(Qos.valueOf(qosValueFor2D));
            }
            LOGGER.debug(QOS_CHANGE_ENABLED_FOR_CLIENT_FOR_ROUTE, qosChangeEnabled, clientId,
                    topicMapping.getRoute().equals(TopicMapping.Route.TO_CLOUD) ? qosValueFor2C : qosValueFor2D);
        }
    }

    /**
     * Checks if the given MQTT topic is a disconnect request topic for the specified client ID.
     *
     * @param mqttTopic The MQTT topic to check.
     * @param clientId The client ID associated with the MQTT topic.
     * @return {@code true} if the MQTT topic is a disconnect request topic for the client ID, {@code false} otherwise.
     */
    private boolean isDisconnectRequest(String mqttTopic, String clientId) {
        // rename to disconnect topic, not swap response
        LOGGER.debug("Message on topic: {} for service clientId: {} ", mqttTopic, clientId);
        String disconnectTopic = PropertyLoader.getValue(AuthConstants.DISCONNECT_TOPIC_NAME);
        boolean isDisconnectTopic = StringUtils.isNotEmpty(disconnectTopic) && mqttTopic.contains(disconnectTopic);

        if (isDisconnectTopic) {
            LOGGER.warn(MESSAGE_ON_DISCONNECT_TOPIC_TOPIC_FOR_CLIENT_ID_DISCONNECT, clientId);
        }
        return isDisconnectTopic;
    }

    /**
     * Clears the cache for the specified client and disconnects the client.
     * This method waits to receive a commcheck response by ecu in case of suspicious
     * device or autoswap before disconnecting.
     *
     * @param clientId the ID of the client to clear the cache and disconnect
     */
    private void clearCacheAndDisconnect(final String clientId) {
        // wait to receive commcheck response by ecu, in case of suspicious
        // device or autoswap before disconnect
        CompletableFuture.runAsync(() -> {
            HivemqUtils.clearCache(clientId, true);
            HivemqServiceProvider.getBlockingClientService().disconnectClient(clientId);
        });
    }

    /**
     * Recreates the subscription cache for a given device.
     *
     * @param pdid The device ID.
     * @param attributeStore The connection attribute store.
     * @return The recreated device subscription.
     * @throws VehicleProfileResponseNotFoundException If the vehicle profile response is not found.
     */
    private @NotNull CompletableFuture<@NotNull DeviceSubscription> recreateSubscriptionCache(
            final String pdid, final ConnectionAttributeStore attributeStore)
                    throws VehicleProfileResponseNotFoundException {
        return deviceToVehicleMapper.getVehicleId(pdid, attributeStore).thenApply(vehicleInfo -> {
            final DeviceSubscription deviceSubscription = new DeviceSubscription(
                    Objects.nonNull(vehicleInfo) ? vehicleInfo.getVehicleId() : null);
            deviceSubscription.setDeviceType(
                    Objects.nonNull(vehicleInfo) ? vehicleInfo.getDeviceType() : Optional.empty());
            deviceSubscriptionCache.addSubscription(pdid, deviceSubscription);
            return deviceSubscription;
        });
    }

    /**
     * Decompresses the payload using the specified compression algorithm.
     *
     * @param pdid    the PDID (Payload Device ID) associated with the payload
     * @param payload the payload to be decompressed
     * @return the decompressed payload, or an empty byte array if decompression fails
     */
    private byte[] decompressPayload(final String pdid, final byte[] payload) {
        byte[] finalPayload = payload;
        try {
            if (decompressionEnabled) {
                finalPayload = compressionUtil.decompress(payload);
            }
        } catch (final IOException ex) {
            LOGGER.error("Failed while doing decompression of message for PDID : {} \n {} ", pdid, ex);
            return new byte[0];
        }
        return finalPayload;
    }

    /**
     * Transforms the given parameters into an Ignite event and returns the serialized byte array representation.
     *
     * @param deviceId         The ID of the device.
     * @param vehicleId        The ID of the vehicle.
     * @param ecuType          The type of the ECU.
     * @param serviceName      The name of the service.
     * @param mqttServiceId    The ID of the MQTT service.
     * @param payload          The payload data.
     * @param traceContext     The tracing context.
     * @return The serialized byte array representation of the Ignite event.
     */
    protected byte[] transformToIgniteEvent(String deviceId, String vehicleId, String ecuType, String serviceName,
            String mqttServiceId, byte[] payload, String traceContext) {
        final boolean deviceAwareEnabled = Boolean
                .parseBoolean(PropertyLoader.getValue(ApplicationConstants.DEVICEAWARE_ENABLED));
        String deviceAwareEnabledServices = PropertyLoader
                .getValue(ApplicationConstants.DEVICEAWARE_ENABLED_SERVICES);
        boolean isServiceDeviceAwareEnabled = deviceAwareEnabled && StringUtils.isNotEmpty(deviceAwareEnabledServices)
                && deviceAwareEnabledServices.contains(serviceName);
        LOGGER.debug(
                "deviceId: {}, vehicleId {}, deviceAwareEnabled: {}, deviceAwareEnabledServices, : {}, "
                + "mqttServiceId: {}, serviceName/unformattedTopic: {},  isServiceDeviceAwareEnabled: {}",
                deviceId, vehicleId, deviceAwareEnabled, deviceAwareEnabledServices, mqttServiceId, serviceName,
                isServiceDeviceAwareEnabled);
        IgniteBlobEvent igniteEvent = null;
        if (isServiceDeviceAwareEnabled) {
            igniteEvent = new IgniteDeviceAwareBlobEvent(ecuType, mqttServiceId);
        } else {
            igniteEvent = new IgniteBlobEvent();
        }
        igniteEvent.setTimestamp(System.currentTimeMillis());
        igniteEvent.setSourceDeviceId(deviceId);
        igniteEvent.setVehicleId(vehicleId);
        igniteEvent.setVersion(org.eclipse.ecsp.domain.Version.V1_0);
        igniteEvent.setEventId(ApplicationConstants.EVENT_ID_BLOB);
        igniteEvent.setRequestId(UUID.randomUUID().toString());
        igniteEvent.setTracingContext(traceContext);
        final BlobDataV1_0 blobData = new BlobDataV1_0();
        blobData.setEventSource(MqttTopicBlobSourceTypeMapper.getEventSource(mqttServiceId));
        blobData.setEncoding(MqttTopicBlobSourceTypeMapper.getEncoding(mqttServiceId));
        blobData.setPayload(payload);
        igniteEvent.setEventData(blobData);
        return transformer.serialize(igniteEvent);
    }


    /**
     * Initializes the components required for the Ignite event wrapper.
     * This method initializes the transformer and keyTransformer components.
     */
    private void initIgniteEventWrapperComponents() {
        transformer = IngestionSerializerFactory.getInstance();
        keyTransformer = new IgniteKeyTransformerStringImpl();
    }

    /**
     * Returns the PDID (Prefix and Suffix Removed) from the given MQTT topic.
     *
     * @param mqttTopic The MQTT topic from which to extract the PDID.
     * @param prefix The prefix to be removed from the MQTT topic.
     * @param suffix The suffix to be removed from the MQTT topic.
     * @return The PDID with the prefix and suffix removed.
     */
    public static String getPdid(String mqttTopic, final String prefix, final String suffix) {
        mqttTopic = mqttTopic.replace(prefix, ApplicationConstants.BLANK);
        return mqttTopic.replace(suffix, ApplicationConstants.BLANK);
    }

    // For test case use only
    /**
     * Sets the flag indicating whether to log per PDID.
     *
     * @param logPerPdid the flag indicating whether to log per PDID
     */
    void setLogPerPdid(final boolean logPerPdid) {
        this.logPerPdid = logPerPdid;
    }

    // For test case use only
    /**
     * Sets the list of global subscription topics.
     *
     * @param globalSubTopics the list of global subscription topics to set
     */
    public void setGlobalSubTopics(final List<String> globalSubTopics) {
        this.globalSubTopics = globalSubTopics;
    }

    /**
     * Sets whether to wrap the message store callback with Ignite event enabled.
     *
     * @param wrapWithIgniteEventEnabled true to enable wrapping with Ignite event, false otherwise
     */
    void setWrapWithIgniteEventEnabled(final boolean wrapWithIgniteEventEnabled) {
        this.wrapWithIgniteEventEnabled = wrapWithIgniteEventEnabled;
    }

    /**
     * Sets the flag indicating whether decompression is enabled or not.
     *
     * @param decompressionEnabled true if decompression is enabled, false otherwise
     */
    void setDecompressionEnabled(final boolean decompressionEnabled) {
        this.decompressionEnabled = decompressionEnabled;
    }

    /**
     * Sets the compression utility for the message store callback.
     *
     * @param compressionUtil the compression utility to be set
     */
    void setCompressionUtil(final CompressionJack compressionUtil) {
        this.compressionUtil = compressionUtil;
    }

    /**
     * Sets the topic formatter for the MessageStoreCallback.
     *
     * @param topicFormatter the topic formatter to be set
     */
    public void setTopicFormatter(final TopicFormatter topicFormatter) {
        this.topicFormatter = topicFormatter;
        keepAliveHandler.setTopicFormatter(topicFormatter);
    }
}
