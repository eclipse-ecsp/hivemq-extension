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
import com.hivemq.extension.sdk.api.client.parameter.ClientTlsInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.InitializerInput;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import org.eclipse.ecsp.hivemq.auth.authentication.TestSslClientCertificateImpl;
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
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import static org.junit.Assert.assertEquals;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for CertificateBasedAuthorizor.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Builders.class)
@PowerMockIgnore({ "javax.security.auth.x500.X500Principal", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*",
    "javax.management.*" })
public class CertificateBasedAuthorizorTest {
    @Mock
    private ClientContext clientContext;
    @Mock
    InitializerInput initializerInput;
    @Mock
    private ConnectionInformation connectionInformation;
    @Mock
    private ConnectionAttributeStore connectionAttributeStore;
    @Mock
    ClientTlsInformation clientTlsInformation;

    CertificateBasedAuthorizer certificateBasedAuthorizer;

    Properties properties;
    private static final int DAYS = 365;

    /**
     * Sets up the test environment before each test case.
     *
     * @throws Exception if an error occurs during setup.
     */
    @Before
    public void setUp() throws Exception {
        initMocks(this);
        properties = PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
    }

    /**
     * Test case to verify the authorization when the client certificate does not exist.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testAuthorizeClientCertificateNotExists() throws IOException {
        certificateBasedAuthorizer = new CertificateBasedAuthorizer();
        certificateBasedAuthorizer.setTopicFormatter(new IgniteTopicFormatter());
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

        Authorizer.removeFromPermissionMap(clientId);

        List<TopicPermission> mqttTopicPermission = certificateBasedAuthorizer.authorize(initializerInput);
        assertEquals("haa/harman/dev/#", mqttTopicPermission.get(0).getTopicFilter());
    }

    /**
     * Test case to verify the authorization when the client certificate is valid and it is a service request.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testAuthorizeWhenClientCertificateValidIsServiceRequet() throws Exception {
        certificateBasedAuthorizer = new CertificateBasedAuthorizer();
        certificateBasedAuthorizer.setTopicFormatter(new IgniteTopicFormatter());
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

        TestSslClientCertificateImpl sslClientCertImpl = new TestSslClientCertificateImpl();
        X509Certificate sslCert = sslClientCertImpl.getCertificate(DAYS, "HAA_PROVISIONING");

        Mockito.when(initializerInput.getConnectionInformation().getClientTlsInformation())
                .thenReturn(Optional.of(clientTlsInformation));
        Mockito.when(initializerInput.getConnectionInformation().getClientTlsInformation().get().getClientCertificate())
                .thenReturn(Optional.of(sslCert));

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder());

        Authorizer.removeFromPermissionMap(clientId);

        List<TopicPermission> mqttTopicPermission = certificateBasedAuthorizer.authorize(initializerInput);
        assertEquals("haa/harman/dev/#", mqttTopicPermission.get(0).getTopicFilter());
    }

    /**
     * Test case to verify the authorization when the client certificate is valid and it is not a service request.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testAuthorizeWhenClientCertificateValidIsNotServiceRequest() throws Exception {
        certificateBasedAuthorizer = new CertificateBasedAuthorizer();
        certificateBasedAuthorizer.setTopicFormatter(new IgniteTopicFormatter());
        certificateBasedAuthorizer.serviceCertificatePrefix = "test";

        String clientId = "HUXOIDDN4HUN18";
        Authorizer.removeFromPermissionMap(clientId);
        Mockito.when(initializerInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        Optional<String> providedUsername = Optional.of("harman/dev/haa_api");
        Mockito.when(initializerInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        Mockito.when(initializerInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(providedUsername);

        TestSslClientCertificateImpl sslClientCertImpl = new TestSslClientCertificateImpl();
        sslClientCertImpl.setCommonName(null);
        X509Certificate sslCert = sslClientCertImpl.getCertificate(DAYS, "HAA_PROVISIONING");

        Mockito.when(initializerInput.getConnectionInformation().getClientTlsInformation())
                .thenReturn(Optional.of(clientTlsInformation));
        Mockito.when(initializerInput.getConnectionInformation().getClientTlsInformation().get().getClientCertificate())
                .thenReturn(Optional.of(sslCert));

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder());

        Authorizer.removeFromPermissionMap(clientId);

        List<TopicPermission> mqttTopicPermission = certificateBasedAuthorizer.authorize(initializerInput);
        assertEquals("haa/harman/dev/HUXOIDDN4HUN18/2c/alerts", mqttTopicPermission.get(0).getTopicFilter());
    }
}
