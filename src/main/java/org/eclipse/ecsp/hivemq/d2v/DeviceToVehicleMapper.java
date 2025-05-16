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

import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import org.eclipse.ecsp.hivemq.exceptions.VehicleProfileResponseNotFoundException;
import java.util.concurrent.CompletableFuture;

/**
 * Implementation of this interface should handle the device id to vehicle id
 * conversion.
 */
public interface DeviceToVehicleMapper {

    /**
     * It returns the vehicleId on the basis of deviceId.
     *
     * @param deviceId - device id
     * @param attributeStore - client connection attribute store
     * @return vehicle information
     */
    CompletableFuture<VehicleInfo> getVehicleId(String deviceId, ConnectionAttributeStore attributeStore)
            throws VehicleProfileResponseNotFoundException;

    /**
     * Initializes the DeviceToVehicleMapper with the given properties.
     *
     * @param prop the properties to initialize the mapper with
     */
    void init(java.util.Properties prop);
}
