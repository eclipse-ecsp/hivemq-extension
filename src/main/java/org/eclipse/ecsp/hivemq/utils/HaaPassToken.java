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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.extension.sdk.api.auth.parameter.SimpleAuthOutput;
import com.hivemq.extension.sdk.api.packets.connect.ConnackReasonCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * This class provides methods for token conversion and validation.
 */
@NoArgsConstructor
@Getter
@Setter
@ToString
public class HaaPassToken implements Serializable {

    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(HaaPassToken.class);
    private static final String ACCESS_TOKEN = "access_token";
    private static final String USER_NAME = "userName";
    private static final String EXPIRES_IN = "expires_in";
    private static final String CREATED_DATE = "createdDate";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long serialVersionUID = -1599776677648827268L;
    private String accessToken; // using underscore to match json format
    private String userName;
    private String expiresIn; // using underscore to match json format
    private String createdDate;
    private static final Long MS_IN_SEC = 1000L;

    /**
     * constructor to set field values.
     *
     * @param accessToken - access token
     * @param userName - username
     * @param expiresIn - expiry time
     * @param createdDate - created date of token
     */
    public HaaPassToken(String accessToken, String userName, String expiresIn, String createdDate) {
        super();
        this.accessToken = accessToken;
        this.userName = userName;
        this.expiresIn = expiresIn;
        this.createdDate = createdDate;
    }

    /**
     * This method creates reads field values from json.
     *
     * @param str - token string
     * @throws IOException - throws if unable to process json
     */
    public HaaPassToken(String str) throws IOException {
        JsonNode propNode = MAPPER.readValue(str, JsonNode.class);

        if (propNode.has(ACCESS_TOKEN)) {
            this.accessToken = propNode.get(ACCESS_TOKEN).asText();
        }
        if (propNode.has(USER_NAME)) {
            this.userName = propNode.get(USER_NAME).asText();
        }
        if (propNode.has(EXPIRES_IN)) {
            this.expiresIn = propNode.get(EXPIRES_IN).asText();
        }
        if (propNode.has(CREATED_DATE)) {
            this.createdDate = propNode.get(CREATED_DATE).asText();
        }
    }

    /**
     * This method validates a token.
     *
     * @param userName - client username
     * @param simpleAuthOutput - hivemq SimpleAuthOutput object
     * @return true if token is valid else false
     */
    public boolean validateToken(String userName, SimpleAuthOutput simpleAuthOutput) {
        if (getCreatedDate() == null || getExpiresIn() == null) {
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                    "Authentication token format is incorrect");
            return false;
        }
        String tokenUserName = getUserName();

        if (!tokenUserName.equals(userName)) {
            LOGGER.error("User {} is using authentication token from another user {} ", userName, tokenUserName);
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                    "Authentication failed. Using unauthorized token");
            return false;
        }
        // format sample 03-11-2016 11:48:59 UTC
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date date = formatter.parse(createdDate);
            long createdTimestamp = date.getTime() / MS_IN_SEC;
            long currentTimestamp = System.currentTimeMillis() / MS_IN_SEC;
            long expireTimestamp = createdTimestamp + Long.parseLong(expiresIn);

            if (currentTimestamp < expireTimestamp) {
                return true;
            } else {
                LOGGER.error(
                        "Authentication token is expired!. userName: {}, currentTimestamp: {}, expireTimestamp: {}",
                        userName, currentTimestamp, expireTimestamp);
                simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                        "Authentication token is expired!");
            }
        } catch (ParseException e) {
            LOGGER.error("Date parsing failed for user: {}, createdDate: {}", userName, createdDate);
            simpleAuthOutput.failAuthentication(ConnackReasonCode.BAD_USER_NAME_OR_PASSWORD,
                    "Authentication failed!. Date Parsing failed");
        }
        return false;
    }
}
