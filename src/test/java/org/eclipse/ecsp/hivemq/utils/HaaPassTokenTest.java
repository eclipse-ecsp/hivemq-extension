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

import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * The HaaPassToken generates and validates a HaaPaasToken.
 */
public class HaaPassTokenTest {

    @Mock
    SimpleAuthOutput simpleAuthOutput;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
    }

    /**
     * Test validateToken() when the token creation date is not available.
     */
    @Test
    public void testValidateTokenWhenCreatedDateNull() throws IOException {
        String token = "{\"access_token\": \"52d736c72f5379ec89d49bc28aa8453\","
                + "\"userName\": \"akumar\",\"expires_in\": \"1000\"}";
        HaaPassToken hpt = new HaaPassToken(token);
        hpt.validateToken("abc", simpleAuthOutput);
    }

    /**
     * Test validateToken() when the token userName is difference from the userName
     * being passed to the validate method.
     */
    @Test
    public void testValidateTokenWhenTokenUserInvalid() throws IOException {
        String token = "{\"access_token\": \"52d736c72f5379ec89d49bc28aa8453\","
                + "\"userName\":\"akumar\", \"expires_in\": \"299\",\"createdDate\":\"" + getCurrentDate() + "\"}";
        HaaPassToken hpt = new HaaPassToken(token);
        hpt.validateToken("tokenUser", simpleAuthOutput);
    }

    /**
     * Test validateToken() method for an expired token Expect.
     */
    @Test
    public void testValidateTokenWhenExpiredToken() {
        HaaPassToken hpt = new HaaPassToken("testToken", "testUser", "-1000", getCurrentDate());
        hpt.validateToken("testUser", simpleAuthOutput);
    }

    /**
     * Test validateToken() successful case.
     */
    @Test
    public void testValidateTokenSuccess() {
        HaaPassToken hpt = new HaaPassToken("testToken", "testUser", "1000", getCurrentDate());
        assertTrue(hpt.validateToken("testUser", simpleAuthOutput));
    }

    /**
     * Test validateToken() method for a token, having date in an incorrect format.
     */
    @Test
    public void testValidateTokenWhenDateParsingFails() {
        HaaPassToken hpt = new HaaPassToken("testToken", "testUser", "1000", "5May2019");
        hpt.validateToken("testUser", simpleAuthOutput);
    }

    /**
     * Added only for test purpose. used in testValidateTokenWhenTokenUserInvalid of
     * this class.
     */
    private String getCurrentDate() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date = new Date();
        return formatter.format(date);
    }

}
