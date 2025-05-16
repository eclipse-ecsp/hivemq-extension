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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test class for IgniteExpiryCacheImpl.
 */
public class IgniteExpiryCacheImplTest {

    IgniteExpiryCacheImpl igniteExpiryCacheImpl;
    ConcurrentHashMap<String, Long> clientIdExpMap;
    private static final int YEARS = 1000;

    /**
     * This setup method gets called before each test case and load required properties.
     *
     * @throws Exception throws when not able to read property file
     */
    @Before
    public void setUp() throws Exception {
        igniteExpiryCacheImpl = new IgniteExpiryCacheImpl();
        Field clientIdExpMapField = igniteExpiryCacheImpl.getClass().getDeclaredField("clientIdExpMap");
        clientIdExpMapField.setAccessible(true);
        clientIdExpMap = (ConcurrentHashMap<String, Long>) clientIdExpMapField.get(igniteExpiryCacheImpl);
    }

    /**
     * Test case for the put method of the IgniteExpiryCacheImpl class.
     * 
     * <p>This test verifies the behavior of the put method under different scenarios:
     * - Case_1: Invalid ClientId - The method should not add the mapping to the cache and the mapping 
     * count should remain 0.
     * - Case_2: Invalid exp - The method should not add the mapping to the cache and the mapping count should remain 0.
     * - Case_3: Valid inputs - The method should add the mapping to the cache and the mapping count should be 1. 
     * The value associated with the client ID should be equal to the provided expiration time.
     */
    @Test
    public void testPut() {
        // Case_1: Invalid ClientId
        igniteExpiryCacheImpl.put("", 0L);
        Assert.assertEquals(0, clientIdExpMap.mappingCount());

        // Case_2: Invalid exp
        igniteExpiryCacheImpl.put("client1", null);
        Assert.assertEquals(0, clientIdExpMap.mappingCount());

        // Case_3 valid inputs
        Long exp = Timestamp.valueOf(LocalDateTime.now()).getTime();
        igniteExpiryCacheImpl.put("client1", exp);
        Assert.assertEquals(1, clientIdExpMap.mappingCount());
        Assert.assertEquals(clientIdExpMap.get("client1"), exp);
    }

    /**
     * This method tests the validation of token expiration in the IgniteExpiryCacheImpl class.
     * It covers three cases:
     * 1. Case_1: When the token has expired and HivemqServiceProvider.getBlockingClientService() is null.
     * 2. Case_2: When HivemqServiceProvider.getBlockingClientService() is non-null.
     * 3. Case_3: When the token has not expired.
     * 
     * <p>For each case, it puts a token with a specific expiration time in the cache, validates the token expiration,
     * and asserts the expected mapping count in the clientIdExpMap.
     */
    @Test
    public void testValidateTokenExpiration() {
        // Case_1: token expired & HivemqServiceProvider.getBlockingClientService() is
        // null
        Long exp = Timestamp.valueOf(LocalDateTime.now().minusYears(YEARS)).getTime();
        igniteExpiryCacheImpl.put("client1", exp);
        igniteExpiryCacheImpl.validateTokenExpiration("client1", false);
        Assert.assertEquals(0, clientIdExpMap.mappingCount());

        // Case_2 HivemqServiceProvider.getBlockingClientService() is non null
        exp = Timestamp.valueOf(LocalDateTime.now().minusYears(YEARS)).getTime();
        igniteExpiryCacheImpl.put("client1", exp);
        HivemqServiceProvider.setBlockingClientService(Mockito.mock(ClientService.class));
        igniteExpiryCacheImpl.validateTokenExpiration("client1", false);
        Assert.assertEquals(0, clientIdExpMap.mappingCount());

        // Case_3 token not expired
        exp = Timestamp.valueOf(LocalDateTime.now()).getTime();
        igniteExpiryCacheImpl.put("client1", exp);
        HivemqServiceProvider.setBlockingClientService(Mockito.mock(ClientService.class));
        igniteExpiryCacheImpl.validateTokenExpiration("client1", false);
        Assert.assertEquals(1, clientIdExpMap.mappingCount());

    }

    /**
    * Test case to verify the behavior of removing an invalid entry from the cache.
    * It puts a value in the cache for a client, removes an invalid entry, 
    * and then checks if the mapping count is as expected.
    */
    @Test
    public void testRemoveInvalid() {
        Long exp = Timestamp.valueOf(LocalDateTime.now()).getTime();
        igniteExpiryCacheImpl.put("client1", exp);
        igniteExpiryCacheImpl.remove("");
        Assert.assertEquals(1, clientIdExpMap.mappingCount());
    }

}
