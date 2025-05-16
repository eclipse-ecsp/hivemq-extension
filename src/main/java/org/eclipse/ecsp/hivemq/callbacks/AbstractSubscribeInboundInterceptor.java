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
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.auth.SubscriptionAuthorizer;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerInput;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerOutput;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.packets.subscribe.Subscription;
import com.hivemq.extension.sdk.api.services.ManagedExtensionExecutorService;
import com.hivemq.extension.sdk.api.services.Services;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.auth.authorization.Authorizer;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.cache.TokenExpiryHandler;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.hivemq.utils.TopicFormatter;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * This class intercepts all subscribe requests form clients and authorize that request.
 */
@Component
public abstract class AbstractSubscribeInboundInterceptor implements SubscriptionAuthorizer {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(AbstractSubscribeInboundInterceptor.class);
    private static volatile Map<String, ModifiableDefaultPermissions> permissions 
        = new ConcurrentHashMap<>();

    private static Long timeout = Long
            .parseLong(PropertyLoader.getValue(ApplicationConstants.PUBLISH_SUBSCRIBE_ASYNC_TIMEOUT, "10"));
    private final Timer subscribeTimer;
    private static final boolean REPOPULATE_PERMISSION = Boolean
            .parseBoolean(PropertyLoader.getValue(ApplicationConstants.REPOPULATE_DEFAULT_PERMISSION, "false"));

    /**
     * This class represents an abstract base class for subscribe inbound interceptors.
     * It provides a timer for measuring the time taken for subscribe operations.
     */
    protected AbstractSubscribeInboundInterceptor() {
        MetricRegistry metricRegistry = Services.metricRegistry();
        this.subscribeTimer = metricRegistry.timer(ApplicationConstants.SUBSCRIBE_JMX_METRICS);
    }

    /**
     * Authorizes a subscribe request from a client.
     *
     * @param input  The input containing information about the subscribe request.
     * @param output The output to be sent back to the client after authorization.
     */
    @Override
    public void authorizeSubscribe(@NotNull final SubscriptionAuthorizerInput input,
            @NotNull final SubscriptionAuthorizerOutput output) {
        final ManagedExtensionExecutorService extensionExecutorService = Services.extensionExecutorService();
        final Async<SubscriptionAuthorizerOutput> async = output.async(Duration.ofSeconds(timeout));
        extensionExecutorService.submit(() -> {
            long start = System.currentTimeMillis();
            String clientId = input.getClientInformation().getClientId();
            try {
                String username = input.getConnectionInformation().getConnectionAttributeStore()
                        .getAsString(AuthConstants.USERNAME).orElse(StringUtils.EMPTY);
                Subscription subscription = input.getSubscription();
                String subscribingTopic = subscription.getTopicFilter();
                LOGGER.info("Subscribe request clientId: {}, username: {}, topic: {}", 
                        clientId, username, subscribingTopic);
    
                ModifiableDefaultPermissions defaultPermissions = permissions.get(clientId);
                if (defaultPermissions == null) {
                    Services.clientService().disconnectClient(clientId, false,
                            DisconnectReasonCode.IMPLEMENTATION_SPECIFIC_ERROR, "Default Permission Not Available");
                    return;
                }
                if (REPOPULATE_PERMISSION && !HivemqUtils.isWhiteListedUser(username)) {
                    // If default permission list is empty or wrong, reassign permissions
                    repopulateDefaultPermission(defaultPermissions, username, clientId);
                }
    
                List<TopicPermission> topicPermissions = defaultPermissions.asList().stream()
                        .filter(topicPermission -> validateTopic(topicPermission.getTopicFilter(), subscribingTopic)
                                && (topicPermission.getActivity().equals(TopicPermission.MqttActivity.ALL)
                                        || topicPermission.getActivity()
                                                .equals(TopicPermission.MqttActivity.SUBSCRIBE))
                                && topicPermission.getType().equals(TopicPermission.PermissionType.ALLOW))
                        .toList();
    
                if (topicPermissions.isEmpty() && !HivemqUtils.isWhiteListedUser(username)) {
                    // HiveMQ will not fail authorization or disconnect the device, it will log a
                    // warn and ignore the subscription
                    LOGGER.warn("Subscribe failed, clientId: {}, unauthorized topic {}",
                            clientId, subscribingTopic);
                    output.authorizeSuccessfully();
                } else {
                    LOGGER.info("Auth for subscribe successful, clientId: {}, topic: {}", clientId,
                            subscribingTopic);
                    output.authorizeSuccessfully();
                    TokenExpiryHandler.validateTokenExpiration(clientId, username);
                    doSubscribe(clientId, subscription);
                }
            } catch (Exception ex) {
                LOGGER.error("Error in subscribe, clientId {} -> ", clientId, ex);
            } finally {
                subscribeTimer.update(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
                async.resume();
            }
        });
    }

    /**
     * Repopulates the default permissions for a given user and client ID.
     * If the default permission list is empty or improper, this method retrieves the proper permissions
     * from the authorizer and updates the default permissions accordingly.
     *
     * @param defaultPermissions The modifiable default permissions object to be updated.
     * @param username The username of the user.
     * @param clientId The client ID of the client.
     */
    private void repopulateDefaultPermission(ModifiableDefaultPermissions defaultPermissions, String username,
            String clientId) {
        String permittedTopic = !defaultPermissions.asList().isEmpty()
                ? defaultPermissions.asList().get(0).getTopicFilter()
                : StringUtils.EMPTY;
        boolean isProperPermission = permittedTopic.contains(clientId) || permittedTopic.contains(username);
        if (!isProperPermission) {
            LOGGER.warn("Default permission list is improper or empty in subscribe for clientId: {}, "
                    + "repopulating permissions", clientId);
            List<TopicPermission> mqttTopicPermissions;
            try {
                Authorizer authorizer = new Authorizer();
                TopicFormatter topicFormatter = (TopicFormatter) getInstance(ApplicationConstants.TOPIC_FORMATTER_CLASS,
                        false).orElseThrow();
                authorizer.setTopicFormatter(topicFormatter);
                mqttTopicPermissions = authorizer.getDevicePermission(username, clientId, true);

                defaultPermissions.clear();
                defaultPermissions.addAll(mqttTopicPermissions);
                permissions.put(clientId, defaultPermissions);
                StringBuilder topicPermissions = new StringBuilder();
                if (mqttTopicPermissions != null) {
                    mqttTopicPermissions.forEach(mqttTopicPermission -> topicPermissions
                            .append(mqttTopicPermission.getTopicFilter()).append(StringUtils.SPACE));
                }
                LOGGER.info("clientId: {}, repopulat permissions: {}", clientId, topicPermissions);
            } catch (Exception ex) {
                LOGGER.info("exception while repopulating subscribe permissions for clientId: {}", clientId, ex);
            }
        }
    }

    /**
     * Sets the client context permissions for a given client ID.
     *
     * @param clientId The ID of the client.
     * @param defaultPermissions The default permissions to be set for the client context.
     */
    public static void setClientContextPermissions(String clientId, ModifiableDefaultPermissions defaultPermissions) {
        permissions.putIfAbsent(clientId, defaultPermissions);
    }

    /**
     * This method removes default tpoic permissions from local cache.
     *
     * @param clientId - client id
     */
    public static void clearClientPermissions(String clientId) {
        if (clientId == null || clientId.isEmpty()) {
            return;
        }
        if (permissions.containsKey(clientId)) {
            permissions.remove(clientId);
        }
    }

    /**
     * Performs the actual subscription for a given client and subscription.
     *
     * @param clientId     the ID of the client
     * @param subscription the subscription to be performed
     */
    protected abstract void doSubscribe(String clientId, Subscription subscription);

    /**
     * This method validates topic name and in case of # - all permission, it replace it with regex.
     *
     * @param topicPermission - topic from default permission list
     * @param topic - topic on which client is trying to subscribe
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
     * @param optional        flag indicating whether the class is optional or not
     * @return an Optional containing the instance of the class, or an empty Optional if the class 
     *      is optional and not available
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
