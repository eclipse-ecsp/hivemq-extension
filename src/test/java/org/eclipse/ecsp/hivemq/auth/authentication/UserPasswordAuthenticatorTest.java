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

package org.eclipse.ecsp.hivemq.auth.authentication;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.hivemq.extension.sdk.api.async.Async;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.Listener;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extension.sdk.api.services.Services;
import okhttp3.mockwebserver.MockWebServer;
import org.eclipse.ecsp.hivemq.auth.authorization.stub.StubManagedExtensionExecutorService;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.base.AbstractAuthentication;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCache;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
import org.eclipse.ecsp.hivemq.cache.IgniteAuthInfo;
import org.eclipse.ecsp.hivemq.callbacks.MessageStoreCallbackTest;
import org.eclipse.ecsp.hivemq.d2v.DeviceToVehicleMapper;
import org.eclipse.ecsp.hivemq.d2v.VehicleInfo;
import org.eclipse.ecsp.hivemq.exceptions.VehicleDetailsNotFoundException;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.PROFILE_CHECK_DISABLED_TOPICS;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for UsernamePasswordAuthentication.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Services.class, HivemqSinkService.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class UserPasswordAuthenticatorTest {
    @Mock
    DeviceToVehicleMapper deviceToVehicleMapper;
    @Mock
    ConnectionInformation connectionInformantion;

    public MockWebServer mockWebServer;

    @Mock
    SimpleAuthInput simpleAuthInput;

    @Mock
    ConnectPacket connectPacket;

    @Mock
    ClientInformation clientInformation;

    @Mock
    SimpleAuthOutput simpleAuthOutput;

    @Mock
    private ConnectionInformation connectionInformation;

    @Mock
    private ConnectionAttributeStore connectionAttributeStore;

    @Mock
    private MetricRegistry registry;
    @Mock
    private Async<SimpleAuthOutput> asyncOutput;
    @Mock
    private HivemqSinkService hivemqSinkService;

    private UsernamePasswordAuthentication authenticator;

    static {
        File path = new File(
                MessageStoreCallbackTest.class.getClassLoader().getResource("hivemq-plugin-base.properties").getFile());
        PropertyLoader.reload(path);
    }

    /**
     * This setup method gets called before each test case and load required properties.
     *
     * @throws Exception Throws exception when property file not found.
     */
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(registry);
        PowerMockito.when(Services.extensionExecutorService()).thenReturn(new StubManagedExtensionExecutorService());
        initMocks(this);
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
    }

    
    /**
     * Test case to verify the behavior when no username is provided.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testNoUsername() throws Exception {
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);

        Optional<ByteBuffer> password = Optional
                .ofNullable(ByteBuffer.wrap("password".getBytes(StandardCharsets.UTF_8)));
        when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(Optional.empty());
        when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(password);
        authenticator = new UsernamePasswordAuthentication();
        Optional<IgniteAuthInfo> authInfo = authenticator.authenticate(simpleAuthInput);
        assertFalse(authInfo.get().isAuthenticated());
    }

    /**
     * Test case to verify the behavior when no password is provided for a user.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testNoPasswordForOtheruser() throws Exception {
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);

        when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(Optional.of("user"));
        when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(Optional.empty());
        authenticator = new UsernamePasswordAuthentication();
        authenticator.setStaticPassEnabled(true);
        Optional<IgniteAuthInfo> authInfo = authenticator.authenticate(simpleAuthInput);
        assertFalse(authInfo.get().isAuthenticated());
    }

    /**
     * This test is for super user haa_api without password.
     */
    @Test
    public void testSuperUserWhenStaticPasswordDisabled() {
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);

        final String providedUsername = "haa_api";
        final String clientId = "haa_api";
        when(simpleAuthInput.getClientInformation().getClientId()).thenReturn(clientId);
        when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(Optional.of(providedUsername));
        authenticator = new UsernamePasswordAuthentication();
        authenticator.setStaticPassEnabled(false);
        Optional<IgniteAuthInfo> authInfo = authenticator.authenticate(simpleAuthInput);
        assertFalse(authInfo.get().isAuthenticated());
    }

    /**
     * This test is for super user haa_api without password but static password
     * disabled.
     */
    @Test
    public void testSuperUserWhenStaticPasswordEnabled() {
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);

        final String providedUsername = "haa_api";
        final String clientId = "haa_api";
        Optional<ByteBuffer> password = Optional.ofNullable(ByteBuffer.wrap("12345".getBytes(StandardCharsets.UTF_8)));
        when(simpleAuthInput.getClientInformation().getClientId()).thenReturn(clientId);
        when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(Optional.of(providedUsername));
        when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(password);
        authenticator = new UsernamePasswordAuthentication();
        authenticator.setStaticPassEnabled(true);
        authenticator.setStaticPass("12345");
        Optional<IgniteAuthInfo> authInfo = authenticator.authenticate(simpleAuthInput);
        assertTrue(authInfo.get().isAuthenticated());
    }

    /**
     * Test case to verify the behavior of the `onConnect` method when a valid health check user connects.
     * It mocks the necessary input and output objects, sets up the required conditions, and then calls 
     * the `onConnect` method of the `UsernamePasswordAuthentication` class.
     * Finally, it verifies that the authentication is successful by checking if the `authenticateSuccessfully` 
     * method of the output object is called exactly once.
     */
    @Test
    public void testOnConnectValidHealthCheckUser() {
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);

        final String providedUsername = PropertyLoader.getValue(AuthConstants.HEALTH_CHECK_USER);
        final String clientId = "haa_api";

        Listener listener = Mockito.mock(Listener.class);

        when(simpleAuthInput.getClientInformation().getClientId()).thenReturn(clientId);
        when(simpleAuthInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(simpleAuthInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        when(simpleAuthInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of(providedUsername));
        when(simpleAuthInput.getConnectionInformation().getListener()).thenReturn(Optional.of(listener));
        when(listener.getPort()).thenReturn(Integer.parseInt(PropertyLoader.getValue(AuthConstants.HEALTH_CHECK_PORT)));
        when(simpleAuthOutput.async(Mockito.any(), Mockito.any())).thenReturn(asyncOutput);
        when(asyncOutput.getOutput()).thenReturn(simpleAuthOutput);
        PowerMockito.when(Services.metricRegistry()).thenReturn(registry);
        final Timer timer = mock(Timer.class);
        when(registry.timer(anyString())).thenReturn(timer);
        final Timer.Context context = mock(Timer.Context.class);
        when(timer.time()).thenReturn(context);
        when(context.stop()).thenReturn(1L);
        authenticator = new UsernamePasswordAuthentication();
        authenticator.onConnect(simpleAuthInput, simpleAuthOutput);

        verify(asyncOutput.getOutput(), times(1)).authenticateSuccessfully();
    }

    /**
     * Test case to verify the behavior of the `onConnect` method in the case of an invalid health check user.
     * It mocks the necessary input and output objects and sets up the required conditions for the test.
     * The provided username and client ID are set to "haa_api".
     * The password is set to "12345".
     * The method verifies that the `onConnect` method does not authenticate the user successfully.
     */
    @Test
    public void testOnConnectInValidHealthCheckUser() {
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);

        final String providedUsername = "health";
        final String clientId = "haa_api";

        Optional<ByteBuffer> password = Optional.ofNullable(ByteBuffer.wrap("12345".getBytes(StandardCharsets.UTF_8)));

        when(simpleAuthInput.getClientInformation().getClientId()).thenReturn(clientId);
        when(simpleAuthInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(simpleAuthInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        when(simpleAuthInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of(providedUsername));
        when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(Optional.of(providedUsername));
        when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(password);
        when(simpleAuthOutput.async(Mockito.any(), Mockito.any())).thenReturn(asyncOutput);
        when(asyncOutput.getOutput()).thenReturn(simpleAuthOutput);
        PowerMockito.when(Services.metricRegistry()).thenReturn(registry);
        final Timer timer = mock(Timer.class);
        when(registry.timer(anyString())).thenReturn(timer);
        final Timer.Context context = mock(Timer.Context.class);
        when(timer.time()).thenReturn(context);
        when(context.stop()).thenReturn(1L);

        authenticator = new UsernamePasswordAuthentication();

        authenticator.setStaticPassEnabled(true);
        authenticator.setStaticPass("12345");

        authenticator.onConnect(simpleAuthInput, simpleAuthOutput);
        verify(asyncOutput.getOutput(), times(0)).authenticateSuccessfully();
    }

    /**
     * Test case to verify the behavior of authentication when the authentication fails.
     * 
     * <p>This test method sets up the necessary mock objects and verifies that the authentication fails
     * when the provided username and password do not match the expected values.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testOnConnectFailAuthentication() {
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(registry);
        PowerMockito.when(Services.extensionExecutorService()).thenReturn(new StubManagedExtensionExecutorService());

        final String providedUsername = "haa_api";
        final String clientId = "haa_api";

        Optional<ByteBuffer> password = Optional.ofNullable(ByteBuffer.wrap("12345".getBytes(StandardCharsets.UTF_8)));

        when(simpleAuthInput.getClientInformation().getClientId()).thenReturn(clientId);
        when(simpleAuthInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(simpleAuthInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        when(simpleAuthInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of(providedUsername));
        when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(Optional.of(providedUsername));
        when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(password);

        authenticator = new UsernamePasswordAuthentication();

        authenticator.setStaticPassEnabled(true);
        authenticator.setStaticPass("wrong pass");

        Optional<IgniteAuthInfo> authInfo = authenticator.authenticate(simpleAuthInput);
        assertFalse(authInfo.get().isAuthenticated());
    }

    /**
     * Test case to verify the behavior of the `testOnConnectWhiteListUser` method.
     * This method tests the authentication process for a white-listed user during connection.
     * It mocks the necessary dependencies and verifies that the authentication is successful.
     * It also checks if the device subscription is created correctly for the authenticated user.
     *
     * @throws Exception if an error occurs during the test.
     */
    @Test
    public void testOnConnectWhiteListUser() throws Exception {
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);
        when(simpleAuthInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(simpleAuthInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        String clientId = "haa_api";
        when(simpleAuthInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(java.util.Optional.ofNullable(clientId));
        when(simpleAuthInput.getClientInformation().getClientId()).thenReturn(clientId);

        getAfterAuthSuccessfulMethod().invoke(new UsernamePasswordAuthentication(), simpleAuthInput);

        DeviceSubscription ds = DeviceSubscriptionCacheFactory.getInstance().getSubscription(clientId);
        Assert.assertNotNull(ds);
        Assert.assertEquals(clientId, ds.getVehicleId());
    }

    /**
     * Test case to verify the behavior when the vehicle ID is not found during the connect process.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test(expected = VehicleDetailsNotFoundException.class)
    public void testOnConnectVehicleIdNotFound() throws Exception {
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);
        when(simpleAuthInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(simpleAuthInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        String clientId = "test-device";
        when(simpleAuthInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(java.util.Optional.ofNullable(clientId));
        when(simpleAuthInput.getClientInformation().getClientId()).thenReturn(clientId);

        Mockito.when(deviceToVehicleMapper.getVehicleId(clientId, connectionAttributeStore))
            .thenReturn(CompletableFuture.completedFuture(null));
        AbstractAuthentication abstractionAuth = new UsernamePasswordAuthentication();
        abstractionAuth.setDeviceToVehicleMapper(deviceToVehicleMapper);
        final Object object = getAfterAuthSuccessfulMethod().invoke(abstractionAuth, simpleAuthInput);
        try {
            ((CompletableFuture<Void>) object).get();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof VehicleDetailsNotFoundException) {
                throw (VehicleDetailsNotFoundException) e.getCause();
            }
        }
    }

    
    /**
     * Test case to verify that connection is allowed without a vehicle ID and username.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testAllowConnectWithoutVehicleIdAndUserName() throws Exception {
        PropertyLoader.getProperties().put(PROFILE_CHECK_DISABLED_TOPICS, "CCHK");

        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);
        when(simpleAuthInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(simpleAuthInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        String clientId = "device";
        when(simpleAuthInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(java.util.Optional.ofNullable(clientId));
        when(simpleAuthInput.getClientInformation().getClientId()).thenReturn(clientId);

        Mockito.when(deviceToVehicleMapper.getVehicleId(clientId, connectionAttributeStore))
            .thenReturn(CompletableFuture.completedFuture(null));

        AbstractAuthentication abstractionAuth = new UsernamePasswordAuthentication();
        abstractionAuth.setDeviceToVehicleMapper(deviceToVehicleMapper);
        getAfterAuthSuccessfulMethod().invoke(abstractionAuth, simpleAuthInput);

        PropertyLoader.getProperties().remove(PROFILE_CHECK_DISABLED_TOPICS);
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        assertNotNull(deviceSubscriptionCache);
        DeviceSubscription ds = deviceSubscriptionCache.getSubscription(clientId);
        assertNotNull(ds);
        assertNull(ds.getVehicleId());
    }

    /**
     * Retrieves the private method "afterAuthSuccessful" from the AbstractAuthentication class.
     * This method is used to perform actions after a successful authentication.
     *
     * @return The retrieved Method object.
     * @throws NoSuchMethodException If the method "afterAuthSuccessful" is not found in the AbstractAuthentication 
     *      class.
     */
    private Method getAfterAuthSuccessfulMethod() throws NoSuchMethodException {
        Method method = AbstractAuthentication.class.getDeclaredMethod("afterAuthSuccessful", SimpleAuthInput.class);
        method.setAccessible(true);
        return method;
    }

}