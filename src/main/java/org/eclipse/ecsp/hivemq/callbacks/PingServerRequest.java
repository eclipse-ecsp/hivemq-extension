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

import com.hivemq.extension.sdk.api.interceptor.pingreq.parameter.PingReqInboundInput;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import lombok.Setter;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.utils.HivemqServiceProvider;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;

/**
 * This class intercept ping requests from client and check if client is connected or not,
 * if not connected then then call disconnect.
 *
 * @author Neha Khan
 */
@Component
@Setter
public class PingServerRequest extends AbstractPingReqInboundInterceptor {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PingServerRequest.class);
    private boolean disconnectOnPingIfNotConnected;
    private ClientService blockingClientService;

    /**
     * Default constructor to load property values.
     */
    public PingServerRequest() {
        disconnectOnPingIfNotConnected = Boolean
                .parseBoolean(PropertyLoader.getValue(ApplicationConstants.DISCONNECT_ONPING_IF_NOT_CONNECTED_ENABLED));
        blockingClientService = HivemqServiceProvider.getBlockingClientService();
    }

    /**
     * Handles the received ping request from the client.
     *
     * @param pingReqInboundInput The input containing information about the ping request.
     */
    @Override
    public void doPingReceived(PingReqInboundInput pingReqInboundInput) {
        String clientId = pingReqInboundInput.getClientInformation().getClientId();
        if (disconnectOnPingIfNotConnected) {
            CompletableFuture<Boolean> connectedFuture = blockingClientService.isClientConnected(clientId);
            connectedFuture.whenComplete((connected, throwable) -> {
                if (throwable == null) {
                    if (!connected.booleanValue()) {
                        blockingClientService.disconnectClient(clientId);
                        LOGGER.warn("Disconnected clientId: {} as client is active and but hivemq thinks "
                                + "it is not connected:", clientId);
                    } else {
                        LOGGER.trace("Ping request to server from client: clientId: {} and isClientConnected: {}",
                                clientId, connected);
                    }
                } else {
                    LOGGER.error("Ping clientId: {} is inactive exception encountered: {}", clientId,
                            throwable.getMessage());
                }
            });
        } else {
            LOGGER.trace("Ping request to server from client: clientId: {} ", clientId);
        }

    }

    /**
     * Returns whether forced client disconnect is enabled.
     *
     * @return true if forced client disconnect is enabled, false otherwise
     */
    public boolean isForcedClientDisconnectEnabled() {
        return disconnectOnPingIfNotConnected;
    }

    /**
     * Initializes the client service by obtaining the blocking client service from the HivemqServiceProvider.
     */
    public void initializeClientService() {
        blockingClientService = HivemqServiceProvider.getBlockingClientService();
    }

}
