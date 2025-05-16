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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.lang.reflect.InvocationTargetException;

/**
 * The TokenExpiryHandler class is responsible for managing token expiration in a cache.
 * It provides methods to put, validate, and remove client entries from the cache.
 * The cache can be enabled or disabled based on the configuration.
 * Super users, defined in the property file, are exempted from token expiration validation.
 *
 * @author Binoy Mandal
 */
public final class TokenExpiryHandler {
    private TokenExpiryHandler() {
    }

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(TokenExpiryHandler.class);
    private static ExpiryCache cache;
    private static boolean isEnabled;
    private static boolean preventLWTMessage;

    static {
        isEnabled = Boolean.valueOf(PropertyLoader.getValue(ApplicationConstants.EXPIRY_CACHE_ENABLED));
        LOGGER.warn("Expiry Cache enable status:{}", isEnabled);
        preventLWTMessage = Boolean.valueOf(PropertyLoader.getValue(ApplicationConstants.PREVENT_LWT_MESSAGE));
        String className = StringUtils.EMPTY;
        try {
            className = PropertyLoader.getValue(ApplicationConstants.EXPIRY_CACHE_IMPL_CLASS);
            if (isEnabled) {
                cache = (ExpiryCache) TokenExpiryHandler.class.getClassLoader().loadClass(className)
                        .getDeclaredConstructor().newInstance();
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            LOGGER.error("{} refers to a class ({}) that is not available on the classpath",
                    ApplicationConstants.EXPIRY_CACHE_IMPL_CLASS, className);
            throw new IllegalArgumentException(ApplicationConstants.EXPIRY_CACHE_IMPL_CLASS + " refers to a class ("
                    + className + ") that is not available on the classpath");
        }
    }

    /**
     * put client entry in local cache.
     *
     * @param clientId - client id
     * @param expTime  - authentication expiry time
     * @param username - username
     */
    public static void put(String clientId, Long expTime, String username) {
        if (isEnabled && !isSuperUser(username, clientId)) {
            cache.put(clientId, expTime);
        }
    }

    /**
     * This method validates token expiry from local cache.
     *
     * @param clientId - client id
     * @param username - usename
     */
    public static void validateTokenExpiration(String clientId, String username) {
        if (isEnabled && !isSuperUser(username, clientId)) {
            cache.validateTokenExpiration(clientId, preventLWTMessage);
        }
    }

    /**
     * This method removes client entry from local cache.
     *
     * @param clientId - client id
     * @param username - username
     */
    public static void remove(String clientId, String username) {
        if (isEnabled && !isSuperUser(username, clientId)) {
            LOGGER.trace("removing clientId: {} from cache for user: {}", clientId, username);
            cache.remove(clientId);
        }
    }

    /**
     * Identify super user.
     *
     * @param username a non empty user which is defined in property file.
     * @return return true if user is defined in property file.
     */
    private static boolean isSuperUser(String username, String clientId) {
        if (HivemqUtils.isWhiteListedUser(username)) {
            LOGGER.debug("It is a super user. userName {} and clientId: {}", username, clientId);
            return true;
        } else {
            return false;
        }
    }
}
