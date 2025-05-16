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

import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.eclipse.ecsp.hivemq.utils.DynamicPropertyUpdater;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class to close the kafka on stop of the hivemq broker.
 */
@Component
public class HiveStopCallback {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(HiveStopCallback.class);

    private static final String BROKERSTOP_WAIT_MAXCOUNT = "waitCountBeforeBrokerStop";
    private static final String BROKERSTOP_WAIT_TIMEPERCOUNT = "waitTimeMsBeforeBrokerStop";
    private static final String ACTIVE_CLIENTS_COUNT = "Active Clients count : {} ";

    @Autowired
    private ScheduledExecutorService extensionDisconnectExecutorService;

    /**
     * This method does cleanup job when hivemq broker sthuts down.
     *
     * <p>This method will wait for configured time and check all clients got disconnected gracefully or not.
     */
    public void onBrokerStop() {
        LOGGER.info("Checking Active clients");
        int waitTimeMs = HivemqUtils.getPropertyIntValue(BROKERSTOP_WAIT_TIMEPERCOUNT);
        int maxWaitCount = HivemqUtils.getPropertyIntValue(BROKERSTOP_WAIT_MAXCOUNT);

        int counter = 0;
        while (isClientsConnected() && ++counter < maxWaitCount) {
            try {
                LOGGER.info("Waiting for all Active clients to get disconnected");
                Thread.sleep(waitTimeMs);
            } catch (InterruptedException e) {
                LOGGER.error("Caught InterruptedException during sleep", e);
            }

        }

        if (counter == maxWaitCount) {
            LOGGER.error("Some clients are still active that might result in data loss");
        }
        LOGGER.info("Gracefully closing of Sink services.");
        HivemqSinkService.getInstance().flush();
        HivemqSinkService.getInstance().close();
        LOGGER.info("Gracefully closed of Sink services.");
        DynamicPropertyUpdater.shutdown();

        if (extensionDisconnectExecutorService != null) {
            LOGGER.info("Gracefully shuttingdown disconnect executorservice.");
            extensionDisconnectExecutorService.shutdown();
        }
    }

    /**
     * Checks if there are any connected clients.
     *
     * @return true if there are connected clients, false otherwise.
     */
    boolean isClientsConnected() {
        final ClientService clientService = Services.clientService();
        final AtomicInteger counter = new AtomicInteger();

        CompletableFuture<Void> iterationFuture = clientService
            .iterateAllClients((context, sessionInformation) -> {
                if (sessionInformation.isConnected()) {
                    counter.incrementAndGet();
                }
            });

        iterationFuture.whenComplete((ignored, throwable) -> {
            if (throwable == null) {
                LOGGER.info("Connected clients: " + counter.get());
            } else {
                LOGGER.error(throwable.getMessage());
            }
        });

        if (counter.get() == 0) {
            return false;
        }

        LOGGER.info(ACTIVE_CLIENTS_COUNT, counter.get());
        return true;
    }

}
