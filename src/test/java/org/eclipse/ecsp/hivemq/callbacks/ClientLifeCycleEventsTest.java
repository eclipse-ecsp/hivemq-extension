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
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.Listener;
import com.hivemq.extension.sdk.api.events.client.parameters.AuthenticationSuccessfulInput;
import com.hivemq.extension.sdk.api.events.client.parameters.ConnectionStartInput;
import com.hivemq.extension.sdk.api.events.client.parameters.DisconnectEventInput;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extension.sdk.api.packets.connect.WillPublishPacket;
import com.hivemq.extension.sdk.api.packets.general.DisconnectedReasonCode;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCache;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
import org.eclipse.ecsp.hivemq.d2v.DeviceToVehicleMapper;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.eclipse.ecsp.hivemq.utils.HivemqServiceProvider;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for ClientLifeCycleEvents.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Services.class, HivemqSinkService.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class ClientLifeCycleEventsTest {

    @Mock
    AuthenticationSuccessfulInput connect;

    @Mock
    ConnectionInformation connectionInformation;

    @Mock
    ClientInformation clientInformation;

    @Mock
    ConnectionAttributeStore connectionAttributeStore;

    @Mock
    DeviceToVehicleMapper deviceToVehicleMapper;

    @Mock
    HivemqSinkService hivemqSinkService;

    @Mock
    DisconnectEventInput disconnectEventInput;

    @Mock
    ClientService blockingClientService;

    @Mock
    private MetricRegistry registry;

    // @Rule
    public MockWebServer mockWebServer;
    private static final int PORT = 1888;
    private static final int TWO = 2;
    private static final int BUFFER_CAPACITY = 10;

    static {
        File path = new File(ClientLifeCycleEventsTest.class.getClassLoader()
                .getResource("hivemq-plugin-base.properties").getFile());
        PropertyLoader.reload(path);
    }

    /**
     * This setup method gets called before each test case and load required properties.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(registry);
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        PropertyLoader.loadRedisProperties("src/test/resources");
        byte[] payload = "test data".getBytes();
        doNothing().when(hivemqSinkService).sendMsgToSink(payload, payload, "test");

        when(connect.getConnectionInformation()).thenReturn(connectionInformation);
        when(connect.getClientInformation()).thenReturn(clientInformation);
        when(connect.getConnectionInformation().getConnectionAttributeStore()).thenReturn(connectionAttributeStore);

        HivemqServiceProvider.setBlockingClientService(blockingClientService);
    }

    /**
     * This method tests the behavior of the `doDisconnect` method in the `ClientLifeCycleEvents` class.
     * It mocks the necessary dependencies and verifies that the `doDisconnect` method removes the device subscription
     * from the cache when a client disconnects.
     */
    @Test
    public void testOnDisconnect() {
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        String clientId = "haa_api";

        when(disconnectEventInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(disconnectEventInput.getClientInformation()).thenReturn(clientInformation);
        when(disconnectEventInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        when(disconnectEventInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(java.util.Optional.ofNullable(clientId));
        when(disconnectEventInput.getClientInformation().getClientId()).thenReturn(clientId);

        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        deviceSubscriptionCache.addSubscription(clientId, new DeviceSubscription(clientId));

        ClientLifeCycleEvents clientLifeCycle = new ClientLifeCycleEvents();
        DeviceSubscription ds = deviceSubscriptionCache.getSubscription(clientId);
        Assert.assertNotNull(ds);
        Assert.assertEquals(clientId, ds.getVehicleId());

        clientLifeCycle.doDisconnect(disconnectEventInput);
        ds = deviceSubscriptionCache.getSubscription(clientId);
        Assert.assertNull(ds);
    }

    /**
     * Test case for the {@link ClientLifeCycleEvents#onMqttConnectionStart(ConnectionStartInput)} method.
     * This test verifies the behavior of the method when different scenarios are encountered.
     * 
     * <p>Scenarios:
     *
     * <p>1. Health User:
     *    - The connect packet has the clean start flag set to true.
     *    - The connection information contains a listener with a specific port.
     *    - The connection attribute store contains the username attribute set to "health".
     * 
     * <p>2. Non-Health User Will Publish Not Present:
     *    - The connect packet has the clean start flag set to true.
     *    - The connect packet contains a username attribute set to "dummy".
     *    - The connection attribute store contains the username attribute set to "dummy".
     * 
     * <p>3. Non-Health User Will Publish Present:
     *    - The connect packet has the clean start flag set to true.
     *    - The connect packet contains a username attribute set to "dummy".
     *    - The connection attribute store contains the username attribute set to "dummy".
     *    - The connect packet contains a will publish packet with a payload.
     * 
     * <p>@see ClientLifeCycleEvents#onMqttConnectionStart(ConnectionStartInput)
     */
    @Test // correct
    public void testOnMqttConnectionStart() {
        ConnectionStartInput connectionStartInput = Mockito.mock(ConnectionStartInput.class);
        ConnectPacket connectPacket = Mockito.mock(ConnectPacket.class);
        Listener listener = Mockito.mock(Listener.class);
        when(connectPacket.getCleanStart()).thenReturn(true);
        when(connectionStartInput.getConnectPacket()).thenReturn(connectPacket);
        when(connectionStartInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(connectionStartInput.getClientInformation()).thenReturn(clientInformation);
        when(connectionInformation.getListener()).thenReturn(Optional.of(listener));
        when(listener.getPort()).thenReturn(PORT);
        when(connectionStartInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        when(connectionAttributeStore.getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of("health"));
        when(registry.timer(anyString())).thenReturn(new Timer());
        ClientLifeCycleEvents clientConnect = new ClientLifeCycleEvents();
        // Case 1: Health User
        clientConnect.onMqttConnectionStart(connectionStartInput);

        // Case 2: Non Health User Will Publish Not Present
        when(connectPacket.getUserName()).thenReturn(Optional.of("dummy"));
        when(connectionAttributeStore.getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of("dummy"));
        clientConnect.onMqttConnectionStart(connectionStartInput);

        // Case 3: Non Health User Will Publish Present
        WillPublishPacket willMockito = Mockito.mock(WillPublishPacket.class);
        when(connectPacket.getWillPublish()).thenReturn(Optional.of(willMockito));
        when(willMockito.getPayload()).thenReturn(Optional.of(ByteBuffer.allocate(BUFFER_CAPACITY)));

        clientConnect.onMqttConnectionStart(connectionStartInput);
    }

    /**
    * Test case for the onAuthenticationSuccessful method.
    *
    * <p>This test verifies that the onAuthenticationSuccessful method of the ClientLifeCycleEvents class
    * is working correctly by simulating a successful authentication and calling the method.
    */
    @Test
    public void testOnAuthenticationSuccessful() {
        ClientLifeCycleEvents clientConnect = new ClientLifeCycleEvents();
        when(connect.getClientInformation().getClientId()).thenReturn("device");
        clientConnect.onAuthenticationSuccessful(connect);
    }

    /**
     * Test case to verify the behavior of the `doDisconnect` method in the `ClientLifeCycleEvents` class
     * when an abrupt disconnect event occurs.
     */
    @Test
    public void testIsAbruptDisconnect() {
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        String clientId = "HUX1345656";
        DisconnectEventInput disconnectEventInput = Mockito.mock(DisconnectEventInput.class);
        when(disconnectEventInput.getClientInformation()).thenReturn(clientInformation);
        when(disconnectEventInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(connectionInformation.getConnectionAttributeStore()).thenReturn(connectionAttributeStore);
        when(connectionAttributeStore.getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of("haa_api"));
        when(disconnectEventInput.getReasonCode()).thenReturn(Optional.of(DisconnectedReasonCode.UNSPECIFIED_ERROR));
        when(clientInformation.getClientId()).thenReturn(clientId);

        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        deviceSubscriptionCache.addSubscription(clientId, new DeviceSubscription(clientId));
        ClientLifeCycleEvents clientConnect = new ClientLifeCycleEvents();
        clientConnect.doDisconnect(disconnectEventInput);

        DeviceSubscription deviceSubscription = deviceSubscriptionCache.getSubscription(clientId);
        Assert.assertNull(deviceSubscription);
    }

    /**
     * Test case to verify the behavior when multiple connections exist for a client.
     */
    @Test
    public void testMutipleConnectionExists() {
        String clientId = "HUX1345656";
        DisconnectEventInput disconnectEventInput = Mockito.mock(DisconnectEventInput.class);
        when(disconnectEventInput.getClientInformation()).thenReturn(clientInformation);
        when(disconnectEventInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(connectionInformation.getConnectionAttributeStore()).thenReturn(connectionAttributeStore);
        when(connectionAttributeStore.getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of("haa_api"));
        when(disconnectEventInput.getReasonCode()).thenReturn(Optional.of(DisconnectedReasonCode.SESSION_TAKEN_OVER));
        when(clientInformation.getClientId()).thenReturn(clientId);

        DeviceSubscription deviceSubscription = new DeviceSubscription("HUX1345656", TWO);

        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        deviceSubscriptionCache.addSubscription(clientId, deviceSubscription);
        ClientLifeCycleEvents clientConnect = new ClientLifeCycleEvents();
        clientConnect.doDisconnect(disconnectEventInput);
        Assert.assertEquals(1, deviceSubscription.getConnectionsInfoCounter());
        deviceSubscriptionCache.removeSubscription(clientId);
    }

    /**
     * Test case to verify the behavior of the `onDisconnect` method in the `ClientLifeCycleEvents` class
     * when the health check user disconnects.
     */
    @Test
    public void testHealthCheckUserDisconnect() {
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);

        final String providedUsername = PropertyLoader.getValue(AuthConstants.HEALTH_CHECK_USER);
        final String clientId = "haa_api";

        Mockito.when(disconnectEventInput.getClientInformation()).thenReturn(clientInformation);
        when(disconnectEventInput.getClientInformation().getClientId()).thenReturn(clientId);
        when(disconnectEventInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(disconnectEventInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);

        Listener listener = Mockito.mock(Listener.class);

        when(disconnectEventInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of(providedUsername));
        when(disconnectEventInput.getConnectionInformation().getListener()).thenReturn(Optional.of(listener));
        when(listener.getPort()).thenReturn(Integer.parseInt(PropertyLoader.getValue(AuthConstants.HEALTH_CHECK_PORT)));
        final Timer timer = mock(Timer.class);
        when(registry.timer(anyString())).thenReturn(timer);
        final Timer.Context context = mock(Timer.Context.class);
        when(timer.time()).thenReturn(context);
        when(context.stop()).thenReturn(1L);
        ClientLifeCycleEvents clientConnect = new ClientLifeCycleEvents();
        clientConnect.onDisconnect(disconnectEventInput);

        verify(hivemqSinkService, times(0)).sendMsgToSink(anyString(), any(byte[].class), anyString());
    }

    /**
     * Test case to verify the behavior of the `doDisconnect` method in the `ClientLifeCycleEvents` class
     * when the cache is null.
     */
    @Test
    public void testOnDisconnectOptionalEmptyReasonCode() {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(registry);
        BDDMockito.given(Services.clientService()).willReturn(blockingClientService);
        String clientId = "haa_api";
        BDDMockito.given(Services.clientService().isClientConnected(clientId))
                .willReturn(CompletableFuture.completedFuture(false));
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);

        when(disconnectEventInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(disconnectEventInput.getClientInformation()).thenReturn(clientInformation);
        when(disconnectEventInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        when(disconnectEventInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(java.util.Optional.ofNullable(clientId));
        when(disconnectEventInput.getClientInformation().getClientId()).thenReturn(clientId);

        ClientLifeCycleEvents clientLifeCycle = new ClientLifeCycleEvents();
        clientLifeCycle.doDisconnect(disconnectEventInput);

        verify(hivemqSinkService, times(1)).sendMsgToSink(anyString(), any(byte[].class), anyString());
    }
}