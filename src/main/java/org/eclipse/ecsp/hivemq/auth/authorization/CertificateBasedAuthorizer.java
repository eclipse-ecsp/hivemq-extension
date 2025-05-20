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
import com.hivemq.extension.sdk.api.client.parameter.ClientTlsInformation;
import com.hivemq.extension.sdk.api.client.parameter.InitializerInput;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Optional;

/**
 * This class identify class via TLS certificate if its a whitelisted user or a
 * device and then provides default permissions..
 */
@Component
public class CertificateBasedAuthorizer extends Authorizer {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(CertificateBasedAuthorizer.class);
    protected String serviceCertificatePrefix;

    
    /**
     * Constructs a new {@code CertificateBasedAuthorizer} instance.
     *
     * <p>Initializes the {@code serviceCertificatePrefix} field by retrieving the list of property values
     * associated with the {@code SERVICE_CERTIFICATE_COMMON_NAME_PREFIX} key from the authorization properties.
     * If the list is not null and not empty, the first value is converted to uppercase and assigned to
     * {@code serviceCertificatePrefix}. Otherwise, it is set to an empty string.
     */
    public CertificateBasedAuthorizer() {
        List<String> propertyValueList = authorizeProperties.get(AuthConstants.SERVICE_CERTIFICATE_COMMON_NAME_PREFIX);
        this.serviceCertificatePrefix = (propertyValueList != null && !propertyValueList.isEmpty())
                ? propertyValueList.get(0).toUpperCase()
                : StringUtils.EMPTY;
    }

    /**
     * Authorizes the client based on the provided initializer input.
     * If the client has a TLS certificate, it authorizes based on the certificate information.
     * If the client does not have a TLS certificate, it forwards the authorization to the next authorizer.
     *
     * @param initializerInput The initializer input containing the client and connection information.
     * @return A list of topic permissions for the client.
     */
    @Override
    public List<TopicPermission> authorize(InitializerInput initializerInput) {
        Optional<ClientTlsInformation> clientTlsInfo = initializerInput.getConnectionInformation()
                .getClientTlsInformation();
        if (clientTlsInfo.isPresent()) {
            String clientId = initializerInput.getClientInformation().getClientId();
            final Optional<String> usernameOptional = initializerInput.getConnectionInformation()
                    .getConnectionAttributeStore().getAsString(AuthConstants.USERNAME);
            String userName = usernameOptional.isPresent() ? usernameOptional.get() : StringUtils.EMPTY;

            X509Certificate certificate = clientTlsInfo.get().getClientCertificate().orElse(null);
            String commonName = HivemqUtils.getCnFromCert(certificate);
            LOGGER.debug("Certificate is present hence authorizing based on the provided certificate cn {}. "
                    + "Logging scope: E2E, DVI", commonName);
            List<TopicPermission> topicPermission = null;
            if (HivemqUtils.isServiceRequest(userName, commonName, serviceCertificatePrefix)) {
                topicPermission = getWhiteUserPermission(userName, clientId);
            } else {
                topicPermission = getDevicePermission(userName, clientId, false);
            }
            return topicPermission;
        } else {
            LOGGER.debug("Certificate is not present.So, forward to the next authorizer. Logging scope: E2E, DVI");
            return super.authorize(initializerInput);
        }
    }
}
