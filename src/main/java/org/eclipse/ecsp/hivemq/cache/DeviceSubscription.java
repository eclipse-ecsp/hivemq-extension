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

package org.eclipse.ecsp.hivemq.cache;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * It contains the vehicleId and all subscribed mqtt topic of a deviceid.
 */
@ToString
@Getter
public class DeviceSubscription {
    private Set<String> subscriptionTopics = Collections.synchronizedSet(new HashSet<>());
    private final String vehicleId;
    @Setter
    private boolean isSuspicious;
    @Setter
    private Optional<String> deviceType = Optional.empty();
    // counter for duplicate connection with with same Id
    private int connectionsInfoCounter = AuthConstants.SINGLE_CONNECTION;
    @Setter
    private boolean isSsdpVehicle;

    public DeviceSubscription(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    /**
     * Constructor to create DeviceSubscription with vehicle id and connection counter.
     *
     * @param vehicleId - vehicle id
     * @param connectionCounter - connection counter
     */
    public DeviceSubscription(String vehicleId, int connectionCounter) {
        this.vehicleId = vehicleId;
        if (connectionCounter > AuthConstants.SINGLE_CONNECTION) {
            connectionsInfoCounter = connectionCounter;
        }
    }


    /**
     * Adds a new subscription topic to the device's list of subscription topics.
     *
     * @param mqttTopic the MQTT topic to be added as a subscription
     */
    public void addSubscription(String mqttTopic) {
        subscriptionTopics.add(mqttTopic);
    }

    /**
     * Removes the specified MQTT topic from the subscription topics.
     *
     * @param mqttTopic the MQTT topic to be removed
     */
    public void removeSusbcription(String mqttTopic) {
        subscriptionTopics.remove(mqttTopic);
    }

    /**
     * Increases the connections info counter by a single connection.
     */
    public void incrConnectionsInfoCounter() {
        this.connectionsInfoCounter += AuthConstants.SINGLE_CONNECTION;
    }

    /**
     * Decreases the connections info counter by a single connection.
     */
    public void decrConnectionsInfoCounter() {
        this.connectionsInfoCounter -= AuthConstants.SINGLE_CONNECTION;
    }

}
