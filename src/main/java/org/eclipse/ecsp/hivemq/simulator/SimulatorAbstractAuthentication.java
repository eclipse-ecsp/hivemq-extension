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

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.auth.parameter.ModifiableClientSettings;
import com.hivemq.extension.sdk.api.auth.parameter.OverloadProtectionThrottlingLevel;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import com.hivemq.extension.sdk.api.services.Services;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.analytics.stream.base.utils.ObjectUtils;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.base.IgniteAuthenticationCallback;
import org.eclipse.ecsp.hivemq.base.TopicMapper;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCache;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
import org.eclipse.ecsp.hivemq.cache.IgniteAuthInfo;
import org.eclipse.ecsp.hivemq.cache.TokenExpiryHandler;
import org.eclipse.ecsp.hivemq.d2v.DeviceToVehicleMapper;
import org.eclipse.ecsp.hivemq.d2v.DeviceToVehicleMapperFactory;
import org.eclipse.ecsp.hivemq.d2v.VehicleInfo;
import org.eclipse.ecsp.hivemq.exceptions.VehicleDetailsNotFoundException;
import org.eclipse.ecsp.hivemq.exceptions.VehicleProfileResponseNotFoundException;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.routing.TopicMapperFactory;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.eclipse.ecsp.hivemq.utils.EventMetadataConstants;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.PROFILE_CHECK_DISABLED_TOPICS;

/**
 * This is an abstract class which provides method to authenticate a client and then custom logic to fetch vehicle
 * details and prepare local cache with that data.
 */
@Component
public abstract class SimulatorAbstractAuthentication implements IgniteAuthenticationCallback {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(SimulatorAbstractAuthentication.class);
    // service.client.overloadprotection.disabled
    private static boolean disableServiceClientOverloadProtection = Boolean.parseBoolean(
            PropertyLoader.getValue(ApplicationConstants.HIVEMQ_SERVICE_CLIENT_OVERLOADPROTECTION_DISABLED, "false"));
    private HivemqSinkService hivemqSinkService;
    private TopicMapper topicMapper;
    private DeviceToVehicleMapper deviceToVehicleMapper;
    private DeviceSubscriptionCache deviceSubscriptionCache;
    // To change session persistence level
    private String externalPresenceTopic = PropertyLoader
            .getValue(ApplicationConstants.KAFKA_SINK_TOPIC_PRESENCE_MANAGER);
    private static Long timeout = Long
            .parseLong(PropertyLoader.getValue(ApplicationConstants.PUBLISH_SUBSCRIBE_ASYNC_TIMEOUT, "10"));
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private static final String DEVICE_PLATFORM = PropertyLoader.getValue(ApplicationConstants.DEVICE_PLATFORM, "ssdp");
    private static final String AUTHENTICATION_FAILED = "Authentication failed!!!";
    private static final String CONNECT_REQUEST_FAILED_LOG
        = "Connection request failed for clientid: {} and reason: {}";

    /**
     * Constructor to load required properties on class loading time.
     */
    protected SimulatorAbstractAuthentication() {
        hivemqSinkService = HivemqSinkService.getInstance();
        ObjectUtils.requireNonNull(hivemqSinkService, "Unable to initialize the hive sink");
        topicMapper = TopicMapperFactory.getInstance();
        ObjectUtils.requireNonNull(topicMapper, "Unable to initialize the topic mapper");
        deviceToVehicleMapper = DeviceToVehicleMapperFactory.getInstance();
        deviceToVehicleMapper.init(PropertyLoader.getProperties());
        ObjectUtils.requireNonNull(deviceToVehicleMapper, "Unable to initialize the deviceToVehicleMapper");
        deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
    }

    /**
     * This method is called when a client attempts to connect to the server.
     * It performs the authentication process for the client.
     *
     * @param simpleAuthInput  The input containing the client's authentication information.
     * @param simpleAuthOutput The output used to communicate the authentication result to the client.
     */
    @Override
    public void onConnect(@NotNull SimpleAuthInput simpleAuthInput, @NotNull SimpleAuthOutput simpleAuthOutput) {
        final MetricRegistry metricRegistry = Services.metricRegistry();
        final Timer.Context timerContext = metricRegistry.timer(ApplicationConstants.ONCONNECT_TIMER_JMX).time();
        // make output async with timeout of 10 seconds and when operation timed out, auth will fail
        final Async<SimpleAuthOutput> asyncOutput = simpleAuthOutput.async(Duration.ofSeconds(timeout),
                TimeoutFallback.FAILURE);

        if (HivemqUtils.isHealthCheckUser(PropertyLoader.getProperties(), simpleAuthInput.getConnectionInformation())) {
            LOGGER.debug("Authentication successful for health check user.");
            asyncOutput.getOutput().authenticateSuccessfully();
            asyncOutput.resume();
            timerContext.stop();
        } else {
            authenticateClient(simpleAuthInput, simpleAuthOutput, asyncOutput, timerContext);
        }
    }

    /**
     * Disables overload protection for internal service clients.
     *
     * @param username           The username of the client.
     * @param clientId           The client id of the client.
     * @param simpleAuthInput    The input object containing the authentication information.
     * @param simpleAuthOutput   The output object for communicating the authentication result.
     */
    private void disableOverloadProtection(String username, String clientId, SimpleAuthInput simpleAuthInput,
            SimpleAuthOutput simpleAuthOutput) {
        if (disableServiceClientOverloadProtection
                && HivemqUtils.isInternalServiceClient(username, simpleAuthInput.getConnectionInformation())) {
            LOGGER.warn("Disabling overload protection for clientId {} ", clientId);
            ModifiableClientSettings clientSettings = simpleAuthOutput.getClientSettings();
            clientSettings.setOverloadProtectionThrottlingLevel(OverloadProtectionThrottlingLevel.NONE);

        }
    }

    /**
     * Authenticates the client using the provided authentication input and output objects.
     * This method submits an external task to the managed extension executor service for authentication.
     *
     * @param simpleAuthInput  The input object containing the authentication information.
     * @param simpleAuthOutput The output object for returning the authentication result.
     * @param asyncOutput      The asynchronous output object for handling the authentication process.
     */
    private void authenticateClient(@NotNull SimpleAuthInput simpleAuthInput,
            @NotNull SimpleAuthOutput simpleAuthOutput, Async<SimpleAuthOutput> asyncOutput,
            Timer.Context timerContext) {
        ConnectionInformation connect = simpleAuthInput.getConnectionInformation();
        String clientId = simpleAuthInput.getClientInformation().getClientId();
        try {
            LOGGER.debug("Client data: {}", connect);

            Optional<IgniteAuthInfo> iaInfoOp = authenticate(simpleAuthInput);
            boolean isAuthenticated = iaInfoOp.isPresent() && iaInfoOp.get().isAuthenticated();
            LOGGER.info("Authentication Status:- clientId: {}, isAuthenticated: {}", clientId,
                    isAuthenticated);

            if (isAuthenticated) {
                LOGGER.debug("Adding to expiry cache: clientId: {}, exp time {} ", clientId,
                        iaInfoOp.get().getExp());
                String username = simpleAuthInput.getConnectPacket().getUserName().orElse(StringUtils.EMPTY);

                disableOverloadProtection(username, clientId, simpleAuthInput, simpleAuthOutput);
                TokenExpiryHandler.put(clientId, iaInfoOp.get().getExp(), username);
                afterAuthSuccessful(simpleAuthInput)
                    .thenRun(() -> asyncOutput.getOutput().authenticateSuccessfully())
                    .whenComplete((unused, throwable) -> {
                        if (throwable != null) {
                            if (throwable instanceof CompletionException) {
                                LOGGER.error(CONNECT_REQUEST_FAILED_LOG,
                                        clientId, throwable.getCause());
                                asyncOutput.getOutput().failAuthentication(ConnackReasonCode.NOT_AUTHORIZED,
                                        AUTHENTICATION_FAILED);
                            } else {
                                LOGGER.error(CONNECT_REQUEST_FAILED_LOG,
                                        clientId, throwable);
                                asyncOutput.getOutput().failAuthentication(ConnackReasonCode.NOT_AUTHORIZED,
                                        AUTHENTICATION_FAILED);
                            }
                        }
                        //resume output to tell HiveMQ auth is complete
                        asyncOutput.resume();
                        timerContext.stop();
                    });
            } else {
                asyncOutput.getOutput().failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                        AUTHENTICATION_FAILED);
                asyncOutput.resume();
                timerContext.stop();
            }

        } catch (Exception e) {
            LOGGER.error(CONNECT_REQUEST_FAILED_LOG, clientId, e);
            asyncOutput.getOutput().failAuthentication(ConnackReasonCode.NOT_AUTHORIZED,
                    AUTHENTICATION_FAILED);
            asyncOutput.resume();
            timerContext.stop();
        }
    }

    /**
     * Performs actions after a successful authentication.
     *
     * @param authenticationSuccessfulInput The input containing information about the successful authentication.
     * @throws VehicleProfileResponseNotFoundException If the vehicle profile response is not found.
     * @throws IOException If an I/O error occurs.
     * @throws VehicleDetailsNotFoundException If the vehicle details are not found.
     */
    private CompletableFuture<Void> afterAuthSuccessful(SimpleAuthInput authenticationSuccessfulInput) {
        String clientId = authenticationSuccessfulInput.getClientInformation().getClientId();
        final Optional<String> usernameOptional = authenticationSuccessfulInput.getConnectionInformation()
                .getConnectionAttributeStore().getAsString(AuthConstants.USERNAME);
        String userName = usernameOptional.isPresent() ? usernameOptional.get() : StringUtils.EMPTY;

        if (HivemqUtils.isWhiteListedUser(userName)) {
            // For white listed user, no need to get the vehicle Id from Vehicle
            // profile service, use deviceId as VehicleId
            DeviceSubscription deviceSubscriptionWhitelistUser = new DeviceSubscription(clientId);
            deviceSubscriptionWhitelistUser.setSsdpVehicle(false);
            deviceSubscriptionCache.addSubscription(clientId, deviceSubscriptionWhitelistUser);
            JSONObject jsonEvt = HivemqUtils.createEvent(clientId, EventMetadataConstants.ONLINE);
            sendMessageToKafka(clientId, jsonEvt, topicMapper.getConnectTopic());
        } else {
            DeviceSubscription deviceSubscription = deviceSubscriptionCache.getSubscription(clientId);
            /*
             * To avoid race condition while new connection request made, when device is
             * still connected case 1: connect request came to same instance where it was
             * connected - any point only one connection can be
             * active , so there would be a disconnect callback - based on counter - removal
             * of local cache - should not remove - based on connection status on cluster-
             * removal of redis cache - should not remove
             * 
             * if disconnect processed earlier, local cache recreated in connect flow if
             * connect processed earlier, local cache delete is ignored based on counter
             * 
             * case 2: connect request came to different instance than it was connected
             * eaarlier - any point only one connection can be active , so there would be a
             * disconnect callback - verify if disconnect reaches same instance where
             * it was connected - based on counter - removal of local cache - should remove
             * from earlier connected instance - cause of memory lick - based on connection
             * status on cluster- removal of redis cache - should not remove
             * 
             */
            if (deviceSubscription != null) {
                synchronized (deviceSubscription) {
                    deviceSubscription.incrConnectionsInfoCounter();
                    LOGGER.info(
                            "Received new connection while a connection is already active for clientId: {},"
                                    + " ConnectionsInfoCounter: {}",
                            clientId, deviceSubscription.getConnectionsInfoCounter());
                }
            } else {
                return doProcess(clientId,
                        authenticationSuccessfulInput.getConnectionInformation().getConnectionAttributeStore());
            }
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Sends a message to Kafka.
     *
     * @param clientId the client ID
     * @param message the message to send
     * @param topic the topic to send the message to
     */
    private void sendMessageToKafka(String clientId, JSONObject message, String topic) {
        hivemqSinkService.sendMsgToSink(clientId, message.toJSONString().getBytes(StandardCharsets.UTF_8), topic);
    }

    /**
     * Performs the processing for a given client ID, device subscription, and connection attribute store.
     * This method retrieves the vehicle information for the client ID and performs various operations based on the
     * connected platform and device type. It also updates the device subscription cache accordingly.
     *
     * @param clientId          The client ID for which the processing is performed.
     * @param deviceSubscription The device subscription object associated with the client ID.
     *                           If null, a new device subscription will be created.
     * @param attributeStore    The connection attribute store containing the connection attributes for the client ID.
     */
    private CompletableFuture<Void> doProcess(final String clientId,
            ConnectionAttributeStore attributeStore) {
        return getVehicleInfo(clientId, attributeStore).thenAccept(vehicleInfo -> {
            String vehicleId = Objects.nonNull(vehicleInfo) ? vehicleInfo.getVehicleId() : null;
            Optional<String> deviceType = Objects.nonNull(vehicleInfo) && vehicleInfo.getDeviceType().isPresent()
                    ? vehicleInfo.getDeviceType()
                    : Optional.empty();
            LOGGER.info("Adding subscription cache for clientId: {} with vehicleId: {}, deviceType: {}", clientId,
                    vehicleId, deviceType);
            boolean isSsdpDevice = false;
            String connectedPlatform = attributeStore.getAsString(AuthConstants.CONNECTED_PLATFORM)
                    .orElse(StringUtils.EMPTY);
            if (connectedPlatform != null && !connectedPlatform.isEmpty()
                    && connectedPlatform.equals(DEVICE_PLATFORM)) {
                LOGGER.info("SSDP device connected with clientId: {}", clientId);
                isSsdpDevice = true;
                JSONObject jsonEvt = createSsdpConnectEvent(clientId);
                sendMessageToKafka(clientId, jsonEvt, externalPresenceTopic);
            } else {
                JSONObject jsonEvt = HivemqUtils.createEvent(clientId, EventMetadataConstants.ONLINE);
                sendMessageToKafka(clientId, jsonEvt, topicMapper.getConnectTopic());
            }
            DeviceSubscription deviceSubscription = new DeviceSubscription(vehicleId);
            deviceSubscription.setDeviceType(deviceType);
            deviceSubscription.setSsdpVehicle(isSsdpDevice);
            deviceSubscriptionCache.addSubscription(clientId, deviceSubscription);
        });
    }

    /**
     * Retrieves the vehicle information for a given client ID.
     *
     * @param clientId The client ID associated with the vehicle.
     * @param attributeStore The attribute store containing the connection attributes.
     * @return The vehicle information.
     */
    private CompletableFuture<VehicleInfo> getVehicleInfo(final String clientId,
            ConnectionAttributeStore attributeStore) {
        return deviceToVehicleMapper.getVehicleId(clientId, attributeStore).thenApply(vehicleInfo -> {
            if (Objects.isNull(vehicleInfo) || Objects.isNull(vehicleInfo.getVehicleId())) {
                LOGGER.warn("clientId: {} with no linked vehicleId tried to connect", clientId);
                String acceptableTopics = Optional.ofNullable(
                        PropertyLoader.getValue(PROFILE_CHECK_DISABLED_TOPICS)).orElse("");
                if (acceptableTopics.trim().isEmpty()) {
                    throw new VehicleDetailsNotFoundException(
                            "client does not have any linked vehicleId and profile_check_disabled_topics "
                            + "is not configured");
                }
            }
            return vehicleInfo;
        });
    }

    /**
     * Sets the device to vehicle mapper.
     *
     * @param deviceToVehicleMapper the device to vehicle mapper to be set
     */
    public void setDeviceToVehicleMapper(DeviceToVehicleMapper deviceToVehicleMapper) {
        this.deviceToVehicleMapper = deviceToVehicleMapper;
    }

    /**
     * This method creates ssdp json connect event to be sent to kafka.
     *
     * @param clientId - client id
     * @return ssdp connect even message
     */
    @SuppressWarnings("unchecked")
    public static JSONObject createSsdpConnectEvent(String clientId) {
        JSONObject eventObj = new JSONObject();
        eventObj.put(SimulatorEventMetadataConstants.UIN, clientId);
        eventObj.put(SimulatorEventMetadataConstants.STATE, SimulatorEventMetadataConstants.ONLINE);

        String parsedDate = LocalDateTime.now().format(FORMATTER);
        eventObj.put(SimulatorEventMetadataConstants.UPDATED, parsedDate);

        JSONObject serviceObj = null;
        eventObj.put(SimulatorEventMetadataConstants.SERVICES, serviceObj);
        return eventObj;
    }

    /**
     * Authenticates the given `simpleAuthInput` and returns an optional `IgniteAuthInfo` object.
     *
     * @param simpleAuthInput The `SimpleAuthInput` object containing the authentication input.
     * @return An optional `IgniteAuthInfo` object representing the authenticated user's information, 
     *      or an empty optional if authentication fails.
     */
    public abstract Optional<IgniteAuthInfo> authenticate(SimpleAuthInput simpleAuthInput);

}
