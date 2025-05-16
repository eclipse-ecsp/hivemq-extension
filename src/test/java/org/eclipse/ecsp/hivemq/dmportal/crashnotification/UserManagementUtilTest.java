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

package org.eclipse.ecsp.hivemq.dmportal.crashnotification;

import org.eclipse.ecsp.hivemq.domain.User;
import org.eclipse.ecsp.hivemq.domain.UsersApiResponse;
import org.eclipse.ecsp.hivemq.exceptions.UrlNotFoundException;
import org.eclipse.ecsp.hivemq.exceptions.UserNotFoundException;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.hivemq.utils.UserManagementUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for UserManagementUtil.
 */
@SuppressWarnings(value = { "unused" })
@RunWith(MockitoJUnitRunner.class)
public class UserManagementUtilTest {

    @Mock
    private RestTemplate template;

    @InjectMocks
    private UserManagementUtil util;

    private User user;

    private UsersApiResponse apiResponse;

    /**
     * This setup method gets called before each test case and load required properties.
     *
     * @throws Exception Throws exception when property file not found.
     */
    @Before
    public void setUp() throws Exception {
        initMocks(this);
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        user = new User();
        apiResponse = new UsersApiResponse();
    }

    /**
     * Test case to verify the successful retrieval of permissions for the DM Portal client.
     * 
     * <p>This test sets up the necessary data and mocks the required dependencies to simulate the scenario.
     * It then executes the method under test and verifies the expected result.
     * 
     * <p>Test Steps:
     * 1. Set the country of the user to "ID".
     * 2. Set the API response with a single user.
     * 3. Create a mock response entity with the API response and HTTP status OK.
     * 4. Mock the exchange method of the template to return the mock response entity.
     * 5. Call the getUserCountry method of the util object with a test user ID.
     * 6. Verify that the retrieved country matches the expected country of the user.
     * 
     * <p>Expected Result:
     * The retrieved country should match the expected country of the user.
     */
    @Test
    public void testGetPermissionsForDmPortalClient_success() {
        // given - preparation
        user.setCountry("ID");
        apiResponse.setResults(Collections.singletonList(user));
        ResponseEntity<UsersApiResponse> response = new ResponseEntity<UsersApiResponse>(apiResponse, HttpStatus.OK);
        when(template.exchange(Mockito.anyString(), Mockito.any(HttpMethod.class), Mockito.any(HttpEntity.class),
                Matchers.<Class<UsersApiResponse>>any())).thenReturn(response);
        // when-execution
        String userId = "l3-admin@test.com";
        String country = util.getUserCountry(userId);

        // then-verification - expected URLNotFoundException exception.
        Assert.assertEquals(user.getCountry(), country);
    }

    @Test(expected = UserNotFoundException.class)
    public void testGetPermissionsForDmPortalClient_EmptyResponseBody() throws Exception {
        // given - preparation
        String userId = "l3-admin@test.com";
        UsersApiResponse apiResponse = new UsersApiResponse();
        apiResponse.setResults(Collections.emptyList());
        ResponseEntity<UsersApiResponse> response = new ResponseEntity<UsersApiResponse>(apiResponse, HttpStatus.OK);
        when(template.exchange(Mockito.anyString(), Mockito.any(HttpMethod.class), Mockito.any(HttpEntity.class),
                Matchers.<Class<UsersApiResponse>>any())).thenReturn(response);
        // when-execution
        String country = util.getUserCountry(userId);
        // then-verification - expected URLNotFoundException exception.
    }

    /**
     * Test case to verify the behavior of the getUserCountry method when the HTTP status is BAD_REQUEST.
     * It expects a UserNotFoundException to be thrown.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test(expected = UserNotFoundException.class)
    public void testGetPermissionsForDmPortalClient_HttpStatusBr() throws Exception {
        // given - preparation
        String userId = "l3-admin@test.com";
        UsersApiResponse apiResponse = new UsersApiResponse();
        apiResponse.setResults(Collections.emptyList());
        ResponseEntity<UsersApiResponse> response = new ResponseEntity<UsersApiResponse>(apiResponse,
                HttpStatus.BAD_REQUEST);
        when(template.exchange(Mockito.anyString(), Mockito.any(HttpMethod.class), Mockito.any(HttpEntity.class),
                Matchers.<Class<UsersApiResponse>>any())).thenReturn(response);
        // when-execution
        String country = util.getUserCountry(userId);
        // then-verification - expected URLNotFoundException exception.
    }

    /**
     * Test case to verify the behavior when the base URL is not found for the DM Portal client.
     * It expects the {@link UrlNotFoundException} to be thrown.
     *
     * @throws Exception if an error occurs during the test execution.
     */
    @Test(expected = UrlNotFoundException.class)
    public void testGetPermissionsForDmPortalClient_BaseUrlNotFound() throws Exception {
        // given - preparation
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties")
                .replace("user.management.base.url", "");
        String userId = "l3-admin@test.com";
        // when-execution
        String country = util.getUserCountry(userId);
        // then-verification - expected URLNotFoundException exception.
    }

    /**
     * Test case to verify the behavior of the getUserCountry method when the API URL is not found.
     * It expects the UrlNotFoundException to be thrown.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test(expected = UrlNotFoundException.class)
    public void testGetPermissionsForDmPortalClient_ApiUrlNotFound() throws Exception {
        // given - preparation
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties")
                .replace("user.management.api.url", "");
        String userId = "l3-admin@test.com";
        // when-execution
        String country = util.getUserCountry(userId);
        // then-verification - expected URLNotFoundException exception.
    }
}
