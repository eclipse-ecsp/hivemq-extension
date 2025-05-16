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
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extension.sdk.api.services.Services;
import com.nimbusds.jwt.JWTClaimsSet;
import org.eclipse.ecsp.hivemq.auth.authorization.stub.StubClientService;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.json.simple.JSONArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for JwtAuthentication.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Services.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class JwtAuthenticationTest {

    private static Logger logger = LoggerFactory.getLogger(JwtAuthentication.class);
    JwtAuthentication authentication;

    @Mock
    SimpleAuthInput simpleAuthInput;

    @Mock
    ConnectPacket connectPacket;

    @Mock
    ClientInformation clientInformation;

    @Mock
    private MetricRegistry registry;
    private static final int SIXTY_SECONDS = 60000;

    /**
     * This setup method gets called before each test case and load required properties.
     */
    @Before
    public void setup() {
        initMocks(this);
        try {
            PowerMockito.mockStatic(Services.class);
            PowerMockito.when(Services.clientService()).thenReturn(new StubClientService());
            PowerMockito.when(Services.metricRegistry()).thenReturn(registry);
            Properties props = PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
            props.put(AuthConstants.JWT_PUBLIC_KEY_PATH, "src/test/resources/wso_test_pk.txt");
            authentication = new JwtAuthentication();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test case to verify if the JWT client ID matches the expected client ID.
     */
    @Test
    public void testJwtClientIdMatch() {
        String clientId = "deviceId";
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("admin")
                .issuer("https://${WSO2_PROXY_HOSTNAME}:443/oauth2/token").audience("KU6Sp4fIownMf3RA0yiDk25flYga")
                .expirationTime(new Date(System.currentTimeMillis() + SIXTY_SECONDS))
                .notBeforeTime(new Date(System.currentTimeMillis() - SIXTY_SECONDS))
                .claim(AuthConstants.CLAIM_SET_AZP, "deviceId").build();

        Assert.assertTrue(authentication.validateIdentity(claimsSet, clientId, null));

    }

    /**
     * Test case to verify if the JWT audience matches the expected client ID.
     */
    @Test
    public void testJwtAudMatch() {
        String clientId = "deviceId";
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("admin")
                .issuer("https://${WSO2_PROXY_HOSTNAME}:443/oauth2/token").audience(clientId)
                .expirationTime(new Date(System.currentTimeMillis() + SIXTY_SECONDS))
                .notBeforeTime(new Date(System.currentTimeMillis() - SIXTY_SECONDS)).build();

        Assert.assertTrue(authentication.validateIdentity(claimsSet, clientId, null));
    }

    /**
     * Test case to verify if the JWT username matches the expected value.
     */
    @Test
    public void testJwtUserNameMatch() {
        String clientId = "randomDeviceId";
        String userName = "user1";
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject(userName)
                .issuer("https://${WSO2_PROXY_HOSTNAME}:443/oauth2/token").audience("KU6Sp4fIownMf3RA0yiDk25flYga")
                .expirationTime(new Date(System.currentTimeMillis() + SIXTY_SECONDS))
                .notBeforeTime(new Date(System.currentTimeMillis() - SIXTY_SECONDS))
                .claim(AuthConstants.CLAIM_SET_AZP, "commonAzp").build();

        Assert.assertTrue(authentication.validateIdentity(claimsSet, clientId, userName));

    }

    /**
     * Test case to verify the behavior when the JWT client ID does not match the expected client ID.
     */
    @Test
    public void testJwtClientIdNotMatch() {
        String clientId = "deviceId";

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("admin")
                .issuer("https://${WSO2_PROXY_HOSTNAME}:443/oauth2/token").audience("KU6Sp4fIownMf3RA0yiDk25flYga")
                .expirationTime(new Date(System.currentTimeMillis() + SIXTY_SECONDS))
                .notBeforeTime(new Date(System.currentTimeMillis() - SIXTY_SECONDS))
                .claim(AuthConstants.CLAIM_SET_AZP, "notMatchedId").build();

        Assert.assertFalse(authentication.validateIdentity(claimsSet, clientId, ""));
    }

    /**
     * Test case to verify that the JWT identity does not match the expected identity.
     * 
     * <p>This test validates the behavior of the `validateIdentity` method when the JWT identity 
     * does not match the expected identity.
     * It creates a JWTClaimsSet with a subject that does not match the expected username and asserts that 
     * the `validateIdentity` method returns false.
     */
    @Test
    public void testJwtIdentityNotMatch() {
        String clientId = "deviceId";
        String userName = "user1";
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("wrongUserName")
                .issuer("https://${WSO2_PROXY_HOSTNAME}:443/oauth2/token").audience("KU6Sp4fIownMf3RA0yiDk25flYga")
                .expirationTime(new Date(System.currentTimeMillis() + SIXTY_SECONDS))
                .notBeforeTime(new Date(System.currentTimeMillis() - SIXTY_SECONDS))
                .claim(AuthConstants.CLAIM_SET_AZP, "wrongAzp").build();

        Assert.assertFalse(authentication.validateIdentity(claimsSet, clientId, userName));
    }

    /**
     *  In test cases following below. the names means:
     *  NBR: Not Before
     *  Exp: Expiration
     *  Time they refer can be considered as valid / invalid values with
     *  respect to current date
     *  And accordingly naming as:
     *  ValidExp / InValidExp
     *  ValidNBR / InvalidNBR
     */
    @Test
    public void testJwtTimeClaimsValidExpValidNbr() {

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("admin")
                .issuer("https://${WSO2_PROXY_HOSTNAME}:443/oauth2/token").audience("KU6Sp4fIownMf3RA0yiDk25flYga")
                .expirationTime(new Date(System.currentTimeMillis() + SIXTY_SECONDS))
                .notBeforeTime(new Date(System.currentTimeMillis() - SIXTY_SECONDS)).build();

        logger.debug("claim set is: {} ", claimsSet);
        assertTrue(authentication.validateJwtTimeClaims(claimsSet));
    }

    /**
     * Test case to verify the behavior of the `validateJwtTimeClaims` method when the expiration time is 
     * valid but the not before time is invalid.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testJwtTimeClaimsValidExpInvalidNbr() throws Exception {

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("admin")
                .issuer("https://${WSO2_PROXY_HOSTNAME}:443/oauth2/token").audience("KU6Sp4fIownMf3RA0yiDk25flYga")
                .expirationTime(new Date(System.currentTimeMillis() + SIXTY_SECONDS))
                .notBeforeTime(new Date(System.currentTimeMillis() + SIXTY_SECONDS)).build();

        logger.debug("claim set is: {} ", claimsSet);
        assertFalse(authentication.validateJwtTimeClaims(claimsSet));
    }

    /**
     * Test case to verify the behavior of the `validateJwtTimeClaims` method when the expiration time is null.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testJwtTimeClaimsWhenExpTimeNull() throws Exception {

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("admin")
                .issuer("https://${WSO2_PROXY_HOSTNAME}:443/oauth2/token").audience("KU6Sp4fIownMf3RA0yiDk25flYga")
                .expirationTime(null).notBeforeTime(new Date(System.currentTimeMillis() + SIXTY_SECONDS)).build();

        logger.debug("claim set is: {} ", claimsSet);
        assertFalse(authentication.validateJwtTimeClaims(claimsSet));
    }

    /**
     * Testing validateJWTTimeClaims method of JWTAuthentication class when the
     * Expire time of JWTClaims set has not been set and is null. Expect response
     * for validateJWTTimeClaims to be false
     */
    @Test
    public void testJwtTimeClaimsInvalidExpInvalidNbr() throws Exception {

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("admin")
                .issuer("https://${WSO2_PROXY_HOSTNAME}:443/oauth2/token").audience("KU6Sp4fIownMf3RA0yiDk25flYga")
                .expirationTime(new Date(System.currentTimeMillis() - SIXTY_SECONDS))
                .notBeforeTime(new Date(System.currentTimeMillis() + SIXTY_SECONDS)).build();

        logger.debug("claim set is: {} ", claimsSet);
        assertFalse(authentication.validateJwtTimeClaims(claimsSet));
    }

    /**
     * Test case to verify the behavior of the `validateJwtTimeClaims` method when the JWT expiration time is invalid
     * but the not-before time is valid.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testJwtTimeClaimsInvalidExpValidNbr() throws Exception {

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("admin")
                .issuer("https://${WSO2_PROXY_HOSTNAME}:443/oauth2/token").audience("KU6Sp4fIownMf3RA0yiDk25flYga")
                .expirationTime(new Date(System.currentTimeMillis() - SIXTY_SECONDS))
                .notBeforeTime(new Date(System.currentTimeMillis() - SIXTY_SECONDS)).build();

        logger.debug("claim set is: {} ", claimsSet);
        assertFalse(authentication.validateJwtTimeClaims(claimsSet));
    }

    /**
     * Test case to verify the validation of valid JWT scopes.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testValidJwtScopes() throws Exception {

        JSONArray scopes = new JSONArray();
        scopes.add("Portal");
        scopes.add("Dongle");

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("admin")
                .issuer("https://${WSO2_PROXY_HOSTNAME}:443/oauth2/token").audience("KU6Sp4fIownMf3RA0yiDk25flYga")
                .expirationTime(new Date(System.currentTimeMillis() + SIXTY_SECONDS))
                .notBeforeTime(new Date(System.currentTimeMillis() - SIXTY_SECONDS)).claim("scopes", scopes).build();

        logger.debug("claim set is: {} ", claimsSet);
        assertTrue(authentication.validateJwtScopes(claimsSet));
    }

    /**
     * Test case to verify the validation of a single valid JWT scope.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testSingleValidJwtScope() throws Exception {

        JSONArray scopes = new JSONArray();
        scopes.add("Dongle");

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("admin")
                .issuer("https://${WSO2_PROXY_HOSTNAME}:443/oauth2/token").audience("KU6Sp4fIownMf3RA0yiDk25flYga")
                .expirationTime(new Date(System.currentTimeMillis() + SIXTY_SECONDS))
                .notBeforeTime(new Date(System.currentTimeMillis() - SIXTY_SECONDS)).claim("scopes", scopes).build();

        logger.debug("claim set is: {} ", claimsSet);
        assertTrue(authentication.validateJwtScopes(claimsSet));
    }

    /**
     * Test case to verify the validation of JWT scopes for any one JWT.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testAnyOneJwtScopes() throws Exception {

        JSONArray scopes = new JSONArray();
        scopes.add("Dongle1");
        scopes.add("PortalMqtt");

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("admin")
                .issuer("https://${WSO2_PROXY_HOSTNAME}:443/oauth2/token").audience("KU6Sp4fIownMf3RA0yiDk25flYga")
                .expirationTime(new Date(System.currentTimeMillis() + SIXTY_SECONDS))
                .notBeforeTime(new Date(System.currentTimeMillis() - SIXTY_SECONDS)).claim("scopes", scopes).build();

        logger.debug("claim set is: {} ", claimsSet);
        assertTrue(authentication.validateJwtScopes(claimsSet));
    }

    /**
     * Test case to verify the behavior of invalid JWT scopes.
     *
     * <p>This method constructs a JWTClaimsSet object with invalid scopes and validates
     * that the authentication does not pass. It asserts that the authentication.validateJwtScopes()
     * method returns false.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testInValidJwtScopes() throws Exception {

        JSONArray scopes = new JSONArray();
        scopes.add("Dongle1");
        scopes.add("scope1");

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("admin")
                .issuer("https://${WSO2_PROXY_HOSTNAME}:443/oauth2/token").audience("KU6Sp4fIownMf3RA0yiDk25flYga")
                .expirationTime(new Date(System.currentTimeMillis() + SIXTY_SECONDS))
                .notBeforeTime(new Date(System.currentTimeMillis() - SIXTY_SECONDS)).claim("scopes", scopes).build();

        logger.debug("claim set is: {} ", claimsSet);
        assertFalse(authentication.validateJwtScopes(claimsSet));
    }

    /**
     * Test case to verify the validation of JWT time claims when the expiration time is valid and no 
     * "not before" time is specified.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testJwtTimeClaimsValidExpNoNbr() throws Exception {

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("admin")
                .issuer("https://${WSO2_PROXY_HOSTNAME}:443/oauth2/token").audience("KU6Sp4fIownMf3RA0yiDk25flYga")
                .expirationTime(new Date(System.currentTimeMillis() + SIXTY_SECONDS)).build();

        logger.debug("claim set is: {} ", claimsSet);
        assertTrue(authentication.validateJwtTimeClaims(claimsSet));
    }

    /**
     * Test when userName is empty.
     */
    @Test
    public void testAuthenticateWhenUserNameEmpty() {
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);
        Mockito.when(simpleAuthInput.getClientInformation().getClientId()).thenReturn("testclient");
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Optional<String> userName = Optional.empty();
        Optional<ByteBuffer> password = Optional.ofNullable(ByteBuffer.wrap("12345".getBytes(StandardCharsets.UTF_8)));
        Mockito.when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(userName);
        Mockito.when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(password);
        assertFalse(authentication.authenticate(simpleAuthInput).get().isAuthenticated());
    }

    /**
     * Test when password is empty.
     */
    @Test
    public void testAuthenticateWhenPasswordEmpty() {
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);
        Mockito.when(simpleAuthInput.getClientInformation().getClientId()).thenReturn("testclient");
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Optional<String> userName = Optional.ofNullable("testUser");
        Optional<ByteBuffer> password = Optional.empty();
        Mockito.when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(userName);
        Mockito.when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(password);
        assertFalse(authentication.authenticate(simpleAuthInput).get().isAuthenticated());
    }

    /**
     * Test when both userName and Password are empty.
     */
    @Test
    public void testAuthenticateWhenNoUserNameAndPassword() {
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);
        Mockito.when(simpleAuthInput.getClientInformation().getClientId()).thenReturn("testclient");
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Optional<String> userName = Optional.empty();
        Optional<ByteBuffer> password = Optional.empty();
        Mockito.when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(userName);
        Mockito.when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(password);
        assertFalse(authentication.authenticate(simpleAuthInput).get().isAuthenticated());
    }

    /**
     * Test when both userName and password are present and correct, however the jwt
     * is invalid.
     */
    @Test
    public void testAuthenticateWhenVaildUserCredsButInvalidJwt() {
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);

        Mockito.when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(Optional.ofNullable("testUser"));
        Mockito.when(simpleAuthInput.getConnectPacket().getPassword())
                .thenReturn(Optional.ofNullable(ByteBuffer.wrap("12345".getBytes(StandardCharsets.UTF_8))));
        Mockito.when(simpleAuthInput.getClientInformation().getClientId()).thenReturn("testClientId");
        assertFalse(authentication.authenticate(simpleAuthInput).get().isAuthenticated());
    }

    /**
     * Testing when the jwt is a proper signedJwt token . however it is not a
     * verified token
     */
    @Test
    public void testAuthenticateWhenUnverifiedJwt() {
        Mockito.when(simpleAuthInput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(simpleAuthInput.getClientInformation()).thenReturn(clientInformation);
        String token = "eyJpZG4iOiJDPVVTLFNUPUlsbGlub2lzLE89SGFybWFuLE9VPUNvbm5lY3RlZCBDYXIsQ049SGFyb"
                + "WFuIEhpdmVNUSBJbnRlcm1lZGlhdGUgQ0EiLCJraWQiOiI1NiIsInR5cCI6IkpXVCIsImFsZyI6IlJTMjU2In0"
                + ".eyJlc24iOiJ0Ym1jbGllbnQtZXNuLTEiLCJ2aW4iOiJsb2NhbGhvc3QiLCJleHAiOjE1NDgxNjU1ODYsImlhdCI6"
                + "MTU0ODE2NTM0Nn0.UZl1uTcaKqzizdp5ffoZCjuznU_ELVaNSQY1UGuVchFUdDRoZ5k-4bD5BD2pNYpWAcwftbKgv6Nv"
                + "YCXi5ovb9EEjL-seMDXmDaqB5eB_VBmkK1zs99fl_visSKUImwxLwgJKHX5CMlECfr6X8lyC75emYDcMe6eyMXMXgpL"
                + "_rHdRWnkiVKqVSSFH_lm9kWSo8mtJqwv3RSGiYnS6WpODHJEz4SpeYiEuAGIIeDd1cBN5K8qnADv-W90Xf3WovcUmQK6h"
                + "6zbblVb1AcmJrvify3Zco5ZuMQJH-3s2ZLBLxVR5anBHwvZRMdHIL4xa4Ujtox26vEwzplmUOXoJ2Pc2Tg";
        Mockito.when(simpleAuthInput.getConnectPacket().getUserName()).thenReturn(Optional.ofNullable("testUser"));
        Mockito.when(simpleAuthInput.getConnectPacket().getPassword()).thenReturn(Optional.ofNullable(
                ByteBuffer.wrap(token.getBytes(StandardCharsets.UTF_8))));
        Mockito.when(simpleAuthInput.getClientInformation().getClientId()).thenReturn("testClientId");
        assertFalse(authentication.authenticate(simpleAuthInput).get().isAuthenticated());
    }

}