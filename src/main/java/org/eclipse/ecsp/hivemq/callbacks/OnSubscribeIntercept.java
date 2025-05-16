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

import com.hivemq.extension.sdk.api.packets.subscribe.Subscription;
import org.eclipse.ecsp.domain.DeviceConnStatusV1_0.ConnectionStatus;
import org.eclipse.ecsp.hivemq.exceptions.InvalidSubscriptionException;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import java.util.Set;

/**
 * This class handles subscribe requests coming from clients.
 */
@Component
public class OnSubscribeIntercept extends AbstractSubscribeInboundInterceptor {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(OnSubscribeIntercept.class);

    @Autowired
    private SubscriptionStatusHandler handler;

    /**
        * Performs the subscription operation for a given client and subscription.
        *
        * @param clientId     the ID of the client performing the subscription
        * @param subscription the subscription to be performed
        * @throws InvalidSubscriptionException if the subscription is invalid
        */
    @Override
    public void doSubscribe(String clientId, Subscription subscription) throws InvalidSubscriptionException {
        Set<String> topicSet = new HashSet<>();
        topicSet.add(subscription.getTopicFilter());
        LOGGER.debug("Subscribe to topic: {}. Logging scope: E2E, DVI for clientId: {}", topicSet, clientId);
        handler.handle(clientId, topicSet, ConnectionStatus.ACTIVE);
    }

    // only for junit test
    /**
     * Sets the subscription status handler for this object.
     *
     * @param handler the subscription status handler to be set
     */
    public void setHandler(SubscriptionStatusHandler handler) {
        this.handler = handler;
    }

}
