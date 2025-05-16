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

import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Before;
import org.junit.Test;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import static org.eclipse.ecsp.hivemq.cache.TokenExpiryHandler.put;
import static org.eclipse.ecsp.hivemq.cache.TokenExpiryHandler.remove;
import static org.eclipse.ecsp.hivemq.cache.TokenExpiryHandler.validateTokenExpiration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * ExpiryCache testing for client certificate validation.
 *
 * @author Binoy Mandal
 */
public class TokenExpiryHandlerTest {

    private static final String CLIENT_ID = "client1";
    private static final String USERNAME = "DeviceTesting123";
    private static final String WHITELIST_USER = "haa_api";
    // private static final String SUPER_USER = "haa_api";
    private static final String PROPERTY_FILE_NAME = "hivemq-plugin-base.properties";
    private IgniteExpiryCacheImpl impl;
    private String tokenExpiryHandlerClassName = "org.eclipse.ecsp.hivemq.cache.TokenExpiryHandler";
    private String wrongTokenExpiryHandlerClassName = "org.eclipse.ecsp.hivemq.cache.TokenExpiryHandler1";
    private ConcurrentHashMap<String, Long> internalMap;
    private static final int ONE_THOUSAND = 1000;
    private static final int TEN_THOUSAND = 100;
    private static final int TEN = 10;

    /**
     * This setup method gets called before each test case and load required properties.
     *
     * @throws Exception throws when not able to read property file
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Before
    public void setup() throws Exception {
        PropertyLoader
                .getProperties(TokenExpiryHandlerTest.class.getClassLoader().getResource(PROPERTY_FILE_NAME).getPath());
        Class clazz = Class.forName(tokenExpiryHandlerClassName);
        Field field = clazz.getDeclaredField("cache");
        field.setAccessible(true);
        impl = (IgniteExpiryCacheImpl) field.get(null);
        field = impl.getClass().getDeclaredField("clientIdExpMap");
        field.setAccessible(true);
        internalMap = (ConcurrentHashMap<String, Long>) field.get(impl);
    }

    /**
     * clientId's certificate expiration is valid. Should not throw any exception
     */
    @Test
    public void testPutForSuccess() {
        Long exp = (System.currentTimeMillis() / ONE_THOUSAND) + TEN_THOUSAND;
        put(CLIENT_ID, exp, USERNAME);
        validateTokenExpiration(CLIENT_ID, USERNAME);
        assertEquals(exp, internalMap.get(CLIENT_ID));
    }

    /**
     * clientId's certificate expiration is invalid. Should throw
     * RefusedConnectionException
     */
    @Test
    public void testPutForFail() {
        Long exp = (System.currentTimeMillis() / ONE_THOUSAND) - TEN;
        put(CLIENT_ID, exp, USERNAME);
        remove(CLIENT_ID, USERNAME);
        assertNull(internalMap.get(CLIENT_ID));
    }

    /**
        * Test case to verify the behavior of the `put` method when adding a whitelisted user.
        * It checks if the user is correctly added to the internal map and if the previous user is removed.
        */
    @Test
    public void testWhitelistUserPut() {
        Long exp = (System.currentTimeMillis() / ONE_THOUSAND) + TEN_THOUSAND;
        put(CLIENT_ID, exp, WHITELIST_USER);
        remove(CLIENT_ID, USERNAME);
        assertNull(internalMap.get(CLIENT_ID));
    }

}
