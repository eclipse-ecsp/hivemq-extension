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

package org.eclipse.ecsp.hivemq.base;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.client.ClientContext;
import com.hivemq.extension.sdk.api.client.parameter.InitializerInput;
import com.hivemq.extension.sdk.api.packets.auth.DefaultAuthorizationBehaviour;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.services.Services;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.callbacks.AbstractSubscribeInboundInterceptor;
import org.eclipse.ecsp.hivemq.callbacks.MessageStoreCallback;
import org.eclipse.ecsp.hivemq.callbacks.OnPublishOutboundIntercept;
import org.eclipse.ecsp.hivemq.callbacks.PingServerRequest;
import org.eclipse.ecsp.hivemq.callbacks.PubackReceived;
import org.eclipse.ecsp.hivemq.callbacks.PubackSend;
import org.eclipse.ecsp.hivemq.callbacks.UnsubscribeInboundIntercept;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.utils.HivemqServiceProvider;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.hivemq.utils.TopicFormatter;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.stereotype.Component;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import static org.eclipse.ecsp.hivemq.utils.HivemqUtils.getPermission;

/**
 * This class provides default permissions to client after successful authentication.
 * It overrides hivemq provided initialize method so after authentication this class gets called automatically.
 */
@Component
public abstract class AbstractAuthorization implements IgniteAuthorizerExtension {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(AbstractAuthorization.class);
    protected TopicFormatter topicFormatter;
    private static final String HEALTH_CHECK_TOPIC = "health";
    private AnnotationConfigApplicationContext applicationContext;

    /**
     * Initializes the authorization process for the client.
     *
     * @param initializerInput The input data for the initializer.
     * @param clientContext The client context for the authorization process.
     */
    @Override
    public void initialize(@NotNull InitializerInput initializerInput, @NotNull ClientContext clientContext) {
        final MetricRegistry metricRegistry = Services.metricRegistry();
        try (final Timer.Context ignored = metricRegistry.timer(ApplicationConstants.AUTHORIZATION_TIMER_JMX).time()) {
            String clientId = initializerInput.getClientInformation().getClientId();
            LOGGER.debug("Default Authorization called for clientId: {}", clientId);
            HivemqServiceProvider.setBlockingClientService(Services.clientService());
            topicFormatter = (TopicFormatter) getInstance(ApplicationConstants.TOPIC_FORMATTER_CLASS, false)
                    .orElseThrow();
            MessageStoreCallback messageStoreCallback = (MessageStoreCallback) getInstance(
                    ApplicationConstants.PUBLISH_INTERCEPTOR, false).orElseThrow();
            messageStoreCallback.setTopicFormatter(topicFormatter);
            PingServerRequest pingReq = (PingServerRequest) applicationContext.getBean("pingServerRequest");
            pingReq.initializeClientService();
            PubackReceived pubackReceived = (PubackReceived) applicationContext.getBean("pubackReceived");
            clientContext.addPubackInboundInterceptor(pubackReceived);
            
            PubackSend pubackSend = (PubackSend) applicationContext.getBean("pubackSend");
            clientContext.addPubackOutboundInterceptor(pubackSend);
            clientContext.addPublishInboundInterceptor(messageStoreCallback);
            OnPublishOutboundIntercept onPublishOutboundIntercept = (OnPublishOutboundIntercept) applicationContext
                    .getBean("onPublishOutboundIntercept", Services.metricRegistry());
            clientContext.addPublishOutboundInterceptor(onPublishOutboundIntercept);
            UnsubscribeInboundIntercept unsubscribeInboundIntercept = (UnsubscribeInboundIntercept) applicationContext
                    .getBean("unsubscribeInboundIntercept");
            clientContext.addUnsubscribeInboundInterceptor(unsubscribeInboundIntercept);
            clientContext.addPingReqInboundInterceptor(pingReq);

            List<TopicPermission> permittedTopics = getPermissions(initializerInput);
            StringBuilder topicPermissions = new StringBuilder();
            if (permittedTopics != null) {
                permittedTopics.forEach(mqttTopicPermission -> topicPermissions
                        .append(mqttTopicPermission.getTopicFilter()).append(StringUtils.SPACE));
            }
            LOGGER.info("clientId: {} got default permissions on topic: {}", clientId, topicPermissions);

            // Get the default permissions from the clientContext
            ModifiableDefaultPermissions defaultPermissions = clientContext.getDefaultPermissions();
            defaultPermissions.addAll(permittedTopics);
            defaultPermissions.setDefaultBehaviour(DefaultAuthorizationBehaviour.ALLOW);

            messageStoreCallback.setClientContext(clientContext);
            AbstractSubscribeInboundInterceptor.setClientContextPermissions(
                    initializerInput.getClientInformation().getClientId(), defaultPermissions);

        } catch (Exception exception) {
            String clientId = initializerInput.getClientInformation().getClientId();
            LOGGER.error("Disconnecting client {} exception encountered at clientInitialization {}", clientId,
                    exception);
            HivemqServiceProvider.getBlockingClientService().disconnectClient(clientId);
        }
    }

    /**
     * Sets the application context for this object.
     *
     * @param applicationContext the application context to be set
     */
    @Override
    public void setApplicationContext(AnnotationConfigApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * This method prepares and returns List of default mqtt topic permission to client.
     *
     * @param initializerInput - provides client details
     * @return List of TopicPermission
     */
    public List<TopicPermission> getPermissions(InitializerInput initializerInput) {
        final MetricRegistry metricRegistry = Services.metricRegistry();
        List<TopicPermission> permittedTopics;
        Properties prop = PropertyLoader.getProperties();

        final Optional<String> usernameOptional = initializerInput.getConnectionInformation()
                .getConnectionAttributeStore().getAsString(AuthConstants.USERNAME);
        String userName = usernameOptional.isPresent() ? usernameOptional.get() : StringUtils.EMPTY;

        if (HivemqUtils.isHealthCheckUser(prop, initializerInput.getConnectionInformation())) {
            String heathCheckTopic = prop.getProperty(AuthConstants.HEALTH_CHECK_TOPIC, HEALTH_CHECK_TOPIC);
            LOGGER.trace("Health check topic configured is: {}", heathCheckTopic);
            permittedTopics = Arrays.asList(getPermission(heathCheckTopic, TopicPermission.MqttActivity.PUBLISH));
            LOGGER.debug("Health check user:{} got permission for following  topics:{}", userName, permittedTopics);
        } else {
            try (final Timer.Context ignored1 = metricRegistry.timer(
                    ApplicationConstants.AUTHORIZATION_DEFAULT_PERMISSION_TIMER_JMX).time()) {
                permittedTopics = authorize(initializerInput);
            }
        }

        return permittedTopics;
    }

    /**
     * Retrieves an instance of a class specified by the given plugin class property.
     *
     * @param pluginClassProp the property name of the plugin class
     * @param optional        a flag indicating whether the class is optional or not
     * @return an Optional containing the instance of the specified class, or an empty Optional 
     *      if the class is optional and not available
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

    /**
     * Checks if the specified client ID is associated with a suspicious ECU (Electronic Control Unit).
     *
     * @param clientId the client ID to check
     * @return true if the client ID is associated with a suspicious ECU, false otherwise
     */
    public boolean isSuspiciousEcu(String clientId) {
        return false;
    }

    /**
     * Sets the topic formatter instance for this abstract authorization.
     *
     * @param topicFormatterInstance the topic formatter instance to be set
     */
    public abstract void setTopicFormatter(TopicFormatter topicFormatterInstance);

    /**
     * Authorizes the given initializer input and returns a list of topic permissions.
     *
     * @param initializerInput the initializer input to be authorized
     * @return a list of topic permissions
     */
    public abstract List<TopicPermission> authorize(InitializerInput initializerInput);
}
