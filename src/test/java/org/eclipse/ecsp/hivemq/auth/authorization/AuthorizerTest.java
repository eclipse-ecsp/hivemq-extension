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

package org.eclipse.ecsp.hivemq.auth.authorization;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.client.ClientContext;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.InitializerInput;
import com.hivemq.extension.sdk.api.client.parameter.Listener;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import org.eclipse.ecsp.hivemq.auth.authorization.stub.StubClientService;
import org.eclipse.ecsp.hivemq.auth.authorization.stub.StubModifiableDefaultPermissions;
import org.eclipse.ecsp.hivemq.auth.authorization.stub.StubTopicPermissionBuilder;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
import org.eclipse.ecsp.hivemq.callbacks.MessageStoreCallback;
import org.eclipse.ecsp.hivemq.callbacks.PingServerRequest;
import org.eclipse.ecsp.hivemq.d2v.DeviceToVehicleMapperFactory;
import org.eclipse.ecsp.hivemq.d2v.DeviceToVehicleMapperVpImpl;
import org.eclipse.ecsp.hivemq.utils.IgniteTopicFormatter;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import static java.util.Arrays.asList;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.PROFILE_CHECK_DISABLED_TOPICS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for Authorizer.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Builders.class, Services.class, DeviceToVehicleMapperFactory.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class AuthorizerTest {

    private static final String SUBSCRIBE = "SUBSCRIBE";
    private static final String PUBLISH = "PUBLISH";
    @Mock
    private ClientContext clientContext;
    @Mock
    InitializerInput initializerInput;
    @Mock
    private ConnectionInformation connectionInformation;
    @Mock
    private ConnectionAttributeStore connectionAttributeStore;
    String clientId = "HUXOIDDN4HUN18";

    Authorizer authorize;

    private static final String FILE_PATH = "src/test/resources/";
    private static final int TWO = 2;
    private static final int THREE = 3;
    private static final int FOUR = 4;
    private static final int FIVE = 5;
    private static final int PORT = 1888;

    /**
     * This setup method gets called before each test case and load required properties.
     */
    @Before
    public void setUp() throws Exception {
        initMocks(this);
        PropertyLoader.getProperties(FILE_PATH + "hivemq-plugin-base.properties");
        PowerMockito.mockStatic(DeviceToVehicleMapperFactory.class);
        PowerMockito.when(DeviceToVehicleMapperFactory.getInstance()).thenReturn(new DeviceToVehicleMapperVpImpl());
    }

    /**
    * Test case for the {@link Authorizer} class.
    * This method tests the initialization of the Authorizer object and ensures that it is not null.
    *
    * @throws IOException if an I/O error occurs.
    */
    @Test
    public void testAuthorizator() throws IOException {
        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());
        assertNotNull(authorize);
    }

    /**
     * Test case to verify the behavior of the authorize method.
     *
     * @throws Exception if an error occurs during the test case execution
     */
    @Test
    public void getDevicePermissionWithNullIgnoringTopicsValue() throws Exception {
        PropertyLoader.getPropertiesMap().remove(PROFILE_CHECK_DISABLED_TOPICS);
        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());

        Authorizer.removeFromPermissionMap(clientId);
        Optional<String> providedUsername = Optional.of("harman/dev/user");
        Mockito.when(initializerInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        Mockito.when(initializerInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(providedUsername);

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder());

        List<TopicPermission> mqttTopicPermission = authorize.authorize(initializerInput);

        assertEquals("haa/harman/dev/" + clientId + "/2c/alerts", mqttTopicPermission.get(THREE).getTopicFilter());
        assertEquals(PUBLISH, mqttTopicPermission.get(THREE).getActivity().toString());

        assertEquals("haa/harman/dev/" + clientId + "/2c/events", mqttTopicPermission.get(TWO).getTopicFilter());
        assertEquals(PUBLISH, mqttTopicPermission.get(TWO).getActivity().toString());

        assertEquals("haa/harman/dev/user/2d/config", mqttTopicPermission.get(0).getTopicFilter());
        assertEquals(SUBSCRIBE, mqttTopicPermission.get(0).getActivity().toString());
    }

    /**
     * Test case to verify the behavior of the authorize method.
     * This method tests the authorization logic when there is one topic value to be ignored.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Test
    public void getDevicePermissionWithOneIgnoringTopicsValue() throws IOException {
        PropertyLoader.getPropertiesMap().put(PROFILE_CHECK_DISABLED_TOPICS, asList("commcheck"));

        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());

        Optional<String> providedUsername = Optional.of("harman/dev/user");
        Mockito.when(initializerInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        Mockito.when(initializerInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(providedUsername);

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder());

        Authorizer.removeFromPermissionMap(clientId);
        List<TopicPermission> mqttTopicsPermission = authorize.authorize(initializerInput);

        TopicPermission mqttTopicPermission = mqttTopicsPermission.get(1);
        assertEquals("haa/harman/dev/user/2d/commcheck", mqttTopicPermission.getTopicFilter());
        assertEquals(SUBSCRIBE, mqttTopicPermission.getActivity().toString());

        mqttTopicPermission = mqttTopicsPermission.get(THREE);
        assertEquals("haa/harman/dev/" + clientId + "/2c/events", mqttTopicPermission.getTopicFilter());
        assertEquals(PUBLISH, mqttTopicPermission.getActivity().toString());

        mqttTopicPermission = mqttTopicsPermission.get(FOUR);
        assertEquals("haa/harman/dev/" + clientId + "/2c/alerts", mqttTopicPermission.getTopicFilter());
        assertEquals(PUBLISH, mqttTopicPermission.getActivity().toString());

        mqttTopicPermission = mqttTopicsPermission.get(FIVE);
        assertEquals("haa/harman/dev/" + clientId + "/2c/commcheck", mqttTopicPermission.getTopicFilter());
        assertEquals(PUBLISH, mqttTopicPermission.getActivity().toString());

        PropertyLoader.getPropertiesMap().remove(PROFILE_CHECK_DISABLED_TOPICS);
    }

    /**
     * This method tests the initialization of the Authorizer class.
     * It verifies the behavior of the Authorizer's initialize method
     * under different scenarios, such as when a specific username is provided
     * and when a different username is provided.
     *
     * @throws IOException if an I/O error occurs during the test
     */
    @Test
    public void testInitialize() throws IOException {
        PropertyLoader.getPropertiesMap().remove(PROFILE_CHECK_DISABLED_TOPICS);

        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());

        Optional<String> providedUsername = Optional.of("harman/dev/haa_api");
        Mockito.when(initializerInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        Mockito.when(initializerInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(providedUsername);

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder());
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.clientService()).thenReturn(new StubClientService());
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());

        ModifiableDefaultPermissions defaultPermissions = new StubModifiableDefaultPermissions();
        Mockito.when(clientContext.getDefaultPermissions()).thenReturn(defaultPermissions);

        AnnotationConfigApplicationContext applicationContext = Mockito.mock(AnnotationConfigApplicationContext.class);
        authorize.setApplicationContext(applicationContext);
        Mockito.when(applicationContext.getBean("messageStoreCallback"))
                .thenReturn(Mockito.mock(MessageStoreCallback.class));
        Mockito.when(applicationContext.getBean("pingServerRequest"))
                .thenReturn(Mockito.mock(PingServerRequest.class));

        authorize.initialize(initializerInput, clientContext);

        List<TopicPermission> mqttTopicPermissions = defaultPermissions.asList();
        assertEquals("haa/harman/dev/#", mqttTopicPermissions.get(0).getTopicFilter());
        defaultPermissions.clear();


        // User authorization test
        providedUsername = Optional.of("User1");
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(providedUsername);

        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder());

        authorize.initialize(initializerInput, clientContext);
        mqttTopicPermissions = defaultPermissions.asList();
        assertEquals("haa/harman/dev/User1/notification", mqttTopicPermissions.get(0).getTopicFilter());
        assertEquals(SUBSCRIBE, mqttTopicPermissions.get(0).getActivity().toString());
        defaultPermissions.clear();
    }
    
    /**
     * Test case to verify the initialization of the Authorizer for a device client.
     * This method tests the initialization process of the Authorizer by setting up the necessary dependencies 
     * and verifying the default permissions.
     *
     * @throws IOException if an I/O error occurs during the test.
     */
    @Test
    public void testInitializeForDeviceClient() throws IOException {
        PropertyLoader.getPropertiesMap().remove(PROFILE_CHECK_DISABLED_TOPICS);
        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());
        
        Optional<String> providedUsername = Optional.of("harman/dev/HUXOIDDN4HUN18");
        Mockito.when(initializerInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        Mockito.when(initializerInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(providedUsername);
        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder());
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.clientService()).thenReturn(new StubClientService());
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        
        Authorizer.removeFromPermissionMap(clientId);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder());
        

        ModifiableDefaultPermissions defaultPermissions = new StubModifiableDefaultPermissions();
        Mockito.when(clientContext.getDefaultPermissions()).thenReturn(defaultPermissions);
        AnnotationConfigApplicationContext applicationContext = Mockito.mock(AnnotationConfigApplicationContext.class);
        authorize.setApplicationContext(applicationContext);
        Mockito.when(applicationContext.getBean("messageStoreCallback"))
                .thenReturn(Mockito.mock(MessageStoreCallback.class));
        Mockito.when(applicationContext.getBean("pingServerRequest"))
                .thenReturn(Mockito.mock(PingServerRequest.class));

        authorize.setApplicationContext(applicationContext);
        authorize.initialize(initializerInput, clientContext);
        List<TopicPermission> mqttTopicPermissions = defaultPermissions.asList();
        TopicPermission mqttTopicPermission = mqttTopicPermissions.get(0);

        assertEquals("haa/harman/dev/HUXOIDDN4HUN18/2d/config", mqttTopicPermission.getTopicFilter());
        assertEquals(SUBSCRIBE, mqttTopicPermission.getActivity().toString());

        mqttTopicPermission = mqttTopicPermissions.get(TWO);
        assertEquals("haa/harman/dev/HUXOIDDN4HUN18/2c/events", mqttTopicPermission.getTopicFilter());
        assertEquals(PUBLISH, mqttTopicPermission.getActivity().toString());

        mqttTopicPermission = mqttTopicPermissions.get(THREE);
        assertEquals("haa/harman/dev/HUXOIDDN4HUN18/2c/alerts", mqttTopicPermission.getTopicFilter());
        assertEquals(PUBLISH, mqttTopicPermission.getActivity().toString());
        defaultPermissions.clear();
    }
    
    /**
     * Test case to verify the initialization of a health user.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Test
    public void testInitializeHealthUser() throws IOException {
        PropertyLoader.getPropertiesMap().remove(PROFILE_CHECK_DISABLED_TOPICS);

        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());
        Listener listener = Mockito.mock(Listener.class);
        Optional<String> providedUsername = Optional.of("health");
        Mockito.when(initializerInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        Mockito.when(initializerInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        Mockito.when(initializerInput.getConnectionInformation().getListener()).thenReturn(Optional.of(listener));
        Mockito.when(listener.getPort()).thenReturn(PORT);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(providedUsername);

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder());
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.clientService()).thenReturn(new StubClientService());
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());

        List<TopicPermission> mqttTopicPermissions = authorize.getPermissions(initializerInput);
        assertEquals("health", mqttTopicPermissions.get(0).getTopicFilter());
    }

    /**
     * Head Uint swap test while headunit swap topic enabled.
     *
     * @throws IOException - throw if not able to read properties from property file.
     */
    @Test
    public void testHeadUnitDeviceTypeEmptyTopicForSuccess() throws IOException {
        PropertyLoader.getPropertiesMap().remove(PROFILE_CHECK_DISABLED_TOPICS);

        PropertyLoader.getProperties().put(AuthConstants.DISCONNECT_TOPIC_NAME, "disconnect");

        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());

        String clientId = "hu-swap-test-client";
        Optional<String> providedUsername = Optional.of("harman/dev/" + clientId);
        Mockito.when(initializerInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        Mockito.when(initializerInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(providedUsername);

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder()).thenReturn(new StubTopicPermissionBuilder());
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.clientService()).thenReturn(new StubClientService());
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());

        Authorizer.removeFromPermissionMap(clientId);

        List<TopicPermission> mqttTopicPermissions = authorize.getPermissions(initializerInput);

        // when deviceType empty
        assertEquals(FOUR, mqttTopicPermissions.size());
    }

    /**
     * Test case to verify the authorization of a head unit topic for success.
     * This method tests the scenario where the authorization is successful for a head unit topic.
     * It sets up the necessary properties and mocks the required dependencies to simulate the test scenario.
     * It then calls the getPermissions method of the Authorizer class and asserts that the returned list of 
     * topic permissions has the expected size.
     *
     * @throws IOException if an I/O error occurs during the test.
     */
    @Test
    public void testHeadUnitTopicForSuccess() throws IOException {
        PropertyLoader.getPropertiesMap().remove(AuthConstants.IS_ALLOWED_ALL_TOPIC);
        // for hu type
        PropertyLoader.getProperties().put(AuthConstants.IS_ALLOWED_ALL_TOPIC, "false");

        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());

        String clientId = "hu-swap-test-client";
        Optional<String> providedUsername = Optional.of("harman/dev/" + clientId);
        Mockito.when(initializerInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        Mockito.when(initializerInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(providedUsername);

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder());
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.clientService()).thenReturn(new StubClientService());
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());

        DeviceSubscription deviceSubscription = new DeviceSubscription("device123");
        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("hu");
        deviceSubscription.setDeviceType(deviceType);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription(clientId, deviceSubscription);
        Authorizer.removeFromPermissionMap(clientId);

        List<TopicPermission> mqttTopicPermission = authorize.getPermissions(initializerInput);

        assertEquals(THREE, mqttTopicPermission.size());
    }

    /**
     * Test case to verify the successful authorization of telematics topic.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testTelematicsTopicForSuccess() throws IOException {
        PropertyLoader.getPropertiesMap().remove(AuthConstants.IS_ALLOWED_ALL_TOPIC);
        // for hu type
        PropertyLoader.getProperties().put(AuthConstants.IS_ALLOWED_ALL_TOPIC, "false");

        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());

        String clientId = "telematics-test-client";
        Optional<String> providedUsername = Optional.of("harman/dev/" + clientId);
        Mockito.when(initializerInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        Mockito.when(initializerInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(providedUsername);

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder()).thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder());
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.clientService()).thenReturn(new StubClientService());
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());

        DeviceSubscription deviceSubscription = new DeviceSubscription("device123");
        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("telematics");
        deviceSubscription.setDeviceType(deviceType);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription(clientId, deviceSubscription);
        Authorizer.removeFromPermissionMap(clientId);

        List<TopicPermission> mqttTopicPermission = authorize.getPermissions(initializerInput);

        assertEquals(FIVE, mqttTopicPermission.size());
    }

    /**
     * Head Uint swap test while headunit swap topic disabled.
     *
     * @throws IOException throw if not able to read properties from property file.
     */
    @Test
    public void testHeadUnitTopicForFailure() throws IOException {
        PropertyLoader.getPropertiesMap().remove(PROFILE_CHECK_DISABLED_TOPICS);

        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());

        String clientId = "hu-swap-test-client";
        Optional<String> providedUsername = Optional.of("harman/dev/" + clientId);
        Mockito.when(initializerInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        Mockito.when(initializerInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(providedUsername);

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder());
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.clientService()).thenReturn(new StubClientService());
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        Authorizer.removeFromPermissionMap(clientId);
        List<TopicPermission> mqttTopicPermission = authorize.getPermissions(initializerInput);
        String expectedTopic = "haa/harman/dev/" + clientId + "/2d/disconnect";
        mqttTopicPermission = mqttTopicPermission.stream().filter(x -> x.getTopicFilter().equals(expectedTopic))
                .toList();
        assertTrue(mqttTopicPermission.isEmpty());
    }

    /**
     * Test case to verify the QoS level for MQTT topic permissions.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testQos2Level() throws IOException {
        PropertyLoader.getProperties(FILE_PATH + "hivemq-plugin-qos2.properties");

        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());

        String clientId = "haa_api";
        Optional<String> providedUsername = Optional.of("harman/dev/haa_api");
        Mockito.when(initializerInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        Mockito.when(initializerInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(providedUsername);

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder());
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.clientService()).thenReturn(new StubClientService());
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        Authorizer.removeFromPermissionMap(clientId);

        List<TopicPermission> mqttTopicPermission = authorize.getPermissions(initializerInput);

        assertEquals(TopicPermission.Qos.TWO, mqttTopicPermission.get(0).getQos());
    }

    /**
     * Test case to verify the behavior of QoS level 1.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testQos1Level() throws IOException {
        PropertyLoader.getProperties(FILE_PATH + "hivemq-plugin-qos1.properties");

        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());

        String clientId = "haa_api";
        Optional<String> providedUsername = Optional.of("harman/dev/haa_api");
        Mockito.when(initializerInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        Mockito.when(initializerInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(providedUsername);

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder());
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.clientService()).thenReturn(new StubClientService());
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        Authorizer.removeFromPermissionMap(clientId);

        List<TopicPermission> mqttTopicPermission = authorize.getPermissions(initializerInput);
        assertEquals(TopicPermission.Qos.ONE, mqttTopicPermission.get(0).getQos());
    }

    /**
     * Test case to verify the behavior of the `getPermissions` method in the `Authorizer` class
     * when the QoS level is set to ALL.
     *
     * @throws IOException if an I/O error occurs while loading the properties file.
     */
    @Test
    public void testQosAllLevel() throws IOException {
        PropertyLoader.getProperties(FILE_PATH + "hivemq-plugin-qos-all.properties");

        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());

        String clientId = "haa_api";
        Optional<String> providedUsername = Optional.of("harman/dev/haa_api");
        Mockito.when(initializerInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        Mockito.when(initializerInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(providedUsername);

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder());
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.clientService()).thenReturn(new StubClientService());
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        Authorizer.removeFromPermissionMap(clientId);

        List<TopicPermission> mqttTopicPermission = authorize.getPermissions(initializerInput);
        assertEquals(TopicPermission.Qos.ALL, mqttTopicPermission.get(0).getQos());
    }

    /**
     * Test case to verify the behavior when the QoS level is not set.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testQosLevelNotSet() throws IOException {
        PropertyLoader.getProperties(FILE_PATH + "hivemq-plugin-base.properties");

        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());

        String clientId = "haa_api";
        Optional<String> providedUsername = Optional.of("harman/dev/haa_api");
        Mockito.when(initializerInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        Mockito.when(initializerInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(providedUsername);

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder());
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.clientService()).thenReturn(new StubClientService());
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        Authorizer.removeFromPermissionMap(clientId);

        List<TopicPermission> mqttTopicPermission = authorize.getPermissions(initializerInput);
        assertEquals(TopicPermission.Qos.ALL, mqttTopicPermission.get(0).getQos());
    }
}