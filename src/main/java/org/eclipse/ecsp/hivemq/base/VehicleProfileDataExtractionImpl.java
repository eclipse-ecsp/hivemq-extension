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

package org.eclipse.ecsp.hivemq.base;

import com.fasterxml.jackson.databind.JsonNode;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import org.springframework.stereotype.Component;

/**
 * This class provides a default implementation to VehicleProfileDataExtraction, and by default does nothing.
 */
@Component
public class VehicleProfileDataExtractionImpl implements VehicleProfileDataExtraction {
    /**
        * Extracts vehicle profile data from the given JSON node.
        *
        * @param node           the JSON node containing the vehicle profile data
        * @param deviceId       the ID of the device
        * @param attributeStore the connection attribute store
        * @throws Exception if an error occurs during the extraction process
        */
    @Override
    public void extractVehicleProfileData(JsonNode node, String deviceId, ConnectionAttributeStore attributeStore)
            throws Exception {
        // do nothing
    }
}
