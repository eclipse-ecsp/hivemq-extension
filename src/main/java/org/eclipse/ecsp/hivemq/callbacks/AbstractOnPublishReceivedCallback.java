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

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.async.Async.Status;
import com.hivemq.extension.sdk.api.async.TimeoutFallback;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.client.ClientContext;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.publish.AckReasonCode;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extension.sdk.api.services.Services;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.auth.authorization.Authorizer;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.cache.TokenExpiryHandler;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.utils.HealthService;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.hivemq.utils.TopicFormatter;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Class to store the messages received on publish in Kafka/Kinesis.
 *
 * @author Neha Khan
 */
@Component
public abstract class AbstractOnPublishReceivedCallback implements PublishInboundInterceptor {
    private static final  @NotNull IgniteLogger LOGGER = IgniteLoggerFactory
            .getLogger(AbstractOnPublishReceivedCallback.class);
    private static final String RECEIVED_MESSAGE_LOG = "clientId: {} received message on Topic : {}, QOS {},"
            + " messageTime from device : {}, currentTime {}";
    private static final String RECEIVED_EMPTY_PAYLOAD = "Received empty payload for {} on Topic {}";
    private ClientContext clientContext;
    private final @NotNull Counter droppedMessageV2C;
    private final @NotNull Counter droppedMessageC2V;
    private static Long timeout = Long
            .parseLong(PropertyLoader.getValue(ApplicationConstants.PUBLISH_SUBSCRIBE_ASYNC_TIMEOUT, "10"));
    private static final boolean REPOPULATE_PERMISSION = Boolean
            .parseBoolean(PropertyLoader.getValue(ApplicationConstants.REPOPULATE_DEFAULT_PERMISSION, "false"));

    protected AbstractOnPublishReceivedCallback() {
        final MetricRegistry metricRegistry = Services.metricRegistry();
        this.droppedMessageV2C = metricRegistry
                .counter(ApplicationConstants.PUBLISH_DROPPED_TIMEOUT_COUNT_V2C_JMX_METRICS);
        this.droppedMessageC2V = metricRegistry
                .counter(ApplicationConstants.PUBLISH_DROPPED_TIMEOUT_COUNT_C2V_JMX_METRICS);
    }

    /**
     * Callback method invoked when an inbound publish packet is received.
     *
     * @param publishInboundInput  The input parameters for the inbound publish packet.
     * @param publishInboundOutput The output parameters for the inbound publish packet.
     */
    @Override
    public void onInboundPublish(
            final @NotNull PublishInboundInput publishInboundInput,
            final @NotNull PublishInboundOutput publishInboundOutput) {
        final MetricRegistry metricRegistry = Services.metricRegistry();
        final Timer.Context asyncTimerContext = metricRegistry.timer(ApplicationConstants.PUBLISH_TIMER_JMX).time();
        final Async<PublishInboundOutput> asyncOutput =
            publishInboundOutput.async(Duration.ofSeconds(timeout), TimeoutFallback.FAILURE);
        
        final Optional<String> usernameOptional = publishInboundInput.getConnectionInformation()
                .getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME);
        final String userName = usernameOptional.orElse(StringUtils.EMPTY);
        final String clientId = publishInboundInput.getClientInformation().getClientId();
        final String mqttTopic = publishInboundInput.getPublishPacket().getTopic();

        LOGGER.info(RECEIVED_MESSAGE_LOG, clientId, mqttTopic, 
                publishInboundInput.getPublishPacket().getQos(),
                publishInboundInput.getPublishPacket().getTimestamp(), 
                System.currentTimeMillis());

        final ModifiableDefaultPermissions defaultPermissions = clientContext.getDefaultPermissions();

        if (defaultPermissions == null) {
            Services.clientService().disconnectClient(clientId, false,
                    DisconnectReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, "Default Permission Not Available");
            return;
        }
        if (REPOPULATE_PERMISSION && !HivemqUtils.isWhiteListedUser(userName)) {
            repopulateDefaultPermission(defaultPermissions, userName, clientId);
        }
        final List<TopicPermission> topicPermissions = clientTopicPermission(publishInboundInput);

        if (topicPermissions.isEmpty() && !HivemqUtils.isWhiteListedUser(userName)) {
            LOGGER.error("Publish failed for clientId: {} unauthorized topics: {}", clientId, mqttTopic);
            publishInboundOutput.preventPublishDelivery();
            asyncOutput.resume();
            asyncTimerContext.stop();
        } else {
            publishAuthorizationSuccessful(publishInboundInput, publishInboundOutput, clientId, userName,
                    mqttTopic, asyncTimerContext, asyncOutput);
            asyncOutput.resume();
            asyncTimerContext.stop();
        }
    }

    /**
     * Repopulates the default permissions for a given user and client ID.
     * If the default permission list is empty or incorrect, this method reassigns the permissions.
     *
     * @param defaultPermissions The modifiable default permissions object.
     * @param userName The user name.
     * @param clientId The client ID.
     */
    private void repopulateDefaultPermission(ModifiableDefaultPermissions defaultPermissions, String userName,
            String clientId) {
        // If default permission list is empty or wrong, reassign permissions
        String permittedTopic = !defaultPermissions.asList().isEmpty()
                ? defaultPermissions.asList().get(0).getTopicFilter()
                : StringUtils.EMPTY;
        boolean isProperPermission = permittedTopic.contains(clientId) || permittedTopic.contains(userName);
        if (!isProperPermission) {
            LOGGER.warn("Default permission list is improper or empty in publish for clientId: {}, "
                    + "repopulating permissions", clientId);
            List<TopicPermission> mqttTopicPermissions;
            try {
                Authorizer authorizer = new Authorizer();
                TopicFormatter topicFormatter = (TopicFormatter) getInstance(ApplicationConstants.TOPIC_FORMATTER_CLASS,
                        false).orElseThrow();
                authorizer.setTopicFormatter(topicFormatter);
                mqttTopicPermissions = authorizer.getDevicePermission(userName, clientId, true);

                defaultPermissions.clear();
                defaultPermissions.addAll(mqttTopicPermissions);
                StringBuilder topicPermissions = new StringBuilder();
                if (mqttTopicPermissions != null) {
                    mqttTopicPermissions.forEach(mqttTopicPermission -> topicPermissions
                            .append(mqttTopicPermission.getTopicFilter()).append(StringUtils.SPACE));
                }
                LOGGER.info("clientId: {}, repopulat permissions: {}", clientId, topicPermissions);
            } catch (Exception ex) {
                LOGGER.error("exception while repopulating publish permissions for clientId: {}", clientId, ex);
            }
        }
    }

    /**
     * Handles the successful authorization of a publish request.
     *
     * @param publishInboundInput  The input parameters for the publish request.
     * @param publishInboundOutput The output parameters for the publish request.
     * @param clientId             The client ID associated with the publish request.
     * @param userName             The username associated with the publish request.
     * @param mqttTopic            The MQTT topic of the publish request.
     */
    private void publishAuthorizationSuccessful(PublishInboundInput publishInboundInput,
            PublishInboundOutput publishInboundOutput, String clientId, String userName, String mqttTopic,
            final Timer.Context asyncTimerContext, final Async<PublishInboundOutput> asyncOutput) {
        LOGGER.info("Auth for publish successful, clientId: {}, topic: {}", clientId,
                mqttTopic);
        TokenExpiryHandler.validateTokenExpiration(clientId, userName);
        if (HivemqUtils.isHealthCheckUser(PropertyLoader.getProperties(),
                publishInboundInput.getConnectionInformation())) {
            onReceivedHealthCheckMessage(userName, publishInboundOutput);
            return;
        }

        if (publishInboundInput.getPublishPacket().getPayload().isEmpty()) {
            LOGGER.warn(RECEIVED_EMPTY_PAYLOAD, clientId, mqttTopic);
            return;
        }
        ByteBuffer publishPayload = publishInboundInput.getPublishPacket().getPayload().orElse(null);
        LOGGER.debug("Publish request received for clientId: {} with payload {} on topic {} ", clientId,
                publishPayload, mqttTopic);

        final MetricRegistry metricRegistry = Services.metricRegistry();
        try (final Timer.Context ignored = metricRegistry.timer(ApplicationConstants.DOPUBLISH_RECEIVED_TIMER_JMX)
                .time()) {
            doPublishReceived(publishInboundInput, publishInboundOutput).whenComplete((object, throwable) -> {
                asyncTimerContext.stop();
                if (throwable != null) {
                    LOGGER.error("Error encountered in onInboundPublish: {}", throwable);
                }
                try {
                    if (Status.CANCELED == asyncOutput.getStatus()) {
                        LOGGER.error("publish failed due to timeout for client: {} on topic {}",
                                publishInboundInput.getClientInformation().getClientId(), 
                                publishInboundInput.getPublishPacket().getTopic());
                        String username = publishInboundInput.getConnectionInformation()
                                .getConnectionAttributeStore()
                                .getAsString(AuthConstants.USERNAME)
                                .orElse(StringUtils.EMPTY);
                        if (HivemqUtils.isInternalServiceClient(username, 
                                publishInboundInput.getConnectionInformation())) {
                            droppedMessageC2V.inc();
                        } else {
                            droppedMessageV2C.inc();
                        }
                    }
                } finally {
                    asyncOutput.resume();
                }
            });
        }
    }

    /**
     * Retrieves the list of topic permissions for the client based on the incoming publish packet.
     *
     * @param publishInboundInput The input containing the publish packet.
     * @return The list of topic permissions for the client.
     */
    private List<TopicPermission> clientTopicPermission(PublishInboundInput publishInboundInput) {
        final ModifiableDefaultPermissions defaultPermissions = clientContext.getDefaultPermissions();
        PublishPacket publishPacket = publishInboundInput.getPublishPacket();
        return defaultPermissions
                .asList().stream().filter(
                        topicPermission -> validateTopic(topicPermission.getTopicFilter(), publishPacket.getTopic())
                                && (topicPermission.getActivity().equals(TopicPermission.MqttActivity.ALL)
                                        || topicPermission.getActivity().equals(TopicPermission.MqttActivity.PUBLISH))
                                && topicPermission.getType().equals(TopicPermission.PermissionType.ALLOW)
                                && (topicPermission.getPublishRetain().equals(TopicPermission.Retain.ALL)
                                        || topicPermission.getPublishRetain()
                                                .equals(publishPacket.getRetain() ? TopicPermission.Retain.RETAINED
                                                        : TopicPermission.Retain.NOT_RETAINED)))
                .toList();
    }

    /**
     * Sets the client context for this callback.
     *
     * @param clientContext the client context to set
     */
    public void setClientContext(ClientContext clientContext) {
        this.clientContext = clientContext;
    }

    /**
     * This method is called when a publish message is received.
     * Implementations of this method should handle the received publish message.
     *
     * @param publishInboundInput  the input containing the details of the received publish message
     * @param publishInboundOutput the output for handling the received publish message
     */
    protected abstract CompletableFuture<Void> doPublishReceived(PublishInboundInput publishInboundInput,
            PublishInboundOutput publishInboundOutput);

    /**
     * Health check user's message processing.
     *
     * @param userName - username
     * @param publishInboundOutput - output parameter of publish interceptor,
     */
    void onReceivedHealthCheckMessage(String userName, PublishInboundOutput publishInboundOutput) {
        LOGGER.debug("Health check message received with user: {} ", userName);
        if (HealthService.getInstance().isHealthy()) {
            LOGGER.debug("HiveMQ is able to read/write from Redis. Passing health check.");
            return;
        }
        LOGGER.error("HiveMQ is not able to read/write from Redis. Failing health check.");
        publishInboundOutput.preventPublishDelivery(AckReasonCode.UNSPECIFIED_ERROR, "Health check failed!!!");
    }

    /**
     * This method validates topic name and in case of # - all permission, it replace it with regex.
     *
     * @param topicPermission - topic from default permission list
     * @param topic - topic on which client is trying to publish
     * @return true - if permission matches else false
     */
    public Boolean validateTopic(String topicPermission, String topic) {
        if (topic != null) {
            topicPermission = topicPermission.replace("+",
                    "[\\x00-\\xFF&&[^\\p{Cntrl}]&&[^\\r\\n\\t\\s/+]&&[\\p{Print}]]");
            topicPermission = topicPermission.replace("#",
                    "[\\x00-\\xFF&&[^\\p{Cntrl}/]&&[^\\r\\n\\t\\s+]&&[\\p{Print}]]+(/[\\x00-\\xFF&&[^\\p{Cntrl}/]"
                            + "&&[^\\r\\n\\t\\s+]&&[\\p{Print}]]+)*");
            return topic.matches(topicPermission);
        }
        return false;
    }

    /**
     * Retrieves an instance of a class specified by the given plugin class property.
     *
     * @param pluginClassProp the property name of the plugin class
     * @param optional        a flag indicating whether the class is optional or not
     * @return an Optional containing the instance of the class, or an empty Optional if the class is 
     *      optional and not available
     * @throws IllegalArgumentException if the specified class is not available on the classpath
     */
    private Optional<Object> getInstance(String pluginClassProp, boolean optional) {
        String className = StringUtils.EMPTY;
        try {
            className = PropertyLoader.getValue(pluginClassProp);
            if (optional && StringUtils.isEmpty(className)) {
                return Optional.empty();
            }
            return Optional.ofNullable(
                    getClass().getClassLoader().loadClass(className).getDeclaredConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException(
                    pluginClassProp + " refers to a class (" + className + ") that is not available on the classpath");
        }
    }

}
