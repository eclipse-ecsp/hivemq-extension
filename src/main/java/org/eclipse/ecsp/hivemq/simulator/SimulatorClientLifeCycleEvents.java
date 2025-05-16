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
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.Listener;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListener;
import com.hivemq.extension.sdk.api.events.client.parameters.AuthenticationSuccessfulInput;
import com.hivemq.extension.sdk.api.events.client.parameters.ConnectionStartInput;
import com.hivemq.extension.sdk.api.events.client.parameters.DisconnectEventInput;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extension.sdk.api.services.Services;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.analytics.stream.base.utils.ObjectUtils;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.base.TopicMapper;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCache;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
import org.eclipse.ecsp.hivemq.cache.TokenExpiryHandler;
import org.eclipse.ecsp.hivemq.callbacks.AbstractSubscribeInboundInterceptor;
import org.eclipse.ecsp.hivemq.config.ScheduledThreadPoolFactory;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.routing.TopicMapperFactory;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.eclipse.ecsp.hivemq.utils.EventMetadataConstants;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class provides implementations to client lifecycle events provided in 
 * ClientLifecycleEventListener class(provided by hivemq).
 * 
 * <ul>
 *   <li>A client starts an MQTT connection
 *   <li>The authentication for a client is successful
 *   <li>A client is disconnected
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "ssdp.simulator", havingValue = "true")
public class SimulatorClientLifeCycleEvents implements ClientLifecycleEventListener {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(SimulatorClientLifeCycleEvents.class);
    private static final String CONNECT_LOG_MESSAGE = "Client with clientId: {} is trying to connect "
            + "using protocolVersion: {}, port: {}, user: {}, "
            + "keepAliveTime: {}, cleanSession: {}, willQos: {}, isWill: {}, isWillretain: {}, "
            + "contains topics: {} of LWT message: {}";
    private static final String CONNECT_LOG_MESSAGE_WO_WILL = "Client with clientId: {} trying to connect "
            + "using protocolVersion: {}, port: {}, user: {}, "
            + "keepAliveTime: {}, cleanSession: {}, isWill: {}";
    private static final String FINAL_CONNECT_MESSAGE = "Client with clientId: {} is connected successfully";
    private HivemqSinkService hivemqSinkService;
    private TopicMapper topicMapper;
    private DeviceSubscriptionCache deviceSubscriptionCache;
    private static volatile AnnotationConfigApplicationContext applicationContext;
    private static ScheduledExecutorService extensionExecutorService;
    private static final Long THREAD_DELAY = Long
            .parseLong(PropertyLoader.getValue(ApplicationConstants.THREAD_DELAY, "200"));
    private String externalPresenceTopic = PropertyLoader
            .getValue(ApplicationConstants.KAFKA_SINK_TOPIC_PRESENCE_MANAGER);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final Object LOCK = new Object();
    private static final int MINUS_ONE = -1;

    /**
     * This constructor gets required class instances on startup.
     * <ul>
     *  <li>Kafka instance</li>
     *  <li>Topic mapper instance</li>
     *  <li>DeviceSubscription local cache instance</li>
     * </ul>
     *  
     */
    public SimulatorClientLifeCycleEvents() {
        hivemqSinkService = HivemqSinkService.getInstance();
        ObjectUtils.requireNonNull(hivemqSinkService, "Unable to initialize the hive sink");
        topicMapper = TopicMapperFactory.getInstance();
        ObjectUtils.requireNonNull(topicMapper, "Unable to initialize the topic mapper");
        deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();

        synchronized (LOCK) {
            if (applicationContext == null) {
                LOGGER.info("initializing application context for disconnect");
                applicationContext = new AnnotationConfigApplicationContext(ScheduledThreadPoolFactory.class);
            }

            if (extensionExecutorService == null) {
                extensionExecutorService = (ScheduledExecutorService) applicationContext
                        .getBean("scheduledDisconnectThreadPool");
            }
        }
    }

    /**
     * This method is called when an MQTT connection starts.
     *
     * @param connectionStartInput The input containing the connection start information.
     */
    @Override
    public void onMqttConnectionStart(@NotNull ConnectionStartInput connectionStartInput) {
        ConnectPacket connect = connectionStartInput.getConnectPacket();
        ConnectionInformation connectData = connectionStartInput.getConnectionInformation();

        if (isHealthCheckUser(connectionStartInput)) {
            return;
        }

        int port = getListenerPort(connectData);
        String userName = connect.getUserName().orElse(null);

        Optional<WillPublishPacket> willPublish = connect.getWillPublish();
        if (willPublish.isPresent()) {
            LOGGER.info(CONNECT_LOG_MESSAGE, connect.getClientId(), connect.getMqttVersion(), port, userName,
                    connect.getKeepAlive(), connect.getCleanStart(), willPublish.get().getQos(),
                    willPublish.isPresent(), willPublish.get().getRetain(), willPublish.get().getTopic(),
                    willPublish.get().getPayload().orElse(null));
        } else {
            LOGGER.info(CONNECT_LOG_MESSAGE_WO_WILL, connect.getClientId(), connect.getMqttVersion(), port, userName,
                    connect.getKeepAlive(), connect.getCleanStart(), willPublish.isPresent());
        }

    }

    /**
     * Returns the port number of the listener associated with the given connection information.
     *
     * @param connectData the connection information
     * @return the port number of the listener, or -1 if no listener is present
     */
    protected int getListenerPort(ConnectionInformation connectData) {
        Optional<Listener> listener = connectData.getListener();
        if (listener.isPresent()) {
            return listener.get().getPort();
        }
        return MINUS_ONE;
    }

    /**
     * Checks if the user is a health check user.
     *
     * @param connectionStartInput the input containing connection start information
     * @return true if the user is a health check user, false otherwise
     */
    protected boolean isHealthCheckUser(ConnectionStartInput connectionStartInput) {
        ClientInformation clientData = connectionStartInput.getClientInformation();
        ConnectPacket connect = connectionStartInput.getConnectPacket();
        ConnectionInformation connectData = connectionStartInput.getConnectionInformation();
        final ConnectionAttributeStore connectionAttributeStore = connectData.getConnectionAttributeStore();
        connect.getUserName().ifPresent(user -> connectionAttributeStore.put(AuthConstants.USERNAME,
                ByteBuffer.wrap(connect.getUserName().get().getBytes(StandardCharsets.UTF_8))));

        if (HivemqUtils.isHealthCheckUser(PropertyLoader.getProperties(), connectData)) {
            LOGGER.debug("Health check user {} with username {} is connected", clientData.getClientId(),
                    connect.getUserName());
            return true;
        }

        return false;
    }

    /**
     * Called when authentication is successful for a client.
     *
     * @param authenticationSuccessfulInput The input containing information about the successful authentication.
     */
    @Override
    public void onAuthenticationSuccessful(@NotNull AuthenticationSuccessfulInput authenticationSuccessfulInput) {
        LOGGER.info(FINAL_CONNECT_MESSAGE, authenticationSuccessfulInput.getClientInformation().getClientId());
    }

    /**
     * Sends a message to Kafka.
     *
     * @param clientId the client ID
     * @param message the message to send
     * @param topic the Kafka topic to send the message to
     */
    private void sendMessageToKafka(String clientId, JSONObject message, String topic) {
        hivemqSinkService.sendMsgToSink(clientId, message.toJSONString().getBytes(StandardCharsets.UTF_8), topic);
    }

    /**
     * Handles the disconnect event for a client.
     *
     * @param disconnectEventInput The input containing information about the disconnect event.
     */
    @Override
    public void onDisconnect(@NotNull DisconnectEventInput disconnectEventInput) {
        LOGGER.debug("Client disconnect request for clientId: {}",
                disconnectEventInput.getClientInformation().getClientId());
        if (HivemqUtils.isHealthCheckUser(PropertyLoader.getProperties(),
                disconnectEventInput.getConnectionInformation())) {
            LOGGER.debug("Health check user: {} is disconnected",
                    disconnectEventInput.getClientInformation().getClientId());
            return;
        }
        extensionExecutorService.schedule(() -> {
            try {
                final Optional<String> usernameOptional = disconnectEventInput.getConnectionInformation()
                        .getConnectionAttributeStore().getAsString(AuthConstants.USERNAME);
                String userName = usernameOptional.isPresent() ? usernameOptional.get() : StringUtils.EMPTY;

                String clientId = disconnectEventInput.getClientInformation().getClientId();
                LOGGER.info("Disconnect {} with userName: {}, and Reason Code {}", clientId, userName,
                        disconnectEventInput.getReasonCode());

                doDisconnect(disconnectEventInput);
                LOGGER.info("Disconnected {} with userName: {}", clientId, userName);
            } catch (Exception ex) {
                LOGGER.error("Error encountered while disconnecting client:{}",
                        disconnectEventInput.getClientInformation().getClientId(), ex);
            }
        }, THREAD_DELAY, TimeUnit.MILLISECONDS);
    }

    /**
     * This method manages disconnect flow of a client. It cleanup local cache and redis entries for client.
     *
     * @param disconnectEventInput - Provides client details
     */
    public void doDisconnect(final DisconnectEventInput disconnectEventInput) {
        String clientId = disconnectEventInput.getClientInformation().getClientId();
        String userName = disconnectEventInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME).orElse(StringUtils.EMPTY);
        //if cache is empty then no need to call clear cache
        //if cache is there -> 
        Optional<DisconnectedReasonCode> disconnectReasonCode = disconnectEventInput.getReasonCode();
        if ((disconnectReasonCode.isEmpty()
                || (DisconnectedReasonCode.SESSION_TAKEN_OVER != disconnectReasonCode.get())) 
                && !isConnected(clientId)) {
            TokenExpiryHandler.remove(clientId, userName);
            AbstractSubscribeInboundInterceptor.clearClientPermissions(clientId);
            updateDeviceStatus(disconnectEventInput);
            HivemqUtils.clearCache(clientId, true);
            return;
        }
        
        //don't send kafka inactive message and don't cleanup redis in case of session_taken_over on same hivemq pod
        //if new connection is on different pod then cleanup local cache
        synchronized (DeviceSubscription.class) {
            DeviceSubscription updatedDeviceSubscription = deviceSubscriptionCache.getSubscription(clientId);
            if (updatedDeviceSubscription != null) {
                if (updatedDeviceSubscription.getConnectionsInfoCounter() == AuthConstants.SINGLE_CONNECTION) {
                    TokenExpiryHandler.remove(clientId, userName);
                    AbstractSubscribeInboundInterceptor.clearClientPermissions(clientId);
                    HivemqUtils.clearCache(clientId, false);
                } else {
                    LOGGER.info(
                            "A new connection with the clientId: {} already in progress with connectionInfoCounter: {},"
                            + " not removing the cache.",
                            clientId, updatedDeviceSubscription.getConnectionsInfoCounter());
                    updatedDeviceSubscription.decrConnectionsInfoCounter();
                }
            } else {
                LOGGER.info("A new connection request for clientId: {} failed before preparing local cache.", clientId);
            }
        }
    }

    /**
     * Updates the device status when a client gets disconnected.
     *
     * @param disconnectEventInput The input containing information about the disconnect event.
     */
    private void updateDeviceStatus(final DisconnectEventInput disconnectEventInput) {
        final Optional<String> usernameOptional = disconnectEventInput.getConnectionInformation()
                .getConnectionAttributeStore().getAsString(AuthConstants.USERNAME);
        String userName = usernameOptional.isPresent() ? usernameOptional.get() : StringUtils.EMPTY;
        String clientId = disconnectEventInput.getClientInformation().getClientId();

        LOGGER.info("Client with clientId: {} and user: {} got disconnected, and Reason Code {} ."
                + "Update device status in Kafka. ", clientId, userName, disconnectEventInput.getReasonCode());

        try {
            DeviceSubscription subscriptionCache = deviceSubscriptionCache.getSubscription(clientId);
            if (subscriptionCache != null && subscriptionCache.isSsdpVehicle()) {
                // Disconnect ssdp client JSONObject
                JSONObject jsonEvt = createSsdpDisconnectEvent(clientId, subscriptionCache);
                sendMessageToKafka(clientId, jsonEvt, externalPresenceTopic);
            } else {
                Optional<DisconnectedReasonCode> reasonCode = disconnectEventInput.getReasonCode();
                boolean abruptDisconnect = false;
                if (reasonCode.isPresent()) {
                    abruptDisconnect = isAbruptDisconnect(reasonCode.get());
                }
                JSONObject jsonEvt = HivemqUtils.createEvent(clientId,
                        abruptDisconnect ? EventMetadataConstants.OFFLINE_ABRUPT : EventMetadataConstants.OFFLINE);
                LOGGER.debug("Disconnect Event for user {} : {}", userName, jsonEvt.toString());
                sendMessageToKafka(clientId, jsonEvt, topicMapper.getDisconnectTopic());
            }
        } catch (Exception e) {
            LOGGER.error("Error while sending message to sink for clientId: " + clientId, e);
        }
    }

    /**
     * Checks if the disconnect reason code indicates an abrupt disconnect.
     *
     * @param reasonCode the reason code for the disconnect
     * @return true if the disconnect is abrupt, false otherwise
     */
    private boolean isAbruptDisconnect(DisconnectedReasonCode reasonCode) {
        return reasonCode.equals(DisconnectedReasonCode.UNSPECIFIED_ERROR);
    }

    /**
     * Checks if a client with the specified ID is connected.
     *
     * @param clientId the ID of the client to check
     * @return true if the client is connected, false otherwise
     */
    private boolean isConnected(String clientId) {
        boolean isConnected = false;
        try {
            isConnected = Services.clientService().isClientConnected(clientId).get();
            LOGGER.debug("Connection status for {} - {}", clientId, isConnected);
        } catch (Exception e) {
            LOGGER.warn("Exception while getting connection status for {}", clientId);
        }
        return isConnected;
    }

    /**
     * This method creates ssdp disconnect event that is to be sent to kafka.
     *
     * @param uin - client id
     * @param deviceSubscription - device subscription cache
     * @return - event json message
     */
    @SuppressWarnings("unchecked")
    public JSONObject createSsdpDisconnectEvent(String uin, DeviceSubscription deviceSubscription) {
        JSONObject eventObj = new JSONObject();
        eventObj.put(SimulatorEventMetadataConstants.UIN, uin);
        eventObj.put(SimulatorEventMetadataConstants.STATE, SimulatorEventMetadataConstants.OFFLINE);

        String parsedDate = LocalDateTime.now().format(FORMATTER);
        eventObj.put(SimulatorEventMetadataConstants.UPDATED, parsedDate);

        JSONArray serviceArray = new JSONArray();
        JSONObject serviceObj = new JSONObject();
        for (String topic : deviceSubscription.getSubscriptionTopics()) {
            JSONObject subscriptionStatusObj = new JSONObject();
            subscriptionStatusObj.put(SimulatorEventMetadataConstants.STATE, SimulatorEventMetadataConstants.OFFLINE);
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
}
