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

package org.eclipse.ecsp.hivemq.cache;

import com.hivemq.extension.sdk.api.services.session.ClientService;
import org.eclipse.ecsp.hivemq.utils.HivemqServiceProvider;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;

/**
 * This class provides implementation to ExpiryCache, and is used to validate token expiry.
 */
public class IgniteExpiryCacheImpl implements ExpiryCache {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(IgniteExpiryCacheImpl.class);
    private static final int ONE_THOUSAND_MS = 1000;

    private ConcurrentHashMap<String, Long> clientIdExpMap = new ConcurrentHashMap<>();

    /**
        * Adds or updates the expiration timestamp for a given client ID.
        *
        * @param clientId The client ID to associate with the expiration timestamp.
        * @param expTimestamp The expiration timestamp to be associated with the client ID.
        */
    @Override
    public void put(String clientId, Long expTimestamp) {
        if (validateClientId(clientId) && validateExp(expTimestamp)) {
            LOGGER.debug("clientId: {} is registered with exp timestamp: {}", clientId, expTimestamp);
            clientIdExpMap.put(clientId, expTimestamp);
        } else {
            LOGGER.debug("clientId: {} is not registered with exp timestamp: {}", clientId, expTimestamp);
        }
    }

    /**
     * Validates the expiration of a token for a given client ID.
     * If the token is expired, it removes the client ID from the cache and disconnects the client.
     *
     * @param clientId          the client ID to validate the token expiration for
     * @param preventLwtMessage flag indicating whether to prevent the Last Will and Testament (LWT) message
     */
    @Override
    public void validateTokenExpiration(String clientId, boolean preventLwtMessage) {
        Long timestamp = clientIdExpMap.get(clientId);
        if (validateClientId(clientId) && validateExp(timestamp) && isExpired(timestamp)) {
            remove(clientId);
            LOGGER.error("clientId: {} is expired. Expired timestamp is:{}", clientId, timestamp);
            ClientService service = HivemqServiceProvider.getBlockingClientService();
            if (Objects.nonNull(service)) {
                service.disconnectClient(clientId, preventLwtMessage);
                LOGGER.info("clientId: {} is disconnected due to expiration and preventLWTMessage status: {}", clientId,
                        preventLwtMessage);
            } else {
                LOGGER.warn("Client service is not set for clientId: {}. Forcefully disconnect will not happen. "
                        + "Please check in PluginMainClass", clientId);
            }
        } else {
            LOGGER.debug("clientId: {} is not expired. So not removing entry from cache", clientId);
        }
    }

    /**
     * Checks if the given timestamp is expired.
     *
     * @param timestamp the timestamp to check
     * @return true if the timestamp is expired, false otherwise
     */
    public boolean isExpired(long timestamp) {
        return timestamp < (System.currentTimeMillis() / ONE_THOUSAND_MS);
    }

    /**
        * Removes the specified client ID from the cache.
        *
        * @param clientId the client ID to be removed
        */
    @Override
    public void remove(String clientId) {
        if (validateClientId(clientId)) {
            clientIdExpMap.remove(clientId);
            LOGGER.debug("clientId: {} is removed from cache", clientId);
        } else {
            LOGGER.debug("clientId: {} is not removed from cache", clientId);
        }
    }

    /**
     * Validates the client ID.
     *
     * @param clientId the client ID to validate
     * @return true if the client ID is not empty, false otherwise
     */
    private boolean validateClientId(String clientId) {
        if (isEmpty(clientId)) {
            LOGGER.error("clientId  is empty");
            return false;
        }
        return true;
    }

    /**
     * Validates the expiration timestamp.
     *
     * @param exp the expiration timestamp to validate
     * @return true if the expiration timestamp is not null, false otherwise
     */
    private boolean validateExp(Long exp) {
        if (isNull(exp)) {
            LOGGER.error("exp timestamp is empty");
            return false;
        }
        return true;
    }
}
