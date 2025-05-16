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
import com.hivemq.extension.sdk.api.interceptor.puback.PubackInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackInboundInput;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackInboundOutput;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;

/**
 * This class intercepts incoming acknowledgement message from client.
 * Once client receives message from hivemq, it will send back an ack.
 * This ack will be sent for Qos 1 and Qos 2 messages.
 */
@Component
public class PubackReceived implements PubackInboundInterceptor {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PubackReceived.class);

    /**
        * This method is called when a PUBACK message is received.
        * It logs the clientId and messageId of the received message.
        *
        * @param pubackInboundInput  The input object containing information about the received PUBACK message.
        * @param pubackInboundOutput The output object for handling the PUBACK message.
        */
    @Override
    public void onInboundPuback(@NotNull PubackInboundInput pubackInboundInput,
            @NotNull PubackInboundOutput pubackInboundOutput) {
        LOGGER.debug("A PUBACK message received from clientId: {} with messageId: {}", 
                pubackInboundInput.getClientInformation().getClientId(),
                pubackInboundInput.getPubackPacket().getPacketIdentifier());

    }
}