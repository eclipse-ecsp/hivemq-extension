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

package org.eclipse.ecsp.hivemq.utils;

import com.hivemq.extension.sdk.api.services.session.ClientService;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;

/**
 * Hivemq core services provider.
 *
 * @author Binoy Mandal
 */
public class HivemqServiceProvider {
    private HivemqServiceProvider() {
    }
    
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(HivemqServiceProvider.class);

    private static ClientService blockingClientService;

    /**
     * Returns the blocking client service.
     *
     * @return the blocking client service
     */
    public static ClientService getBlockingClientService() {
        return blockingClientService;
    }

    /**
     * Sets the blocking client service.
     *
     * @param blockingClientService the blocking client service to be set
     */
    public static void setBlockingClientService(ClientService blockingClientService) {
        LOGGER.debug("BlockingClientService is initialized with:{}", blockingClientService);
        HivemqServiceProvider.blockingClientService = blockingClientService;
    }

}
