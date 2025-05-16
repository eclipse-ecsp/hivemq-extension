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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.domain.User;
import org.eclipse.ecsp.hivemq.domain.UserPayload;
import org.eclipse.ecsp.hivemq.domain.UsersApiResponse;
import org.eclipse.ecsp.hivemq.exceptions.UrlNotFoundException;
import org.eclipse.ecsp.hivemq.exceptions.UserNotFoundException;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Optional;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.HEADER_ACCEPT;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.HEADER_CONTENT_TYPE;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.HEADER_CORRELATIONID;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.USER_MANAGEMENT_API_URL;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.USER_MANAGEMENT_BASE_URL;

/**
 * This class calls user-management api to fetch user country code.
 */
public class UserManagementUtil {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(UserManagementUtil.class);
    private RestTemplate restTemplate = new RestTemplate();

    /**
     * This method hits user-management api to fetch user country code.
     *
     * @param userId - user id
     * @return country code
     */
    public String getUserCountry(String userId) {
        String country = "";
        UserPayload payload = new UserPayload(userId);
        String userManagementBaseUrl = PropertyLoader.getValue(USER_MANAGEMENT_BASE_URL);
        String userManagementApiUrl = PropertyLoader.getValue(USER_MANAGEMENT_API_URL);
        if (StringUtils.isBlank(userManagementBaseUrl) || StringUtils.isBlank(userManagementApiUrl)) {
            LOGGER.error("Base : {} or API : {} URL for userManagement API not configured properly ",
                    userManagementBaseUrl, userManagementApiUrl);
            throw new UrlNotFoundException("Base or API URL for userManagement API not found");
        }
        String url = getUserManagementApiUrl(userManagementBaseUrl, userManagementApiUrl);
        HttpEntity<UserPayload> entity = new HttpEntity<>(payload, getUserManagementHeaders(userId));
        ResponseEntity<UsersApiResponse> response = restTemplate.exchange(url, HttpMethod.POST, entity,
                UsersApiResponse.class);
        UsersApiResponse responseBody = response.getBody();
        if (response.getStatusCode().equals(HttpStatus.OK) && Optional.ofNullable(responseBody).isPresent()
                && !responseBody.getResults().isEmpty()) {
            List<User> users = responseBody.getResults();
            User user = users.get(0);
            country = user.getCountry();
            LOGGER.debug("Successfully Obtained response and logged in user's {} country is {}", userId, country);

        } else {
            LOGGER.error("User details not found for {}", userId);
            throw new UserNotFoundException(
                    MessageFormatter.format("User details not found for {}", userId).getMessage());
        }
        return country;

    }

    /**
     * Constructs the complete URL for the user management API by appending the base URL and the API URL.
     *
     * @param userManagementBaseUrl The base URL of the user management service.
     * @param userManagementApiUrl  The API URL of the user management service.
     * @return The complete URL for the user management API.
     */
    private String getUserManagementApiUrl(String userManagementBaseUrl, String userManagementApiUrl) {
        return userManagementBaseUrl.endsWith("/") ? userManagementBaseUrl + userManagementApiUrl
                : userManagementBaseUrl + "/" + userManagementApiUrl;
    }

    /**
     * Returns the HttpHeaders object with the necessary headers for user management.
     *
     * @param userId the user ID to set as the correlation ID header
     * @return the HttpHeaders object with the necessary headers
     */
    private HttpHeaders getUserManagementHeaders(String userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HEADER_CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HEADER_ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HEADER_CORRELATIONID, userId);
        return headers;
    }
}
