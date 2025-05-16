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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.cache.IgniteAuthInfo;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.json.simple.JSONArray;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;

/**
 * This class provides methods for jwt authentication for mqtt client connect.
 */
@Component
@ConditionalOnProperty(name = "ssdp.simulator", havingValue = "true")
public class SimulatorJwtAuthentication extends SimulatorUsernamePasswordAuthentication {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(SimulatorJwtAuthentication.class);
    private JWSVerifier verifier;
    private String jwtPublicKeyPath;
    private boolean jwtEnabled;

    /**
     * This constructor checks if jwt authentication is enabled then load jwt public key from property map.
     *
     * @throws IOException              File not available on given path
     * @throws InvalidKeySpecException  If wrong algorithm is passed in
     *                                  KeyFactory.getInstance
     * @throws NoSuchAlgorithmException If invalid key is provided to generatePublic
     *                                  method
     * @throws Exception                - if not able to read public key from
     *                                  properties.
     */
    public SimulatorJwtAuthentication() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        jwtEnabled = "true".equalsIgnoreCase(PropertyLoader.getValue(AuthConstants.JWT_VALIDATION_ENABLED));
        if (jwtEnabled) {
            jwtPublicKeyPath = PropertyLoader.getValue(AuthConstants.JWT_PUBLIC_KEY_PATH);
            if (!StringUtils.isEmpty(jwtPublicKeyPath)) {
                LOGGER.info("JWT token public key path found {}", jwtPublicKeyPath);
                PublicKey publicKey = HivemqUtils.readPublickey(jwtPublicKeyPath);
                verifier = new RSASSAVerifier((RSAPublicKey) publicKey);
            } else {
                LOGGER.info("JWT token public key path not found :" + jwtPublicKeyPath);
            }
        } else {
            LOGGER.trace("JWT validation is disabled");
        }
    }

    /**
     * Authenticates the user based on the provided authentication input.
     * If JWT authentication is enabled, it retrieves the username and password from the input,
     * decodes the password buffer, and checks if the user is authenticated.
     * If authentication is successful, it returns the authenticated user information.
     * If JWT authentication is disabled, it logs a trace message and falls back to the superclass's 
     * authentication method.
     * If an exception occurs during authentication, it logs an error message and returns an empty optional.
     *
     * @param simpleAuthInput The authentication input containing the connect packet and client information.
     * @return An optional containing the authenticated user information if authentication is successful, 
     *      otherwise an empty optional.
     */
    @Override
    public Optional<IgniteAuthInfo> authenticate(SimpleAuthInput simpleAuthInput) {
        try {
            if (jwtEnabled) {
                String userName = simpleAuthInput.getConnectPacket().getUserName().orElse(StringUtils.EMPTY);
                String password = StringUtils.EMPTY;
                @NotNull
                Optional<ByteBuffer> passwordBuffer = simpleAuthInput.getConnectPacket().getPassword();
                if (passwordBuffer.isPresent()) {
                    password = StandardCharsets.ISO_8859_1.decode(passwordBuffer.get()).toString();
                }

                LOGGER.debug("JWTauthenticate: userName: {} clientId:  {}", userName,
                        simpleAuthInput.getClientInformation().getClientId());
                Optional<IgniteAuthInfo> info = isAuthenticated(password,
                        simpleAuthInput.getClientInformation().getClientId(), userName);
                if (info.isPresent() && info.get().isAuthenticated()) {
                    return info;
                }
            } else {
                LOGGER.trace("JWT validation is disabled");
            }
            LOGGER.info("Failed to validate by JWT and trying via UserName and Password");
            return super.authenticate(simpleAuthInput);
        } catch (Exception e) {
            LOGGER.error("Unable to authenticate.", e);
        }
        return Optional.empty();
    }

    /**
     * Checks if the provided token is authenticated for the given clientId and userName.
     *
     * @param token     The JWT token to be authenticated.
     * @param clientId  The client ID associated with the token.
     * @param userName  The username associated with the token.
     * @return          An Optional containing the authenticated information if the token is valid, 
     *                  otherwise an empty Optional.
     * @throws NoSuchAlgorithmException     If the algorithm used for token verification is not available.
     * @throws InvalidKeySpecException       If the key specification used for token verification is invalid.
     * @throws IOException                  If an I/O error occurs while parsing the token.
     */
    private Optional<IgniteAuthInfo> isAuthenticated(String token, String clientId, String userName) 
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        try {
            SignedJWT signedJwt = SignedJWT.parse(token);
            LOGGER.debug("isverifier present: {}", verifier);
            if (signedJwt.verify(verifier)) {

                if (validateIdentity(signedJwt.getJWTClaimsSet(), clientId, userName)
                        && validateJwtTimeClaims(signedJwt.getJWTClaimsSet())
                        && validateJwtScopes(signedJwt.getJWTClaimsSet())) {
                    Long expInEpoc = signedJwt.getJWTClaimsSet().getExpirationTime().toInstant().getEpochSecond();
                    IgniteAuthInfo info = new IgniteAuthInfo().setAuthenticated(true).setExp(expInEpoc);
                    return Optional.of(info);
                } else {
                    LOGGER.info("Invalid clientId: {}, JWT time rage or invalid JWT scope. JWT Claimset is {}",
                            clientId, signedJwt.getJWTClaimsSet());
                }
                LOGGER.debug("JwtPayload:" + signedJwt.getJWTClaimsSet().toJSONObject());
            } else {
                LOGGER.info("Validation is failed for token:" + token);
            }
            return Optional.empty();
        } catch (ParseException e) {
            LOGGER.warn("Unexpected number of Base64URL parts, must be three: {}", e);
        } catch (JOSEException e) {
            LOGGER.error("Javascript Object Signing and Encryption (JOSE) exception.{}", e);
        }
        return Optional.empty();
    }

    /**
     * This method  validates identity of user from jwt token by comparing azp or sub field 
     * with clientId or username.
     *
     * @param claimsSet - token claimset
     * @param clientId - client id
     * @param userName - username
     * @return true if identity is valid
     */
    public boolean validateIdentity(JWTClaimsSet claimsSet, String clientId, String userName) {
        String azp = (String) claimsSet.getClaims().get(AuthConstants.CLAIM_SET_AZP);
        String sub = (String) claimsSet.getClaims().get(AuthConstants.CLAIM_SET_SUB);
        List<String> aud = claimsSet.getAudience();
        if (azp != null && azp.equals(clientId)) {
            LOGGER.debug("azp {} (Authorized party - the party to which the ID Token was issued) "
                    + "is matched with clientId: {}", azp, clientId);
            return true;
        } else if (aud != null && aud.contains(clientId)) {
            LOGGER.debug("aud {} (Authorized party - the party to which the ID Token was issued) "
                    + "is matched with clientId: {}", aud, clientId);
            return true;
        } else if (sub.equals(userName)) {
            LOGGER.debug(
                    "sub {} (The subject value MUST either be scoped to be locally unique in the "
                            + "context of the issuer or be globally unique.) is matched with username {}",
                    sub, userName);
            return true;
        }
        LOGGER.debug("azp {} (Authorized party - the party to which the ID Token was issued) "
                + "is not matched with clientId: {}", azp, clientId);
        return false;
    }

    /**
     * This method validates token for expiration.
     *
     * @param claimSet - token claimset
     * @return - true if token is not expired
     */
    public boolean validateJwtTimeClaims(JWTClaimsSet claimSet) {
        Date now = new Date();
        Date expTime = claimSet.getExpirationTime();
        Date nbfTime = claimSet.getNotBeforeTime();
        if (expTime == null) {
            LOGGER.debug("exp claims of JWT is null");
            return false;
        }
        if (nbfTime != null && now.before(expTime) && (now.after(nbfTime) || now.equals(nbfTime))) {
            LOGGER.debug("exp and nbf claims of JWT are valid: {} , {} ", expTime, nbfTime);
            return true;
        } else if (nbfTime == null && now.before(expTime)) {
            LOGGER.debug("exp claim of JWT are valid [nbf claim is not in token]: {} ", expTime);
            return true;
        } else {
            LOGGER.debug("exp and nbf claims of JWT aren't valid: {} , {} ", expTime, nbfTime);
            return false;
        }
    }

    /**
     * This method validates jwt scops available in token with list of pre configured scopes in hivemq properties.
     *
     * @param claimSet - jwt claimset
     * @return true if token scope is valid
     */
    public boolean validateJwtScopes(JWTClaimsSet claimSet) 
            throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        final String scopes = "scopes";
        boolean scopesValidity = false;
        JSONArray scopesFromConfig = getScopesFromConfig();
        LOGGER.info("Scopes present in config file are : {}", scopesFromConfig.toJSONString());
        ArrayList<Object> scopesFromJwtToken = (ArrayList) claimSet.getClaim(scopes);
        LOGGER.info("Scopes present in incoming JWT are: {}", scopesFromJwtToken);

        for (Object object : scopesFromJwtToken) {
            if (scopesFromConfig.contains(object)) {
                scopesValidity = true;
                break;
            }
        }

        if (!scopesValidity) {
            LOGGER.info("Invalid scope from the client, JWT client scopes received {}. Valid scopes are {}",
                    scopesFromJwtToken, scopesFromConfig.toJSONString());
        }
        return scopesValidity;
    }

    /**
     * Retrieves the scopes from the configuration file.
     *
     * @return The scopes from the configuration file as a JSONArray.
     * @throws NoSuchAlgorithmException If the algorithm used for key generation is not available.
     * @throws InvalidKeySpecException If the key specification is invalid.
     * @throws IOException If an I/O error occurs while reading the configuration file.
     */
    private JSONArray getScopesFromConfig() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        loadJwtKeyPath();
        String validScopesString = PropertyLoader.getValue(AuthConstants.VALID_SCOPES);
        JSONArray scopesFromConfig = new JSONArray();
        StringTokenizer st = new StringTokenizer(validScopesString, AuthConstants.DELIMITER);
        while (st.hasMoreTokens()) {
            scopesFromConfig.add(st.nextToken());
        }
        return scopesFromConfig;
    }

    /**
     * Loads the JWT key path and initializes the JWT verifier.
     *
     * @throws NoSuchAlgorithmException if the algorithm used for key generation is not available
     * @throws InvalidKeySpecException if the provided key specification is invalid
     * @throws IOException if an I/O error occurs while reading the key file
     */
    private void loadJwtKeyPath() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        String newJwtPubKeyPath = PropertyLoader.getValue(AuthConstants.JWT_PUBLIC_KEY_PATH);
        if (!StringUtils.isEmpty(newJwtPubKeyPath) && !newJwtPubKeyPath.equals(jwtPublicKeyPath)) {
            jwtPublicKeyPath = newJwtPubKeyPath;
            LOGGER.info("JWT token public key path found {}", jwtPublicKeyPath);
            PublicKey publicKey;
            try {
                publicKey = HivemqUtils.readPublickey(jwtPublicKeyPath);
                verifier = new RSASSAVerifier((RSAPublicKey) publicKey);
            } catch (NoSuchAlgorithmException | InvalidKeySpecException | IOException e) {
                LOGGER.error("Error while reading JWT public key: {}", e);
                throw e;
            }
        } else {
            LOGGER.debug("JWT token key path is either old (so no need to load JWT public key again) OR not found : {}",
                    jwtPublicKeyPath);
        }
    }
}
