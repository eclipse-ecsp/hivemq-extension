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

package org.eclipse.ecsp.hivemq.config;

import com.codahale.metrics.MetricRegistry;
import org.eclipse.ecsp.hivemq.auth.authentication.CertificateAuthentication;
import org.eclipse.ecsp.hivemq.auth.authentication.JwtAuthentication;
import org.eclipse.ecsp.hivemq.auth.authentication.UsernamePasswordAuthentication;
import org.eclipse.ecsp.hivemq.auth.authorization.Authorizer;
import org.eclipse.ecsp.hivemq.auth.authorization.CertificateBasedAuthorizer;
import org.eclipse.ecsp.hivemq.base.AbstractAuthentication;
import org.eclipse.ecsp.hivemq.base.AbstractAuthorization;
import org.eclipse.ecsp.hivemq.callbacks.AbstractOnPublishReceivedCallback;
import org.eclipse.ecsp.hivemq.callbacks.AbstractPingReqInboundInterceptor;
import org.eclipse.ecsp.hivemq.callbacks.AbstractSubscribeInboundInterceptor;
import org.eclipse.ecsp.hivemq.callbacks.ClientLifeCycleEvents;
import org.eclipse.ecsp.hivemq.callbacks.ConnectInterceptor;
import org.eclipse.ecsp.hivemq.callbacks.HiveStopCallback;
import org.eclipse.ecsp.hivemq.callbacks.MessageStoreCallback;
import org.eclipse.ecsp.hivemq.callbacks.OnPublishOutboundIntercept;
import org.eclipse.ecsp.hivemq.callbacks.OnSubscribeIntercept;
import org.eclipse.ecsp.hivemq.callbacks.PingServerRequest;
import org.eclipse.ecsp.hivemq.callbacks.PubackReceived;
import org.eclipse.ecsp.hivemq.callbacks.PubackSend;
import org.eclipse.ecsp.hivemq.callbacks.SubscriptionStatusHandler;
import org.eclipse.ecsp.hivemq.callbacks.UnsubscribeInboundIntercept;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * This class creates and return beans of different class.
 */
@Configuration
@EnableAspectJAutoProxy(proxyTargetClass = true)
@PropertySource("file:/opt/hivemq/conf/hivemq-plugin-base.properties")
@EnableScheduling
@ComponentScan(basePackages = { "org.eclipse.ecsp.hivemq" })
public class PluginConfig {

    /**
     * Creates and returns an instance of JwtAuthentication for JWT-based authentication.
     *
     * @return an instance of JwtAuthentication
     * @throws NoSuchAlgorithmException if the algorithm used for JWT authentication is not available
     * @throws InvalidKeySpecException if the key specification used for JWT authentication is invalid
     * @throws IOException if an I/O error occurs while creating the JwtAuthentication instance
     */
    @Bean("jwtauthentication")
    public AbstractAuthentication jwtAuthentication()
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        return new JwtAuthentication();
    }

    /**
     * Creates and returns an instance of CertificateAuthentication for certificate-based authentication.
     *
     * @return an instance of CertificateAuthentication
     * @throws NoSuchAlgorithmException if the specified algorithm is not available
     * @throws InvalidKeySpecException if the provided key specification is invalid
     * @throws IOException if an I/O error occurs while reading the certificate
     */
    @Bean("certificateauthentication")
    public AbstractAuthentication certificateAuthentication()
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        return new CertificateAuthentication();
    }

    /**
     * Creates an instance of the {@link UsernamePasswordAuthentication} class.
     * This method is annotated with {@link Bean} to indicate that the returned object should be managed 
     * by the Spring container.
     * The bean name is set to "usernamepasswordauthentication".
     *
     * @return An instance of the {@link AbstractAuthentication} class representing the username password 
     *      authentication.
     */
    @Bean("usernamepasswordauthentication")
    public AbstractAuthentication usernamePasswordAuthentication() {
        return new UsernamePasswordAuthentication();
    }

    /**
     * Creates and returns an instance of the {@link Authorizer} class.
     * This bean is used for authorization purposes.
     *
     * @return An instance of the {@link Authorizer} class.
     */
    @Bean("authorizer")
    public AbstractAuthorization authorizer() {
        return new Authorizer();
    }

    /**
     * Creates an instance of the CertificateBasedAuthorizer class and registers it as a bean with the name
     * "certificatebasedauthorizer".
     * This bean is used for certificate-based authorization.
     *
     * @return The CertificateBasedAuthorizer instance.
     */
    @Bean("certificatebasedauthorizer")
    public AbstractAuthorization certificateBasedAuthorizer() {
        return new CertificateBasedAuthorizer();
    }

    /**
     * Returns an instance of the {@link AbstractOnPublishReceivedCallback} interface
     * that is used as the message store callback.
     *
     * @return an instance of {@link AbstractOnPublishReceivedCallback}
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public AbstractOnPublishReceivedCallback messageStoreCallback() {
        return new MessageStoreCallback();
    }

    /**
     * Creates a new instance of the PingServerRequest interceptor.
     * This interceptor handles incoming PingReq messages from MQTT clients.
     * It is used to process and respond to PingReq messages.
     *
     * @return the PingServerRequest interceptor instance
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public AbstractPingReqInboundInterceptor pingServerRequest() {
        return new PingServerRequest();
    }

    /**
     * Creates and returns an instance of the {@link OnSubscribeIntercept} class,
     * which is an implementation of the {@link AbstractSubscribeInboundInterceptor} interface.
     * This interceptor is used to intercept and handle subscribe messages.
     *
     * @return an instance of the {@link OnSubscribeIntercept} class
     */
    @Bean
    public AbstractSubscribeInboundInterceptor onSubscribeIntercept() {
        return new OnSubscribeIntercept();
    }

    /**
     * Creates a new instance of the ClientLifeCycleEvents class.
     *
     * @return the ClientLifeCycleEvents instance
     */
    @Bean
    public ClientLifeCycleEvents clientLifeCycleEvents() {
        return new ClientLifeCycleEvents();
    }

    /**
     * Creates and returns a new instance of the ConnectInterceptor class.
     * This interceptor is used to intercept and handle connect events.
     *
     * @return a new instance of the ConnectInterceptor class
     */
    @Bean
    public ConnectInterceptor connectInterceptor() {
        return new ConnectInterceptor();
    }

    /**
     * Creates a new instance of the HiveStopCallback class.
     *
     * @return the HiveStopCallback instance
     */
    @Bean
    public HiveStopCallback hiveStopCallback() {
        return new HiveStopCallback();
    }

    /**
     * Creates a new instance of the OnPublishOutboundIntercept class with the specified MetricRegistry.
     *
     * @param metricRegistry the MetricRegistry to be used by the OnPublishOutboundIntercept instance
     * @return a new instance of the OnPublishOutboundIntercept class
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public OnPublishOutboundIntercept onPublishOutboundIntercept(MetricRegistry metricRegistry) {
        return new OnPublishOutboundIntercept(metricRegistry);
    }

    /**
     * Creates and returns a new instance of the PubackReceived class.
     *
     * @return a new instance of the PubackReceived class
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public PubackReceived pubackReceived() {
        return new PubackReceived();
    }

    /**
     * Creates and returns a new instance of the PubackSend class.
     *
     * @return a new instance of the PubackSend class.
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public PubackSend pubackSend() {
        return new PubackSend();
    }

    /**
     * Creates and returns a new instance of SubscriptionStatusHandler.
     * This method is annotated with @Bean to indicate that it is a bean definition method.
     * The bean is scoped as prototype, meaning a new instance will be created each time it is requested.
     *
     * @return a new instance of SubscriptionStatusHandler
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public SubscriptionStatusHandler subscriptionStatusHandler() {
        return new SubscriptionStatusHandler();
    }

    /**
     * Creates a new instance of the {@link UnsubscribeInboundIntercept} class.
     * This method is annotated with {@link Bean} and {@link Scope} annotations to define the bean scope as prototype.
     * Prototype scope means that a new instance of the bean will be created each time it is requested.
     *
     * @return a new instance of the {@link UnsubscribeInboundIntercept} class.
     */
    @Bean
    @Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public UnsubscribeInboundIntercept unsubscribeInboundIntercept() {
        return new UnsubscribeInboundIntercept();
    }
}
