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

package org.eclipse.ecsp.hivemq.plugin;

import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.events.EventRegistry;
import com.hivemq.extension.sdk.api.events.client.ClientLifecycleEventListener;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.analytics.stream.base.utils.ObjectUtils;
import org.eclipse.ecsp.cache.redis.IgniteCacheRedisImpl;
import org.eclipse.ecsp.cache.redis.RedisConfig;
import org.eclipse.ecsp.hivemq.auth.authentication.AuthenticationFactory;
import org.eclipse.ecsp.hivemq.auth.authorization.AuthorizationFactory;
import org.eclipse.ecsp.hivemq.base.BaseAuthenticatorProvider;
import org.eclipse.ecsp.hivemq.base.IgniteAuthenticationCallback;
import org.eclipse.ecsp.hivemq.base.IgniteAuthorizerExtension;
import org.eclipse.ecsp.hivemq.base.SubscribeAuthProvider;
import org.eclipse.ecsp.hivemq.base.VehicleProfileDataExtraction;
import org.eclipse.ecsp.hivemq.callbacks.AbstractSubscribeInboundInterceptor;
import org.eclipse.ecsp.hivemq.callbacks.ConnectInterceptor;
import org.eclipse.ecsp.hivemq.callbacks.HiveStopCallback;
import org.eclipse.ecsp.hivemq.callbacks.SubscriptionStatusHandler;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.eclipse.ecsp.hivemq.utils.DynamicPropertyUpdater;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.hivemq.utils.VehicleProfileApiClient;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.util.Map;
import java.util.Optional;
import static org.eclipse.ecsp.hivemq.kafka.ApplicationConstants.REDIS_ENABLED;

/**
 * This is the main class of the Extension, which is instantiated during the
 * HiveMQ start up process.
 *
 * @author Maheshgouda Patil
 */
public class PluginMainClass implements ExtensionMain {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PluginMainClass.class);
    private IgniteAuthorizerExtension authorization;
    private HiveStopCallback hiveStopCallback;
    private ClientLifecycleEventListener clientLifeCycleEvents;
    private BaseAuthenticatorProvider baseAuthenticatorProvider;
    private SubscribeAuthProvider subscriptionAuthorizer;
    final ClientService clientService = Services.clientService();
    private ConnectInterceptor connectInterceptor;
    private AnnotationConfigApplicationContext applicationContext;
    private static final int PATCH_VERSION = 4;
    private static final String FALSE = "false";

    /**
     * Retrieves an instance of an object based on the given property name.
     *
     * @param propertyName the name of the property
     * @return an Optional containing the instance of the object, or an empty Optional if the object 
     *      could not be instantiated
     */
    private Optional<Object> getInstance(String propertyName) {
        // Object instantiation is must.
        return getInstance(propertyName, false);
    }

    /**
     * It returns instance of a class. It read the class name from the property
     * file.
     *
     * @param pluginClassProp classNameProperty
     * @param optional - if true - returns empty, false - return required instance
     * @return instance of required class
     */
    private Optional<Object> getInstance(String pluginClassProp, boolean optional) {
        String className = StringUtils.EMPTY;
        try {
            className = PropertyLoader.getValue(pluginClassProp);
            if (optional && StringUtils.isEmpty(className)) {
                return Optional.empty();
            }
            return Optional.ofNullable(applicationContext.getBean(getClass().getClassLoader().loadClass(className)));
        } catch (ClassNotFoundException | IllegalArgumentException | SecurityException e) {
            throw new IllegalArgumentException(
                    pluginClassProp + " refers to a class (" + className + ") that is not available on the classpath");
        }
    }

    /**
     * Initializes the plugin by setting up various components and dependencies.
     * This method performs the following tasks:
     * 1. Retrieves the value of the SSDP_SIMULATOR property and determines if the plugin is running in simulator mode.
     * 2. Creates an instance of the AnnotationConfigApplicationContext using the configured plugin config class.
     * 3. Retrieves the authentication implementation from the AuthenticationFactory.
     * 4. Initializes the BaseAuthenticatorProvider with the authentication implementation.
     * 5. Determines the class name for the subscribe interceptor based on the simulator mode.
     * 6. Retrieves the subscribe interceptor bean from the application context.
     * 7. Initializes the SubscribeAuthProvider with the subscribe interceptor.
     * 8. Retrieves the client lifecycle event listener implementation.
     * 9. Retrieves the HiveStopCallback bean from the application context.
     * 10. Retrieves the VehicleProfileDataExtraction implementation and sets it in the VehicleProfileApiClient.
     * 11. Retrieves the ConnectInterceptor bean from the application context.
     * 12. Initializes the RedissonClient and sets it in the IgniteCacheRedisImpl if Redis is enabled.
     * 13. Retrieves the authorization implementation from the AuthorizationFactory.
     *
     * @throws ClassNotFoundException if the plugin config class or subscribe interceptor class cannot be found.
     */
    private void init() throws ClassNotFoundException {
        boolean isSimulator = Boolean
                .parseBoolean(PropertyLoader.getValue(ApplicationConstants.SSDP_SIMULATOR, FALSE));
        applicationContext = new AnnotationConfigApplicationContext(
                Class.forName(PropertyLoader.getValue(ApplicationConstants.HIVEMQ_PLUGIN_CONFIG_CLASS)));
        IgniteAuthenticationCallback authImplementation = AuthenticationFactory.getInstance(applicationContext);
        baseAuthenticatorProvider = new BaseAuthenticatorProvider(authImplementation);

        String subscribeClass = "org.eclipse.ecsp.hivemq.callbacks.OnSubscribeIntercept";
        if (isSimulator) {
            subscribeClass = "org.eclipse.ecsp.hivemq.simulator.SimulatorOnSubscribeIntercept";
        }
        AbstractSubscribeInboundInterceptor subscribeInboundInterceptor 
            = (AbstractSubscribeInboundInterceptor) applicationContext.getBean(getClass().getClassLoader()
                    .loadClass(subscribeClass));
        subscriptionAuthorizer = new SubscribeAuthProvider(subscribeInboundInterceptor);

        clientLifeCycleEvents = (ClientLifecycleEventListener) getInstance(
                ApplicationConstants.HIVEMQ_PLUGIN_CLIENT_LIFECYCLE_IMPL_CLASS).get();
        hiveStopCallback = applicationContext.getBean(HiveStopCallback.class);

        VehicleProfileDataExtraction vehicleProfileDataExtraction = (VehicleProfileDataExtraction) getInstance(
                ApplicationConstants.VEHICLE_PROFILE_DATA_EXTRACTER_CLASS, false).get();
        VehicleProfileApiClient.setVehicleProfileDataExtraction(vehicleProfileDataExtraction);

        connectInterceptor = applicationContext.getBean(ConnectInterceptor.class);
        boolean redisEnabled = Boolean.parseBoolean(PropertyLoader.getValue(REDIS_ENABLED, FALSE));
        if (redisEnabled) {
            RedisConfig redisConfig = new RedisConfig();
            Map<String, String> redisProperties = PropertyLoader.getRedisPropertiesMap();
            LOGGER.info("Trying to initialize the PluginMainClass RedissonClient with properties: {}", redisProperties);
            RedissonClient redissonClient = redisConfig.builder().build(PropertyLoader.getRedisPropertiesMap());
            ObjectUtils.requireNonNull(redissonClient, "Unable to initialize Redisson client. Aborting..");
            IgniteCacheRedisImpl igniteCacheRedisImpl = new IgniteCacheRedisImpl();
            igniteCacheRedisImpl.setRedissonClient(redissonClient);
            SubscriptionStatusHandler.setIgniteCacheRedisImpl(igniteCacheRedisImpl);
        }
        this.authorization = AuthorizationFactory.getInstance(applicationContext);
    }

    /**
     * This method is called when the extension starts.
     * It initializes various components and sets up the necessary configurations.
     *
     * @param extensionStartInput  The input parameters for the extension start.
     * @param extensionStartOutput The output parameters for the extension start.
     */
    @Override
    public void extensionStart(@NotNull ExtensionStartInput extensionStartInput,
            @NotNull ExtensionStartOutput extensionStartOutput) {
        try {
            init();
            Services.metricRegistry().gauge(ApplicationConstants.HIVEMQ_PATCH_VERSION_JMX, () -> () -> PATCH_VERSION);
            HivemqSinkService.initializeSinkService();
            addClientLifecycleEventListener();
            Services.initializerRegistry().setClientInitializer(authorization);
            Services.securityRegistry().setAuthenticatorProvider(baseAuthenticatorProvider);
            Services.securityRegistry().setAuthorizerProvider(subscriptionAuthorizer);
            Services.interceptorRegistry().setConnectInboundInterceptorProvider(input -> connectInterceptor);
            DynamicPropertyUpdater.init();
            LOGGER.info("HiveMQ extension started successfully...");
        } catch (Exception exception) {
            LOGGER.error("Exception encountered during initialization of PluginMainClass Interceptors: {}", exception);
            extensionStartOutput.preventExtensionStartup("Unable to start hivemq extension");
        }
    }

    /**
        * This method is called when the extension is stopped.
        * It invokes the `onBrokerStop` method of the `hiveStopCallback` object
        * and closes the application context.
        *
        * @param extensionStopInput  the input parameters for the extension stop
        * @param extensionStopOutput the output parameters for the extension stop
        */
    @Override
    public void extensionStop(@NotNull ExtensionStopInput extensionStopInput,
            @NotNull ExtensionStopOutput extensionStopOutput) {
        hiveStopCallback.onBrokerStop();
        applicationContext.close();
    }

    /**
     * Adds a client lifecycle event listener to the event registry.
     * This listener will be notified of client lifecycle events.
     */
    private void addClientLifecycleEventListener() {
        final EventRegistry eventRegistry = Services.eventRegistry();
        eventRegistry.setClientLifecycleEventListener(input -> clientLifeCycleEvents);
    }
}
