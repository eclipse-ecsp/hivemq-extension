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
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.UnsubscribeInboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundOutput;
import org.eclipse.ecsp.domain.DeviceConnStatusV1_0.ConnectionStatus;
import org.eclipse.ecsp.hivemq.exceptions.InvalidSubscriptionException;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class intercept all unsubscribe requests.
 */
@Component
public class UnsubscribeInboundIntercept implements UnsubscribeInboundInterceptor {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(UnsubscribeInboundIntercept.class);

    @Autowired
    private SubscriptionStatusHandler handler;

    /**
        * This method is called when a client sends an unsubscribe packet to the broker.
        * It handles the unsubscribe operation by extracting the client ID and topic filters,
        * and then passing them to the handler for further processing.
        *
        * @param unsubscribeInboundInput  The input object containing information about the unsubscribe operation.
        * @param unsubscribeInboundOutput The output object used to control the behavior of the unsubscribe operation.
        */
    @Override
    public void onInboundUnsubscribe(@NotNull UnsubscribeInboundInput unsubscribeInboundInput,
            @NotNull UnsubscribeInboundOutput unsubscribeInboundOutput) {
        String clientId = unsubscribeInboundInput.getClientInformation().getClientId();
        Set<String> topicSet = unsubscribeInboundInput.getUnsubscribePacket().getTopicFilters().stream()
                .collect(Collectors.toSet());
        LOGGER.info("Unsubscription clientId: {} , topics: {}", clientId, topicSet);
        try {
            handler.handle(clientId, topicSet, ConnectionStatus.INACTIVE);
        } catch (InvalidSubscriptionException e) {
            LOGGER.error("Unsubscribe tried by a client, clientId: " + clientId + " without a vehicleId ", e);
        }
    }
}
