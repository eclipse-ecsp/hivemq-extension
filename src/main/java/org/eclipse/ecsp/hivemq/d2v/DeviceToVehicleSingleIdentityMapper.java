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

package org.eclipse.ecsp.hivemq.d2v;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.services.Services;
import org.eclipse.ecsp.hivemq.exceptions.VehicleProfileResponseNotFoundException;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.utils.HttpClient;
import org.eclipse.ecsp.hivemq.utils.VehicleProfileApiClient;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * It is a Ignite specific implementation for DeviceId to vehicleId mapping.In
 * Ignite deviceId and vehicleId are same.
 */
public class DeviceToVehicleSingleIdentityMapper implements DeviceToVehicleMapper {
    private static final String AT = "@";
    private static final String UNDERSCORE = "_";
    private static final String DMPORTAL = "dmportal_";
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(DeviceToVehicleSingleIdentityMapper.class);
    private static final int RETRY_COUNT = 2;
    private static final int SLEEP_BEFORE_RETRY = 10;

    /**
     * Retrieves the vehicle ID associated with the given device ID.
     * It consider deviceId as VehicleId.
     *
     * @param deviceId The device ID for which to retrieve the vehicle ID.
     * @param attributeStore The ConnectionAttributeStore used to retrieve connection attributes.
     * @return The VehicleInfo object containing the vehicle ID.
     * @throws VehicleProfileResponseNotFoundException If the vehicle profile response is not found.
     */
    @Override
    public CompletableFuture<VehicleInfo> getVehicleId(String deviceId, ConnectionAttributeStore attributeStore)
            throws VehicleProfileResponseNotFoundException {
        int retry = 0;
        if (deviceId.contains(DMPORTAL) || deviceId.contains(UNDERSCORE) || deviceId.contains(AT)) {
            return CompletableFuture.completedFuture(new VehicleInfo(deviceId));
        }
        final MetricRegistry metricRegistry = Services.metricRegistry();
        metricRegistry.counter(ApplicationConstants.GET_VEHICLEID_COUNTER_JMX).inc();
        final Timer.Context timerContext = metricRegistry.timer(ApplicationConstants.GET_VEHICLEID_TIMER_JMX).time();
        return getVehicleData(deviceId, retry, attributeStore)
                .whenComplete((vehicleInfo, throwable) -> timerContext.stop());
    }

    /**
     * Retrieves the vehicle data for the specified device ID.
     *
     * @param deviceId       The ID of the device.
     * @param retry          The number of retries attempted.
     * @param attributeStore The connection attribute store.
     * @return The vehicle information.
     * @throws VehicleProfileResponseNotFoundException If the vehicle profile response is not found.
     */
    private CompletableFuture<VehicleInfo> getVehicleData(final String deviceId, final int retry,
            ConnectionAttributeStore attributeStore)
            throws VehicleProfileResponseNotFoundException {
        final MetricRegistry metricRegistry = Services.metricRegistry();
        metricRegistry.counter(ApplicationConstants.GET_VEHICLEDATA_COUNTER_JMX).inc();
        final Timer.Context timerContext = metricRegistry.timer(ApplicationConstants.GET_VEHICLEDATA_TIMER_JMX).time();
        return VehicleProfileApiClient.getVehicleInfo(deviceId, attributeStore).thenApply(vehicleInfo1 -> {
            vehicleInfo1.setVehicleId(deviceId);
            return vehicleInfo1;
        }).<CompletableFuture<VehicleInfo>>handle((vehicleInfo, throwable) -> {
            timerContext.stop();
            if (throwable != null) {
                metricRegistry.counter(ApplicationConstants.GET_VEHICLEDATA_EXCEPTION_COUNT_JMX).inc();
                if (retry < RETRY_COUNT) {
                    LOGGER.debug("VehicleData fetch retry for clientId : {}, retryCount: {}", deviceId, retry);
                    return CompletableFuture.runAsync(() -> {},
                            CompletableFuture.delayedExecutor(SLEEP_BEFORE_RETRY, TimeUnit.MILLISECONDS))
                            .thenCompose(unused -> getVehicleData(deviceId, retry + 1, attributeStore));
                }
                LOGGER.error("VehicleData fetch failed for clientId : {}", deviceId, retry);
                return CompletableFuture.failedFuture(throwable);
            }
            return CompletableFuture.completedFuture(vehicleInfo);
        }).thenCompose(Function.identity());
    }

    /**
     * Initializes the DeviceToVehicleSingleIdentityMapper with the given properties.
     *
     * @param prop the properties to initialize the mapper with
     */
    @Override
    public void init(Properties prop) {
        HttpClient.init(prop);
    }

}
