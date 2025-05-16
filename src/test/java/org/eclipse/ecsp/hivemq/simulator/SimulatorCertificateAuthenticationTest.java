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

package org.eclipse.ecsp.hivemq.simulator;

import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ClientTlsInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.Listener;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.ecsp.hivemq.auth.authentication.TestSslClientCertificateImpl;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Properties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for SimulatorCertificateAuthentication.
 */
public class SimulatorCertificateAuthenticationTest {
    private SimulatorCertificateAuthentication certAuthentication;
    @Mock
    SimpleAuthInput simpleAuthInput;

    @Mock
    private Listener listener;

    @Mock
    ConnectionInformation connectionInformation;

    @Mock
    ConnectPacket connectPacket;

    @Mock
    ClientTlsInformation clientTlsInformation;

    @Mock
    X509Certificate certificate;

    @Mock
    ClientInformation clientInformation;

    private Properties props;
    private static final int PORT = 1883;
    private static final int DAYS = 365;
    private static final int MINUS_ONE = -1;

    /**
     * This setup method gets called before each test case and load required properties.
     */
    @Before
    public void setup() {
        initMocks(this);
        try {
            props = PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
            certAuthentication = new SimulatorCertificateAuthentication();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Testing authenticate() when tls info is not present.
     */
    @Test
    public void testAuthenticateWhenTlsDisabled() throws Exception {

        Mockito.when(simpleAuthInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getConnectionInformation().getClientTlsInformation())
                .thenReturn(Optional.of(clientTlsInformation));
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);

        Optional<ByteBuffer> password = Optional
                .ofNullable(ByteBuffer.wrap("testPasswd".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(simpleAuthInput.getConnectionInformation().getClientTlsInformation().get().getClientCertificate())
                .thenReturn(Optional.empty());
        Mockito.when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(Optional.of("testUser"));
        Mockito.when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(password);
        Mockito.when(simpleAuthInput.getConnectionInformation().getListener()).thenReturn(Optional.of(listener));
        Mockito.when(listener.getPort()).thenReturn(PORT);
        assertFalse(certAuthentication.authenticate(simpleAuthInput).get().isAuthenticated());
    }

    /**
     * Testing authenticate() when clientData and SslClinetCertificate are present
     * but isNotAValidCertificate. Expect AuthenticationException
     */

    @Test
    public void testAuthenticateWhenInvalidSslCertifidate() throws Exception {
        Mockito.when(simpleAuthInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getConnectionInformation().getClientTlsInformation())
                .thenReturn(Optional.of(clientTlsInformation));
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);

        Mockito.when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(Optional.of("testUser"));
        Optional<ByteBuffer> password = Optional
                .ofNullable(ByteBuffer.wrap("testPasswd".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(password);

        TestSslClientCertificateImpl sslClientCertImpl = new TestSslClientCertificateImpl();
        sslClientCertImpl.setDays(DAYS);
        Optional<X509Certificate> sslCert = sslClientCertImpl.getClientCertificate();
        Mockito.when(simpleAuthInput.getConnectionInformation().getClientTlsInformation().get().getClientCertificate())
                .thenReturn(sslCert);
        assertFalse(certAuthentication.authenticate(simpleAuthInput).get().isAuthenticated());
    }

    /**
     * Testing authenticate() using a SslClinetCertificate when clientData and
     * certificate are present but the certificate is already expired. Expect
     * Response IgniteAuthInfo is present. However IgniteAuthInfo.isAuthenticated is
     * false
     */
    @Test
    public void testAuthenticateWhenInvalidExpDateInCert() throws Exception {

        Mockito.when(simpleAuthInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getConnectionInformation().getClientTlsInformation())
                .thenReturn(Optional.of(clientTlsInformation));
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);

        Mockito.when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(Optional.of("testUser"));
        Optional<ByteBuffer> password = Optional
                .ofNullable(ByteBuffer.wrap("testPasswd".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(password);

        TestSslClientCertificateImpl sslClientCertImpl = new TestSslClientCertificateImpl();
        sslClientCertImpl.setCommonName("GlobalRootCA");
        sslClientCertImpl.setDays(MINUS_ONE);
        Optional<X509Certificate> sslCert = sslClientCertImpl.getClientCertificate();
        Mockito.when(simpleAuthInput.getConnectionInformation().getClientTlsInformation().get().getClientCertificate())
                .thenReturn(sslCert);
        assertFalse(certAuthentication.authenticate(simpleAuthInput).get().isAuthenticated());
    }

    /**
     * Testing authenticate() when clientData certificate is present and valid but
     * it is not a isServiceCertificateValid since its commonName is not present in
     * the serviceCertificateCommonNames list Expect Response IgniteAuthInfo is
     * present. However IgniteAuthInfo.isAuthenticated is false
     */
    @Test
    public void testAuthenticateWhenCertIsNotServiceCertificate() throws Exception {
        Mockito.when(simpleAuthInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getConnectionInformation().getClientTlsInformation())
                .thenReturn(Optional.of(clientTlsInformation));
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);
        Mockito.when(simpleAuthInput.getConnectionInformation().getClientTlsInformation().get().getClientCertificate())
                .thenReturn(Optional.empty());

        Mockito.when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(Optional.of("testUser"));
        Optional<ByteBuffer> password = Optional
                .ofNullable(ByteBuffer.wrap("testPasswd".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(password);

        TestSslClientCertificateImpl sslClientCertImpl = new TestSslClientCertificateImpl();
        sslClientCertImpl.setCommonName("GlobalRootCA");
        sslClientCertImpl.setDays(DAYS);
        Optional<X509Certificate> sslCert = sslClientCertImpl.getClientCertificate();
        Mockito.when(simpleAuthInput.getConnectionInformation().getClientTlsInformation().get().getClientCertificate())
                .thenReturn(sslCert);
        assertEquals(false, certAuthentication.authenticate(simpleAuthInput).get().isAuthenticated());
    }

    /**
     * Test case to verify the authentication process when a valid certificate is provided.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testAuthenticateWhenValidCert() throws Exception {
        Mockito.when(simpleAuthInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getConnectionInformation().getClientTlsInformation())
                .thenReturn(Optional.of(clientTlsInformation));
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);
        Mockito.when(simpleAuthInput.getConnectionInformation().getClientTlsInformation().get().getClientCertificate())
                .thenReturn(Optional.empty());

        Mockito.when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(Optional.of("testUser"));
        Optional<ByteBuffer> password = Optional
                .ofNullable(ByteBuffer.wrap("testPasswd".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(password);

        TestSslClientCertificateImpl sslClientCertImpl = new TestSslClientCertificateImpl();
        // This commonName is brought from the service.certificate.common.names
        // propery in hivemq-plugin-base.properties
        sslClientCertImpl.setCommonName("HAA_PROVISIONING");
        sslClientCertImpl.setDays(DAYS);
        Optional<X509Certificate> sslCert = sslClientCertImpl.getClientCertificate();
        Mockito.when(simpleAuthInput.getConnectionInformation().getClientTlsInformation().get().getClientCertificate())
                .thenReturn(sslCert);
        assertEquals(true, certAuthentication.authenticate(simpleAuthInput).get().isAuthenticated());
    }

    /**
     * New way TWo way TLS testing for service.
     *
     * @throws Exception - if not able to read property file
     */
    @Test
    public void testAuthenticateWhenValidCertForNew2WayTls_Success_Service() throws Exception {
        props.remove("service.certificate.common.name.prefix");
        Mockito.when(simpleAuthInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getConnectionInformation().getClientTlsInformation()).thenReturn(Optional.empty());
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);

        Mockito.when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(Optional.of("testUser"));
        Mockito.when(simpleAuthInput.getConnectPacket().getPassword())
                .thenReturn(generatePassword(DAYS, "HAA_PROVISIONING"));
        SimulatorCertificateAuthentication certificateAuthentication = new SimulatorCertificateAuthentication();
        assertEquals(true, certificateAuthentication.authenticate(simpleAuthInput).get().isAuthenticated());
    }

    /**
     * New way TWo way TLS testing for device.
     *
     * @throws Exception - if not able to read property file
     */
    @Test
    public void testAuthenticateWhenValidCertForNew2WayTls_Success_device() throws Exception {
        props.put("service.certificate.common.name.prefix", "TEST");
        Mockito.when(simpleAuthInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getConnectionInformation().getClientTlsInformation()).thenReturn(Optional.empty());
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);
        Mockito.when(simpleAuthInput.getClientInformation().getClientId()).thenReturn("twowaytls");

        Mockito.when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(generatePassword(DAYS, "twowaytls"));
        SimulatorCertificateAuthentication certObj = new SimulatorCertificateAuthentication();
        assertEquals(true, certObj.authenticate(simpleAuthInput).get().isAuthenticated());
        props.remove("service.certificate.common.name.prefix");
    }

    /**
     * New way TWo way TLS testing for invalid certificate.
     *
     * @throws Exception - if not able to read property file
     */
    @Test
    public void testAuthenticateWhenValidCertForNew2WayTls_InvalidCertificate() throws Exception {
        Mockito.when(simpleAuthInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getConnectionInformation().getClientTlsInformation()).thenReturn(Optional.empty());
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);
        Mockito.when(simpleAuthInput.getClientInformation().getClientId()).thenReturn("twowaytls");

        Optional<ByteBuffer> password = Optional.of(
                ByteBuffer.wrap((generatePassword(DAYS, "twowaytls").get().array().toString() + "invalid").getBytes()));

        Mockito.when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(password);
        props.put("service.certificate.common.name.prefix", "TEST");
        SimulatorCertificateAuthentication certificateAuthentication = new SimulatorCertificateAuthentication();
        assertEquals(false, certificateAuthentication.authenticate(simpleAuthInput).get().isAuthenticated());
        props.remove("service.certificate.common.name.prefix");
    }

    /**
     * Generates a password as an optional ByteBuffer based on the specified number of days and common name.
     *
     * @param days        the number of days for which the certificate is valid
     * @param commonName  the common name for the certificate
     * @return an optional ByteBuffer containing the generated password
     * @throws Exception if an error occurs during password generation
     */
    private Optional<ByteBuffer> generatePassword(int days, String commonName) throws Exception {
        TestSslClientCertificateImpl certImpl = new TestSslClientCertificateImpl();
        certImpl.setDays(DAYS);
        certImpl.setCommonName(commonName);
        String password = new String(Base64.encodeBase64(certImpl.getCertificate(days, commonName).getEncoded()));
        Optional<ByteBuffer> byteBufferPassword = Optional
                .ofNullable(ByteBuffer.wrap(password.getBytes(StandardCharsets.UTF_8)));
        return byteBufferPassword;
    }

}
