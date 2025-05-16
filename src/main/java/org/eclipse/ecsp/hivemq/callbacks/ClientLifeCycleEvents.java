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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
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
import org.eclipse.ecsp.hivemq.config.ScheduledThreadPoolFactory;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.routing.TopicMapperFactory;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.eclipse.ecsp.hivemq.utils.EventMetadataConstants;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.json.simple.JSONObject;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
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
public class ClientLifeCycleEvents implements ClientLifecycleEventListener {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ClientLifeCycleEvents.class);
    private static final String CONNECT_LOG_MESSAGE = "clientId: {} is trying to connect, protocolVersion: {}, "
            + "port: {}, user: {}, keepAliveTime: {}, cleanSession: {}, willQos: {}, isWill: {}, isWillretain: {}, "
            + "contains topics: {} of LWT message: {}";
    private static final String CONNECT_LOG_MESSAGE_WO_WILL = "clientId: {} "
            + "trying to connect, protocolVersion: {}, port: {}, user: {}, "
            + "keepAliveTime: {}, cleanSession: {}, isWill: {}";
    private static final String FINAL_CONNECT_MESSAGE = "clientId: {} is connected successfully";
    private HivemqSinkService hivemqSinkService;
    private TopicMapper topicMapper;
    private DeviceSubscriptionCache deviceSubscriptionCache;

    private static volatile AnnotationConfigApplicationContext applicationContext;
    private static ScheduledExecutorService extensionExecutorService;
    private static final Long THREAD_DELAY = Long
            .parseLong(PropertyLoader.getValue(ApplicationConstants.THREAD_DELAY, "200"));
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
    public ClientLifeCycleEvents() {
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
        final MetricRegistry metricRegistry = Services.metricRegistry();
        final Timer.Context timerContext = metricRegistry.timer(ApplicationConstants.MQTT_START_TIMER_JMX).time();
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
        timerContext.stop();
    }

    /**
     * Retrieves the port number of the listener associated with the given connection information.
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
     * Checks if the given connection is from a health check user.
     *
     * @param connectionStartInput the input containing information about the connection start
     * @return true if the connection is from a health check user, false otherwise
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

    @Override
    public void onAuthenticationSuccessful(@NotNull AuthenticationSuccessfulInput authenticationSuccessfulInput) {
        LOGGER.info(FINAL_CONNECT_MESSAGE, authenticationSuccessfulInput.getClientInformation().getClientId());
    }

    /**
     * This method is called when a client disconnects from the server.
     *
     * @param disconnectEventInput The input object containing information about the disconnect event.
     */
    @Override
    public void onDisconnect(@NotNull DisconnectEventInput disconnectEventInput) {
        final MetricRegistry metricRegistry = Services.metricRegistry();
        final Timer.Context timerContext = metricRegistry.timer(ApplicationConstants.DISCONNECT_TIMER_JMX).time();
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

                doDisconnect(disconnectEventInput);
                LOGGER.info("Disconnect {} with userName: {}, and Reason Code {}", clientId, userName,
                        disconnectEventInput.getReasonCode());
            } catch (Exception ex) {
                LOGGER.error("Error encountered while disconnecting client:{}",
                        disconnectEventInput.getClientInformation().getClientId(), ex);
            } 
            timerContext.stop();
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
        if (disconnectReasonCode.isPresent() 
                && DisconnectedReasonCode.SESSION_TAKEN_OVER == disconnectReasonCode.get()) {
            synchronized (DeviceSubscription.class) {
                DeviceSubscription updatedDeviceSubscription = deviceSubscriptionCache.getSubscription(clientId);
                if (updatedDeviceSubscription != null) {
                    if (updatedDeviceSubscription.getConnectionsInfoCounter() == AuthConstants.SINGLE_CONNECTION) {
                        TokenExpiryHandler.remove(clientId, userName);
                        AbstractSubscribeInboundInterceptor.clearClientPermissions(clientId);
                        HivemqUtils.clearCache(clientId, false);
                    } else {
                        LOGGER.info(
                                "A new connection with the clientId: {} already in progress with "
                                + "connectionInfoCounter: {}, not removing the cache.",
                                clientId, updatedDeviceSubscription.getConnectionsInfoCounter());
                        updatedDeviceSubscription.decrConnectionsInfoCounter();
                    }
                } else {
                    LOGGER.info("A new connection request for clientId: {} failed before preparing local cache.",
                            clientId);
                }
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

        LOGGER.info("clientId: {} and user: {} disconnected, Reason Code {}",
                clientId, userName, disconnectEventInput.getReasonCode());

        try {
            Optional<DisconnectedReasonCode> reasonCode = disconnectEventInput.getReasonCode();
            boolean abruptDisconnect = false;
            if (reasonCode.isPresent()) {
                if (DisconnectedReasonCode.BAD_USER_NAME_OR_PASSWORD == reasonCode.get()) {
                    return;
                }
                abruptDisconnect = isAbruptDisconnect(reasonCode.get());
            }
            JSONObject jsonEvt = HivemqUtils.createEvent(clientId,
                    abruptDisconnect ? EventMetadataConstants.OFFLINE_ABRUPT : EventMetadataConstants.OFFLINE);
            LOGGER.debug("Disconnect Event for user {} : {}", userName, jsonEvt.toString());
            hivemqSinkService.sendMsgToSink(clientId, jsonEvt.toJSONString().getBytes(StandardCharsets.UTF_8),
                    topicMapper.getDisconnectTopic());
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
     * Checks if a client with the given ID is connected.
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

}
