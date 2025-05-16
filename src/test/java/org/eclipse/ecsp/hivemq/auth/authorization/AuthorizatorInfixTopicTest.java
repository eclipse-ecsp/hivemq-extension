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
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import org.eclipse.ecsp.hivemq.auth.authorization.stub.StubClientService;
import org.eclipse.ecsp.hivemq.auth.authorization.stub.StubModifiableDefaultPermissions;
import org.eclipse.ecsp.hivemq.auth.authorization.stub.StubTopicPermissionBuilder;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
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
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * This class is to test AuthorizatorInfixTopic class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Builders.class, Services.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class AuthorizatorInfixTopicTest {
    @Mock
    private ClientContext clientContext;
    @Mock
    InitializerInput initializerInput;
    @Mock
    private ConnectionInformation connectionInformation;
    @Mock
    private ConnectionAttributeStore connectionAttributeStore;

    Authorizer authorize;
    
    private static final int TWO = 2;
    private static final int THREE = 3;

    /**
     * Sets up the test environment before each test case.
     *
     * @throws Exception if an error occurs during setup.
     */
    @Before
    public void setUp() throws Exception {
        initMocks(this);
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
    }

    /**
     * Test case to verify the permissions obtained for a white-listed client with HAA API.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Test
    public void testGetPermissionsForWhiteListClientHaaApi() throws IOException {
        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());
        String clientId = "HUXOIDDN4HUN18";
        Authorizer.removeFromPermissionMap(clientId);
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
        ModifiableDefaultPermissions defaultPermissions = new StubModifiableDefaultPermissions();
        Mockito.when(clientContext.getDefaultPermissions()).thenReturn(defaultPermissions);

        List<TopicPermission> mqttTopicPermission = authorize.getPermissions(initializerInput);
        assertEquals("haa/harman/dev/#", mqttTopicPermission.get(0).getTopicFilter());

    }

    /**
     * Test case to verify the permissions for a white-listed client with HAA notification.
     *
     * @throws IOException if an I/O error occurs.
     */
    @Test
    public void testGetPermissionsForWhiteListClientHaaNotification() throws IOException {
        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());
        String clientId = "HUXOIDDN4HUN18";
        Authorizer.removeFromPermissionMap(clientId);
        Optional<String> providedUsername = Optional.of("harman/dev/haa_notification");
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

        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder());
        Authorizer.removeFromPermissionMap(clientId);

        List<TopicPermission> mqttTopicPermission = authorize.getPermissions(initializerInput);
        assertEquals("haa/harman/dev/#", mqttTopicPermission.get(0).getTopicFilter());
    }

    /**
     * Test case to verify the behavior of the getPermissionsForDevice method.
     * This method tests the authorization logic for a specific device and its permissions.
     * It sets up the necessary mocks and verifies that the expected permissions are returned.
     *
     * @throws IOException if an I/O error occurs during the test
     */
    @Test
    public void testGetPermissionsForDevice() throws IOException {
        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());
        String clientId = "HUXOIDDN4HUN18";
        Authorizer.removeFromPermissionMap(clientId);
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

        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder());

        Authorizer.removeFromPermissionMap(clientId);

        List<TopicPermission> mqttTopicPermission = authorize.getPermissions(initializerInput);
        assertEquals("haa/harman/dev/HUXOIDDN4HUN18/2c/events", mqttTopicPermission.get(TWO).getTopicFilter());
        assertEquals("PUBLISH", mqttTopicPermission.get(TWO).getActivity().toString());

        assertEquals("haa/harman/dev/HUXOIDDN4HUN18/2c/alerts", mqttTopicPermission.get(THREE).getTopicFilter());
        assertEquals("PUBLISH", mqttTopicPermission.get(THREE).getActivity().toString());

        assertEquals("haa/harman/dev/HUXOIDDN4HUN18/2d/config", mqttTopicPermission.get(0).getTopicFilter());
        assertEquals("SUBSCRIBE", mqttTopicPermission.get(0).getActivity().toString());
    }

    /**
     * Test case for the `getPermissionsForUse` method.
     * This method tests the functionality of the `getPermissionsForUse` method in the `Authorizer` class.
     * It verifies that the correct topic permissions are returned for a given client and username.
     *
     * @throws IOException if an I/O error occurs during the test.
     */
    @Test
    public void testGetPermissionsForUse() throws IOException {
        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());
        String clientId = "HUXOIDDN4HUN18";
        Authorizer.removeFromPermissionMap(clientId);
        Optional<String> providedUsername = Optional.of("User1");
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

        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder())
                .thenReturn(new StubTopicPermissionBuilder()).thenReturn(new StubTopicPermissionBuilder());

        Authorizer.removeFromPermissionMap(clientId);

        List<TopicPermission> mqttTopicPermission = authorize.getPermissions(initializerInput);
        assertEquals("haa/harman/dev/User1/notification", mqttTopicPermission.get(0).getTopicFilter());
        assertEquals("SUBSCRIBE", mqttTopicPermission.get(0).getActivity().toString());
    }
}
