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

import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.client.ClientContext;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.InitializerInput;
import com.hivemq.extension.sdk.api.services.builder.Builders;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for AuthorizorForTenant.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Builders.class)
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class AuthorizorForTenantTest {
    @Mock
    private ClientContext clientContext;
    @Mock
    InitializerInput initializerInput;
    @Mock
    private ConnectionInformation connectionInformation;
    @Mock
    private ConnectionAttributeStore connectionAttributeStore;

    Authorizer authorize;

    private static final String FILE_PATH = "src/test/resources/";

    /**
     * This setup method gets called before each test case and load required properties.
     */
    @Before
    public void setUp() throws Exception {
        initMocks(this);
        PropertyLoader.getProperties(FILE_PATH + "hivemq-plugin-base-tenant.properties");
        authorize = new Authorizer();
        authorize.setTopicFormatter(new IgniteTopicFormatter());
    }

    /**
     * Test case to verify the topic permissions for client data.
     * It checks if the expected topic filters are present in the list of authorized topic permissions.
     */
    @Test
    public void should_returnTopicPermisionsForClientData() {
        String clientId = "HUXOIDDN4HUN18";
        Authorizer.removeFromPermissionMap(clientId);
        Optional<String> providedUsername = Optional.of("devices/dongle/user");
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

        List<TopicPermission> mqttTopicPermission = authorize.authorize(initializerInput);

        List<String> topics = new ArrayList<String>();
        mqttTopicPermission.forEach(x -> topics.add(x.getTopicFilter()));

        assertTrue(topics.contains("haa/harman/dev/" + clientId + "/2c/events"));
        assertTrue(topics.contains("haa/harman/dev/" + clientId + "/2c/alerts"));
        assertTrue(topics.contains("haa/harman/dev/user/2d/config"));
    }
}
