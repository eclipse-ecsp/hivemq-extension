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
import com.hivemq.extension.sdk.api.interceptor.puback.PubackOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackOutboundOutput;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class intercepts the outgoing publish acknowledgement packets
 * to clients.
 * Puback is sent only for QoS 1 and Qos 2 messages.
 *
 * @author saadhikari
 */
@Component
public class PubackSend implements PubackOutboundInterceptor {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PubackSend.class);

    /**
     * This method is called when a PUBACK message is sent outbound.
     * It logs the clientId and messageId of the PUBACK message.
     *
     * @param pubackOutboundInput The input object containing information about the PUBACK message.
     * @param pubackOutboundOutput The output object for handling the PUBACK message.
     */
    @Override
    public void onOutboundPuback(@NotNull PubackOutboundInput pubackOutboundInput,
            @NotNull PubackOutboundOutput pubackOutboundOutput) {
        LOGGER.debug("A PUBACK message sent to clientId: {} with messageId: {}", 
                pubackOutboundInput.getClientInformation().getClientId(), 
                pubackOutboundInput.getPubackPacket().getPacketIdentifier());

    }

}