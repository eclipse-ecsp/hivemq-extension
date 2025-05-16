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
import com.hivemq.extension.sdk.api.interceptor.pingreq.PingReqInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.pingreq.parameter.PingReqInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pingreq.parameter.PingReqInboundOutput;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.cache.TokenExpiryHandler;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Ping interceptor class which intecept all ping request coming from clients.
 *
 * @author Neha Khan
 */
@Component
public abstract class AbstractPingReqInboundInterceptor implements PingReqInboundInterceptor {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(AbstractPingReqInboundInterceptor.class);

    /**
     * Handles the incoming PINGREQ message from the MQTT client.
     *
     * @param pingReqInboundInput  The input object containing information about the incoming PINGREQ message.
     * @param pingReqInboundOutput The output object used to control the behavior of the PINGREQ handling.
     */
    @Override
    public void onInboundPingReq(@NotNull PingReqInboundInput pingReqInboundInput,
            @NotNull PingReqInboundOutput pingReqInboundOutput) {
        String username = pingReqInboundInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME).orElse(StringUtils.EMPTY);
        String clientId = pingReqInboundInput.getClientInformation().getClientId();
        LOGGER.debug("onPingReceived request for clientId: {} and userName {}", clientId, username);
        TokenExpiryHandler.validateTokenExpiration(clientId, username);
        doPingReceived(pingReqInboundInput);
    }

    /**
     * This method is called when a PINGREQ message is received.
     * Implementations of this method should handle the logic for processing the PINGREQ message.
     *
     * @param pingReqInboundInput The input object containing information about the received PINGREQ message.
     */
    protected abstract void doPingReceived(PingReqInboundInput pingReqInboundInput);

}
