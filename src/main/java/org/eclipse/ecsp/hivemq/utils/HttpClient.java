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

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.time.Duration;
import java.util.Objects;
import java.util.Properties;

/**
 * Http client which uses for HTTP call.
 */
public class HttpClient {

    private static final @NotNull IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(HttpClient.class);
    private static @Nullable Duration connectionTimeoutInSec;
    private static @Nullable Duration requestTimeoutInSec;
    private static @Nullable String deviceIdParam;
    private static @Nullable String vehicleProfileURL;
    private @Nullable java.net.http.HttpClient javaHttpClient;

    /**
     * This method initialize http client, which will be used to fetch data from
     * vehicle-profile.
     */
    public static void init(final @NotNull Properties prop) {
        connectionTimeoutInSec = Duration.ofSeconds(
                Long.parseLong(prop.getProperty("http.connection.timeout.in.sec", "10")));
        requestTimeoutInSec = Duration.ofSeconds(
                Long.parseLong(prop.getProperty("http.request.timeout.in.sec", "1")));
        LOGGER.debug("HTTPClient Properties: connectionTimeoutInSec:{}, requestTimeoutInSec:{}",
                connectionTimeoutInSec,
                requestTimeoutInSec);
        deviceIdParam = prop.getProperty("d2v.http.request.device.id.param");
        vehicleProfileURL = prop.getProperty("d2v.http.url");
        LOGGER.debug("VehicleProfile: deviceIdParam, vehicleProfileUrl: {}", deviceIdParam, vehicleProfileURL);
    }

    /**
     * It returns the HttpClient client which supports HTTP protocol.
     *
     * @return HttpClient
     */
    public @NotNull java.net.http.HttpClient createDefaultHttpBuilder() {
        if (javaHttpClient == null) {
            javaHttpClient = java.net.http.HttpClient.newBuilder().connectTimeout(connectionTimeoutInSec).build();
            LOGGER.info("Default httpClient created ..");
        }
        return Objects.requireNonNull(javaHttpClient);
    }

    public static @NotNull Duration getRequestTimeoutInSec() {
        return Objects.requireNonNull(requestTimeoutInSec);
    }

    public static @NotNull String getDeviceIdParam() {
        return Objects.requireNonNull(deviceIdParam);
    }

    public static @Nullable String getVehicleProfileUrl() {
        return vehicleProfileURL;
    }

}