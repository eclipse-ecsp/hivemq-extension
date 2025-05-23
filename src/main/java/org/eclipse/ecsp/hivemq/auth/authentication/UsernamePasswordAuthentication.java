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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthInput;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.base.AbstractAuthentication;
import org.eclipse.ecsp.hivemq.cache.IgniteAuthInfo;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * This class authenticate user with username and password. This way of
 * authentication is only used internal services.
 */
@Component
public class UsernamePasswordAuthentication extends AbstractAuthentication {

    private static final String UNKNOWN_USER = "Unknown user";

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(UsernamePasswordAuthentication.class);

    private boolean staticPassEnabled = HivemqUtils.getPropertyBooleanValue(AuthConstants.STATIC_CRED_ENABLED);

    @Value("${mqtt.user.password:hivemq}")
    private String staticPass;
    private static final String PERF_USER = "perf_user";

    /**
     * Authenticates the user based on the provided authentication input.
     *
     * @param simpleAuthInput The authentication input containing client information and credentials.
     * @return An optional {@link IgniteAuthInfo} object representing the authentication result.
     *         If the user is authenticated successfully, the object will be present and marked as authenticated.
     *         If the user fails to authenticate, the object will be present but marked as not authenticated.
     *         If the authentication process encounters an error, the object will be absent.
     */
    @Override
    public Optional<IgniteAuthInfo> authenticate(SimpleAuthInput simpleAuthInput) {
        String clientId = simpleAuthInput.getClientInformation().getClientId();
        String userName = simpleAuthInput.getConnectPacket().getUserName().orElse(StringUtils.EMPTY);
        String password = StringUtils.EMPTY;
        @NotNull
        Optional<ByteBuffer> passwordBuffer = simpleAuthInput.getConnectPacket().getPassword();
        if (passwordBuffer.isPresent()) {
            password = StandardCharsets.ISO_8859_1.decode(passwordBuffer.get()).toString();
        }
        if (staticPassEnabled && (HivemqUtils.isWhiteListedUser(userName) || PERF_USER.equals(userName))
                && staticPass.equals(password)) {
            LOGGER.info("Whitelisted user: {} is authenticated successfully...!!!", userName);
            return Optional.of(new IgniteAuthInfo().setAuthenticated(true).setExp(Long.MAX_VALUE));
        } else {
            LOGGER.warn("Authentication request with wrong password, clientId: {} username {}",
                    clientId, userName != null ? userName : UNKNOWN_USER);
            return Optional.of(new IgniteAuthInfo().setAuthenticated(false).setExp(0L));
        }
    }

    /**
     * Sets the flag indicating whether static password authentication is enabled.
     *
     * @param staticPassEnabled true if static password authentication is enabled, false otherwise
     */
    public void setStaticPassEnabled(boolean staticPassEnabled) {
        this.staticPassEnabled = staticPassEnabled;
    }

    /**
     * Sets the static password for authentication.
     *
     * @param staticPass the static password to set
     */
    public void setStaticPass(String staticPass) {
        this.staticPass = staticPass;
    }

}
