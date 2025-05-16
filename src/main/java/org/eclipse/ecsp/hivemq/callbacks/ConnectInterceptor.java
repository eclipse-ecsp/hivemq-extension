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

package org.eclipse.ecsp.hivemq.callbacks;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.connect.ConnectInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundInput;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableConnectPacket;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class intercept all connect request and override cleanSession property as configured.
 */
@Component
public class ConnectInterceptor implements ConnectInboundInterceptor {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(ConnectInterceptor.class);
    // To change session persistence level
    private boolean cleanSession;

    /**
     * The ConnectInterceptor class is responsible for intercepting the connect requests made to the HiveMQ broker.
     * It checks if the clean session is enabled and logs the status.
     */
    public ConnectInterceptor() {
        cleanSession = Boolean.valueOf(PropertyLoader.getValue(AuthConstants.CLEAN_SESSION));
        LOGGER.debug("Clean session enabled status: {} ", cleanSession);
    }

    /**
        * This method is called when a client connects to the HiveMQ broker.
        * It is responsible for handling the connect event and modifying the connect packet if necessary.
        *
        * @param connectInboundInput  The input parameters for the connect event.
        * @param connectInboundOutput The output parameters for the connect event.
        */
    @Override
    public void onConnect(@NotNull ConnectInboundInput connectInboundInput,
            @NotNull ConnectInboundOutput connectInboundOutput) {
        final ModifiableConnectPacket connectPacket = connectInboundOutput.getConnectPacket();

        if (cleanSession) {
            connectPacket.setSessionExpiryInterval(0);
            connectPacket.setCleanStart(cleanSession);
            LOGGER.debug("New persistence status: {} for clientId: {}", cleanSession,
                    connectInboundInput.getClientInformation().getClientId());
        }

    }
}
