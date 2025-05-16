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
import com.hivemq.extension.sdk.api.client.parameter.ClientTlsInformation;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.cache.IgniteAuthInfo;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * This class authenticates via TLS certificate.
 */
@Component
@ConditionalOnProperty(name = "ssdp.simulator", havingValue = "true")
public class SimulatorCertificateAuthentication extends SimulatorJwtAuthentication {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(SimulatorCertificateAuthentication.class);
    private final Properties properties;
    private final String serviceCertificatePrefix;
    private final boolean allowOnlyWhiteListUserWithoutCert;
    private Set<String> serviceCertificateCommonNames;

    /**
     * This constructor loads all required properties on application startup.
     *
     * @throws Exception - when class not able to load property file.
     */
    public SimulatorCertificateAuthentication() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        properties = PropertyLoader.getProperties();
        this.serviceCertificatePrefix = properties.getProperty(AuthConstants.SERVICE_CERTIFICATE_COMMON_NAME_PREFIX,
                StringUtils.EMPTY);
        this.allowOnlyWhiteListUserWithoutCert = Boolean
                .getBoolean(properties.getProperty(AuthConstants.ALLOW_ONLY_WHITELIST_USER_WITHOUT_CERT, "true"));
        extractServiceCertificateCommonNames(
                PropertyLoader.getPropertiesMap().get(AuthConstants.SERVICE_CERTIFICATE_COMMON_NAMES));
        LOGGER.debug("The service cns that are considered {} ", serviceCertificateCommonNames);
    }

    /**
     * Authenticates the client based on the provided authentication input.
     *
     * @param simpleAuthInput The authentication input containing client information and credentials.
     * @return An optional {@link IgniteAuthInfo} object representing the authentication result.
     *         If authentication is successful, the object will be present and authenticated will be set to true.
     *         If authentication fails, the object will be present and authenticated will be set to false.
     *         If authentication cannot be performed, the object will be empty.
     */
    @Override
    public Optional<IgniteAuthInfo> authenticate(SimpleAuthInput simpleAuthInput) {
        if (HivemqUtils.isInternalService(properties, simpleAuthInput.getConnectionInformation())) {
            LOGGER.info("Authenticate for internal service with clientId: {}",
                    simpleAuthInput.getClientInformation().getClientId());
            // It happens in two ways:-
            // DeviceMessaging is connecting to Broker on port 1883 or ELB is mapped with
            // 1883 (in place of 8883 which is TLS enabled),
            // so Client can connect without certificate.
            if (allowOnlyWhiteListUserWithoutCert) {
                // TLS enabled, allow only DeviceMessaging to connect without certificate
                // (authenticate via username and password),
                // property used when TLS is enabled
                String userName = simpleAuthInput.getConnectPacket().getUserName().orElse(StringUtils.EMPTY);
                LOGGER.debug("Only whitelist is allowed to connect without TLS certificate. "
                        + "WhiteList user is connecting. User Name: {}", userName);
                return super.authenticate(simpleAuthInput);
            } else {
                // ELB is mapped with 1883 (in place of 8883 which is TLS enabled), so Client
                // can connect without certificate.
                LOGGER.debug("TLS is disabled. It means ELB is mapped to 1883. "
                        + "So allow devices to connect without TLS certificate.");
                return super.authenticate(simpleAuthInput);
            }
        } else if (simpleAuthInput.getConnectionInformation().getClientTlsInformation().isPresent()) {
            LOGGER.info("Authenticate by validating certificate for clientId: {}",
                    simpleAuthInput.getClientInformation().getClientId());
            /*
             * If device is connecting, - get certificate - get cn name - make sure the cn
             * name is equal to the clientID If service is connecting, - get the cn name,
             * and check if it is in the list
             */
            ClientTlsInformation clientCertificate = simpleAuthInput.getConnectionInformation()
                    .getClientTlsInformation().orElse(null);

            X509Certificate certificate = clientCertificate != null
                    ? clientCertificate.getClientCertificate().orElse(null)
                    : null;
            String commonName = HivemqUtils.getCnFromCert(certificate);
            String cnName = "null".equalsIgnoreCase(commonName) ? null : commonName;

            LOGGER.debug("received certificate: cn {}. Logging scope: E2E, DVI", cnName);
            return doValidate(simpleAuthInput, certificate, cnName, true);
        } else if (simpleAuthInput.getConnectPacket().getPassword().isPresent()) {
            LOGGER.info("Authenticate by validating 2 way TLS for clientId: {}",
                    simpleAuthInput.getClientInformation().getClientId());
            String pwd = StandardCharsets.ISO_8859_1
                    .decode(simpleAuthInput.getConnectPacket().getPassword().orElse(null)).toString();
            LOGGER.debug("Two way TLS authentication is started for {}",
                    simpleAuthInput.getClientInformation().getClientId());

            X509Certificate certificate = null;
            try {
                certificate = HivemqUtils.getCertificateFromString(pwd);
                LOGGER.debug("Certificate parsed is done. Logging scope: E2E, DVI");
            } catch (CertificateException e) {
                LOGGER.error("Certificate parsed is not done . Logging scope: E2E, DVI", e);
                return Optional.of(new IgniteAuthInfo().setAuthenticated(false).setExp(0L));
            }
            String commonName = HivemqUtils.getCnFromCert(certificate);
            String cnName = "null".equalsIgnoreCase(commonName) ? null : commonName;
            LOGGER.debug("Certificate common name is: {}. Logging scope: E2E, DVI", cnName);
            return doValidate(simpleAuthInput, certificate, cnName, false);
        }
        LOGGER.error("Client:{} is not authenticated any of the procedure. Logging scope: E2E, DVI",
                simpleAuthInput.getClientInformation().getClientId());
        return Optional.empty();
    }

    /**
     * Validates the given certificate and performs additional validation based on the certificate type.
     *
     * @param simpleAuthInput The input for the authentication process.
     * @param certificate The X509 certificate to be validated.
     * @param cnName The common name extracted from the certificate.
     * @param isNewTwoWayTls A flag indicating whether the certificate is for a new two-way TLS connection.
     * @return An Optional containing the authentication information if the certificate is valid, 
     *      otherwise an empty Optional.
     */
    private Optional<IgniteAuthInfo> doValidate(SimpleAuthInput simpleAuthInput, X509Certificate certificate,
            String cnName, boolean isNewTwoWayTls) {
        if (!isValidCertificate(certificate, cnName, isNewTwoWayTls)) {
            LOGGER.error("certificate is not valid. Logging scope: E2E, DVI");
            return Optional.of(new IgniteAuthInfo().setAuthenticated(false).setExp(0L));
        }
        String userName = simpleAuthInput.getConnectPacket().getUserName().orElse(StringUtils.EMPTY);

        LOGGER.debug("certificate is valid, further validating as device or service/server by identiyfing it");
        boolean isValid = HivemqUtils.isServiceRequest(userName, cnName, serviceCertificatePrefix)
                ? isServiceCertificateValid(cnName)
                : isValidDeviceCertificate(cnName, simpleAuthInput.getClientInformation().getClientId());
        Long expInEpoc = certificate.getNotAfter().toInstant().getEpochSecond();
        IgniteAuthInfo info = new IgniteAuthInfo().setAuthenticated(isValid).setExp(expInEpoc);
        return Optional.of(info);
    }

    /**
     * Checks if the given X.509 certificate is valid.
     *
     * @param certificate The X.509 certificate to be validated.
     * @param cnName The common name (CN) of the certificate.
     * @param isNewTwoWayTls Flag indicating if the certificate is for a new two-way TLS connection.
     * @return {@code true} if the certificate is valid, {@code false} otherwise.
     */
    private boolean isValidCertificate(X509Certificate certificate, String cnName, boolean isNewTwoWayTls) {
        //check expiry date, check issuer - Yet to implement etc
        if (StringUtils.isEmpty(cnName)) {
            LOGGER.error("Common name is missing. No valid certifcate submitted. Logging scope: E2E, DVI");
            return false;
        }
        // certificate expiry check
        if (isNewTwoWayTls) {
            if (certificate.getNotAfter().after(new Date())) {
                LOGGER.debug("Valid ceertificate found. Logging scope: E2E, DVI");
                return true;
            }
            LOGGER.error("Certificate period is invalid. No valid certifcate submitted. Logging scope: E2E, DVI");
        }

        return !isNewTwoWayTls;
    }

    /**
     * Checks if the service certificate is valid based on the provided common name.
     *
     * @param commonName The common name of the certificate to be checked.
     * @return {@code true} if the service certificate is valid, {@code false} otherwise.
     */
    private boolean isServiceCertificateValid(String commonName) {
        boolean isServiceCertificateValid = false;
        String serviceCommonNameSuffix = commonName.substring(serviceCertificatePrefix.length());
        isServiceCertificateValid = serviceCertificateCommonNames.contains(serviceCommonNameSuffix.toUpperCase());
        LOGGER.debug(
                "isAServiceCertificateValid - certificate cn {} is compared against {}. "
                        + "Is Service Certificate Valid? {}",
                commonName, serviceCommonNameSuffix, isServiceCertificateValid);
        return isServiceCertificateValid;
    }

    /**
     * Checks if the device certificate is valid for the given common name and client ID.
     *
     * @param commonName The common name from the certificate.
     * @param clientId   The client ID to compare against the common name.
     * @return {@code true} if the device certificate is valid, {@code false} otherwise.
     */
    private boolean isValidDeviceCertificate(String commonName, String clientId) {
        boolean isDeviceCerticateValid = false;
        isDeviceCerticateValid = commonName.equalsIgnoreCase(clientId);
        LOGGER.debug(
                "isAValidDeviceCertificate - certificate cn {} is compared against {}. "
                        + "Is Device Certificate Valid {}. Logging scope: E2E, DVI",
                commonName, clientId, isDeviceCerticateValid);
        return isDeviceCerticateValid;
    }

    /**
     * Extracts the common names from the given list of service certificate common names.
     * The extracted common names are stored in a HashSet and converted to uppercase.
     *
     * @param serviceCertificateCommonNamesList the list of service certificate common names
     */
    private void extractServiceCertificateCommonNames(List<String> serviceCertificateCommonNamesList) {
        this.serviceCertificateCommonNames = new HashSet<>();
        for (String cn : serviceCertificateCommonNamesList) {
            this.serviceCertificateCommonNames.add(cn.trim().toUpperCase());
        }
        LOGGER.debug("Service Certificate Common Names {}", this.serviceCertificateCommonNames);
    }
}