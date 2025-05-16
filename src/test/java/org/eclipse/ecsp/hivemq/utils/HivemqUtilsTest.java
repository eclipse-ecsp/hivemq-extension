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

package org.eclipse.ecsp.hivemq.utils;

import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.Listener;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import org.apache.commons.codec.binary.Base64;
import org.eclipse.ecsp.analytics.stream.base.utils.ObjectUtils;
import org.eclipse.ecsp.cache.redis.IgniteCacheRedisImpl;
import org.eclipse.ecsp.cache.redis.RedisConfig;
import org.eclipse.ecsp.hivemq.auth.authentication.TestSslClientCertificateImpl;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.callbacks.SubscriptionStatusHandler;
import org.json.simple.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Optional;
import java.util.Properties;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for HivemqUtils.
 */
public class HivemqUtilsTest {

    private static final String HEALTH_USER = "health";
    private static final String HEALTH_PORT = "1888";
    private static final String INTERNAL_SERVICE_PORT = "1883";
    Properties prop = new Properties();
    @Mock
    ConnectionInformation clientData;

    @Mock
    ConnectionAttributeStore connectionAttributeStore;

    @Mock
    SimpleAuthInput simpleAuthInput;

    @Mock
    ConnectPacket connectPacket;
    
    private static final int TEN = 10;
    private static final int DAYS_IN_YEAR = 365;

    /**
     * This setup method gets called before each test case and load required properties.
     *
     * @throws Exception throws when not able to read property file
     */
    @Before
    public void setUp() throws Exception {
        initMocks(this);
        prop = PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        prop.put(AuthConstants.JWT_PUBLIC_KEY_PATH, "src/test/resources/wso_test_pk.txt");

    }

    /**
        * Test case to verify the functionality of the test method.
        * It checks if the public key is not null after reading it from the specified file.
        *
        * @throws Exception if an error occurs during the test
        */
    @Test
    public void test() throws Exception {
        assertNotNull(HivemqUtils.readPublickey("src/test/resources/wso_test_pk.txt"));
    }

    /**
     * Test case to verify the functionality of the createEvent method in HivemqUtils class.
     */
    @Test
    public void testcreateEvent() {
        String deviceId = "DeviceId";
        String connectionStatus = "connectionStatus";
        JSONObject object = HivemqUtils.createEvent(deviceId, connectionStatus);
        Assert.assertEquals(deviceId, object.get(EventMetadataConstants.PDID));
    }

    /**
        * Test case to verify the behavior of the isServiceRequest method.
        * It checks if the method returns true when the input parameters match the expected values.
        */
    @Test
    public void testIsServiceRequest() {
        Assert.assertEquals(true, HivemqUtils.isServiceRequest("", "ser-ro", "SER-"));
    }

    /**
     * Test case to verify the behavior of the isHealthCheckUser method when the health check user is present.
     * It sets up the necessary test data and mocks the required dependencies to simulate the scenario.
     * It asserts that the isHealthCheckUser method returns true when the health check user is found in the client data.
     */
    @Test
    public void testIsHealthCheckUserForSuccess() {
        prop.put(AuthConstants.HEALTH_CHECK_PORT, HEALTH_PORT);
        prop.put(AuthConstants.HEALTH_CHECK_USER, HEALTH_USER);
        Mockito.when(clientData.getConnectionAttributeStore()).thenReturn(connectionAttributeStore);
        Mockito.when(clientData.getConnectionAttributeStore().getAsString(AuthConstants.USERNAME))
                .thenReturn(Optional.of(HEALTH_USER));
        Listener listener = Mockito.mock(Listener.class);
        Mockito.when(listener.getPort()).thenReturn(Integer.parseInt(HEALTH_PORT));
        Mockito.when(clientData.getListener()).thenReturn(Optional.of(listener));
        Assert.assertEquals(true, HivemqUtils.isHealthCheckUser(prop, clientData));
    }

    /**
     * Test case to verify the behavior of the isHealthCheckUser method when the health check user fails.
     */
    @Test
    public void testIsHealthCheckUserForFailed() {
        prop.put(AuthConstants.HEALTH_CHECK_PORT, HEALTH_PORT);
        prop.put(AuthConstants.HEALTH_CHECK_USER, HEALTH_USER);
        Mockito.when(clientData.getConnectionAttributeStore()).thenReturn(connectionAttributeStore);
        Mockito.when(clientData.getConnectionAttributeStore().getAsString(AuthConstants.USERNAME))
                .thenReturn(Optional.of(HEALTH_USER));
        Listener listener = Mockito.mock(Listener.class);
        Mockito.when(listener.getPort()).thenReturn(Integer.parseInt(HEALTH_PORT) + TEN);
        Mockito.when(clientData.getListener()).thenReturn(Optional.of(listener));
        Assert.assertEquals(false, HivemqUtils.isHealthCheckUser(prop, clientData));
    }

    /**
     * Test case to verify the behavior of the isHealthCheckUser method when the health check user is not found.
     * It sets up the necessary test data and mocks the required dependencies to simulate the scenario.
     * The method asserts that the isHealthCheckUser method returns false as expected.
     */
    @Test
    public void testIsHealthCheckUserForFailedUser() {
        prop.put(AuthConstants.HEALTH_CHECK_PORT, HEALTH_PORT);
        prop.put(AuthConstants.HEALTH_CHECK_USER, HEALTH_USER + "unknown");
        Mockito.when(clientData.getConnectionAttributeStore()).thenReturn(connectionAttributeStore);
        Mockito.when(clientData.getConnectionAttributeStore().getAsString(AuthConstants.USERNAME))
                .thenReturn(Optional.of(HEALTH_USER));
        Listener listener = Mockito.mock(Listener.class);
        Mockito.when(listener.getPort()).thenReturn(Integer.parseInt(HEALTH_PORT));
        Mockito.when(clientData.getListener()).thenReturn(Optional.of(listener));
        Assert.assertEquals(false, HivemqUtils.isHealthCheckUser(prop, clientData));
    }

    /**
     * Test case for the {@link HivemqUtils#createCommcheckAckResponseEvent(String)} method.
     * Verifies that the created event contains the correct device ID.
     */
    @Test
    public void testCreateCommcheckAckResponseEvent() {
        String deviceId = "DeviceId";
        JSONObject object = HivemqUtils.createCommcheckAckResponseEvent(deviceId);
        Assert.assertEquals(deviceId, object.get(EventMetadataConstants.PDID));
    }

    /**
     * Test case to verify the correctness of the getCnFromCert method when the certificate is valid.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testGetCnFromCertValid() throws Exception {
        TestSslClientCertificateImpl sslClientCertImpl = new TestSslClientCertificateImpl();
        sslClientCertImpl.setCommonName("HAA_PROVISIONING");
        X509Certificate sslCert = sslClientCertImpl.getCertificate(DAYS_IN_YEAR, "HAA_PROVISIONING");
        String cnName = HivemqUtils.getCnFromCert(new String(Base64.encodeBase64(sslCert.getEncoded())));
        Assert.assertEquals("HAA_PROVISIONING", cnName);
    }

    /**
        * Test case to verify the behavior of getCnFromCert method when an invalid certificate string is provided.
        * It expects a CertificateException to be thrown.
        *
        * @throws CertificateException if an error occurs while processing the certificate
        */
    @Test(expected = CertificateException.class)
    public void testGetCnFromCertInValid() throws CertificateException {
        HivemqUtils.getCnFromCert("invalid certificate String");
    }

    /**
     * Test case to verify the behavior of getPropertyDoubleValue method when the property value is invalid.
     */
    @Test
    public void testGetPropertyDoubleValueInValid() {
        Double doubleVal = HivemqUtils.getPropertyDoubleValue("testDoubleValue");
        Assert.assertEquals(Double.valueOf(0.0), doubleVal);
    }

    /**
     * This method loads and initialize redis property for tests.
     */
    public static void setRedisClient() {
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        PropertyLoader.loadRedisProperties("src/test/resources");
        RedisConfig redisConfig = new RedisConfig();
        RedissonClient redissonClient = redisConfig.builder().build(PropertyLoader.getRedisPropertiesMap());
        ObjectUtils.requireNonNull(redissonClient, "Unable to initialize Redisson client. Aborting..");

        IgniteCacheRedisImpl igniteCacheRedisImpl = new IgniteCacheRedisImpl();
        igniteCacheRedisImpl.setRedissonClient(redissonClient);
        SubscriptionStatusHandler.setIgniteCacheRedisImpl(igniteCacheRedisImpl);
    }

    /**
        * Test case to verify the behavior of the isInternalServiceClient method.
        * It checks if the given client is an internal service client.
        */
    @Test
    public void testIsInternalServiceClient() {
        Listener listener = Mockito.mock(Listener.class);
        Mockito.when(listener.getPort()).thenReturn(Integer.parseInt(INTERNAL_SERVICE_PORT));
        Mockito.when(clientData.getListener()).thenReturn(Optional.of(listener));
        assertTrue(HivemqUtils.isInternalServiceClient("haa_api", clientData));
    }

    /**
     * Test case for the getPropertyIntValue method in the HivemqUtils class.
     * It verifies that the method returns the correct integer value for the specified property key.
     */
    @Test
    public void testGetPropertyIntValue() {
        prop.put(AuthConstants.HEALTH_CHECK_PORT, HEALTH_PORT);
        assertEquals(Integer.parseInt(HEALTH_PORT), 
                HivemqUtils.getPropertyIntValue("health.check.port"));
    }

    /**
     * Test case for the getUsername method of the HivemqUtils class.
     * It verifies that the method returns the correct username from the ConnectPacket.
     */
    @Test
    public void testGetUsername() {
        Mockito.when(connectPacket.getUserName()).thenReturn(Optional.of("haa_api"));
        assertEquals("haa_api", HivemqUtils.getUserName(connectPacket));
    }

    /**
     * Test case for the getPassword method in the HivemqUtils class.
     * It verifies that the method returns the correct password from the ConnectPacket.
     */
    @Test
    public void testGetPassword() {
        Optional<ByteBuffer> password = Optional.ofNullable(ByteBuffer.wrap("12345".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(connectPacket.getPassword()).thenReturn(password);
        assertEquals("12345", HivemqUtils.getPassword(connectPacket));
    }
}
