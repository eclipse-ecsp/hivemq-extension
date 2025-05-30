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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import java.util.Optional;

/**
 * DTO class for vehicle information.
 *
 * @author Binoy Mandal
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class VehicleInfo {

    private String vehicleId;
    private Optional<String> deviceType;

    /**
     * Constructs a new VehicleInfo object with the specified vehicle ID.
     *
     * @param vehicleId the ID of the vehicle
     */
    public VehicleInfo(String vehicleId) {
        this.vehicleId = vehicleId;
        this.deviceType = Optional.empty();
    }
}
