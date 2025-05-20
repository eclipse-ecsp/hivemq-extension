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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.base.VehicleProfileDataExtraction;
import org.eclipse.ecsp.hivemq.d2v.VehicleInfo;
import org.eclipse.ecsp.hivemq.exceptions.VehicleProfileResponseNotFoundException;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * This class does the vehicle profile api call, using the jdk client.
 */
public class VehicleProfileApiClient {
    private static final @NotNull IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(VehicleProfileApiClient.class);
    private static final @NotNull ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final @NotNull HttpClient HTTP_CLIENT = new HttpClient();
    private static @Nullable VehicleProfileDataExtraction vehicleProfileDataExtraction;

    private VehicleProfileApiClient() {
    }

    
    /**
     * Retrieves the {@link VehicleInfo} for the specified device ID asynchronously.
     *
     * <p>
     * This method fetches vehicle details for the given device ID and parses the response
     * into a {@link VehicleInfo} object using the provided {@link ConnectionAttributeStore}.
     * </p>
     *
     * @param deviceId the unique identifier of the device whose vehicle information is to be retrieved
     * @param attributeStore the attribute store used for additional connection attributes during parsing
     * @return a {@link CompletableFuture} that, when completed, contains the {@link VehicleInfo} 
     *      for the specified device
     * @throws VehicleProfileResponseNotFoundException if the vehicle profile response is not found
     */
    public static @NotNull CompletableFuture<VehicleInfo> getVehicleInfo(
            final String deviceId, final ConnectionAttributeStore attributeStore)
                    throws VehicleProfileResponseNotFoundException {
        return getVehicleDetails(deviceId).thenApply(jsonNode -> parseVehicleInfo(jsonNode, deviceId, attributeStore));
    }

    
    /**
     * Retrieves vehicle details for the specified device ID by making an asynchronous HTTP request
     * to the VehicleProfile service. The method constructs the request URL with the provided device ID
     * as a query parameter, sends the request asynchronously, and parses the JSON response.
     *
     * @param deviceId the unique identifier of the device whose vehicle details are to be fetched
     * @return a {@link CompletableFuture} containing the {@link JsonNode} representation of the vehicle details,
     *         or {@code null} if the VehicleProfile URL is not configured
     * @throws VehicleProfileResponseNotFoundException if the vehicle profile response is not found
     */
    public static @NotNull CompletableFuture<JsonNode> getVehicleDetails(final String deviceId)
            throws VehicleProfileResponseNotFoundException {
        final Map<String, String> keyValue = new HashMap<>();
        keyValue.put(HttpClient.getDeviceIdParam(), deviceId);

        final java.net.http.HttpClient httpClient = HTTP_CLIENT.createDefaultHttpBuilder();
        final String vehicleProfileUrl = HttpClient.getVehicleProfileUrl();
        if (vehicleProfileUrl != null) {
            final HttpUrl.Builder urlBuilder = HttpUrl.parse(vehicleProfileUrl).newBuilder();

            for (final Entry<String, String> entry : keyValue.entrySet()) {
                urlBuilder.addQueryParameter(entry.getKey(), entry.getValue());
            }
            final HttpUrl url = urlBuilder.build();
            LOGGER.info("Hitting VehicleProfile {}", url.toString());

            final HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url.toString()))
                    .timeout(HttpClient.getRequestTimeoutInSec())
                    .build();

            return httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString())
                    .thenApply(stringHttpResponse -> {
                        try {
                            return JSON_MAPPER.readTree(stringHttpResponse.body());
                        } catch (final JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                    })
                    .whenComplete((ignored, throwable) -> {
                        if (throwable != null) {
                            if (throwable instanceof CompletionException) {
                                LOGGER.warn("Error while hitting VehicleProfile service. url: " + url,
                                        throwable.getCause());
                            } else {
                                LOGGER.warn("Error while hitting VehicleProfile service. url: " + url, throwable);
                            }
                        }
                    });
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Parses the vehicle information from the given JSON node and returns a VehicleInfo object.
     *
     * @param node           the JSON node containing the vehicle data
     * @param deviceId       the ID of the device
     * @param attributeStore the ConnectionAttributeStore to store the extracted vehicle profile data
     * @return the parsed VehicleInfo object, or null if the vehicle data is empty or the response is a failure
     * @throws VehicleProfileResponseNotFoundException if the vehicle data response is not found
     */
    private static VehicleInfo parseVehicleInfo(
            final JsonNode node, final String deviceId, final ConnectionAttributeStore attributeStore)
                    throws VehicleProfileResponseNotFoundException {
        VehicleInfo vinfo = null;

        if (Objects.isNull(node)) {
            LOGGER.error("VehicleData response is empty, So deviceType will not be identified for clientId : {} .",
                    deviceId);
            return null;
        }
        final JsonNode messageNode = node.findValue(AuthConstants.MESSAGE_NODE_KEY);
        if ((messageNode == null) || !(AuthConstants.MESSAGE_SUCCESS.equalsIgnoreCase(messageNode.textValue()))) {
            LOGGER.error("VehicleData response failure, So deviceType will not be identified for clientId : {} .",
                    deviceId);
            throw new VehicleProfileResponseNotFoundException("VehicleData response failure");
        }

        final JsonNode dataNode = node.findValue(AuthConstants.DATA_NODE_KEY);
        if (dataNode == null || dataNode.size() == 0) {
            LOGGER.error("VehicleData is empty, So deviceType will not be identified for clientId : {} .", deviceId);
            return vinfo;
        }

        final String vehicleIdKey = PropertyLoader.getProperties().getProperty(AuthConstants.VEHICLE_ID_KEY);
        final JsonNode vehicleJsonNode = dataNode.findValue(vehicleIdKey);
        if (vehicleJsonNode == null || StringUtils.isEmpty(vehicleJsonNode.textValue())) {
            LOGGER.error("VehicleData VehicleId is empty, So deviceType will not be identified for clientId : {} .",
                    deviceId);
            return null;
        }

        final String vehicleId = vehicleJsonNode.textValue();
        String deviceTypeKey = PropertyLoader.getProperties().getProperty(AuthConstants.DEVICE_TYPE_PARAM);
        JsonNode deviceTypeJsonNode = dataNode.findValue(deviceTypeKey);
        String deviceType = deviceTypeJsonNode.textValue();
        vinfo = new VehicleInfo(vehicleId, Optional.of(deviceType));

        LOGGER.info("ClientId {} - {}", deviceId, vinfo);
        try {
            vehicleProfileDataExtraction.extractVehicleProfileData(node, deviceId, attributeStore);
            JsonNode connectedPlatformJson = dataNode.findValue(AuthConstants.CONNECTED_PLATFORM);
            if (connectedPlatformJson != null) {
                String connectedPlatform = connectedPlatformJson.textValue();
                attributeStore.putAsString(AuthConstants.CONNECTED_PLATFORM,
                        connectedPlatform != null ? connectedPlatform : "");
            }
        } catch (final Exception ex) {
            LOGGER.error("Error while extracting extra vehicle profile data for deviceId:{} {}", deviceId, ex);
        }
        return vinfo;
    }

    /**
     * Sets the vehicle profile data extraction instance.
     *
     * @param vehicleProfileDataExtraction The vehicle profile data extraction instance to set.
     */
    public static void setVehicleProfileDataExtraction(
            final @NotNull VehicleProfileDataExtraction vehicleProfileDataExtraction) {
        VehicleProfileApiClient.vehicleProfileDataExtraction = vehicleProfileDataExtraction;
    }

}