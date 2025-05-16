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

/**
 * A cache to maintain some device related info for the current mqtt connection
 * of the device. Vehicle id for the device and also the topics the device has
 * actively subscribed to, are cached.
 */
public interface DeviceSubscriptionCache {

    /**
     * Adds a subscription for the specified device.
     *
     * @param deviceId The ID of the device.
     * @param deviceSubscription The subscription to be added.
     */
    void addSubscription(String deviceId, DeviceSubscription deviceSubscription);

    /**
     * Retrieves the subscription for the specified device.
     *
     * @param deviceId the ID of the device
     * @return the device subscription, or null if no subscription is found
     */
    DeviceSubscription getSubscription(String deviceId);

    /**
     * Adds a MQTT topic to the device subscription cache.
     *
     * @param deviceId   the ID of the device
     * @param vehicleId  the ID of the vehicle
     * @param mqttTopic  the MQTT topic to be added
     */
    void addTopic(String deviceId, String vehicleId, String mqttTopic);

    /**
     * Removes the specified MQTT topic from the device subscription cache.
     *
     * @param deviceId the ID of the device
     * @param mqttTopic the MQTT topic to be removed
     */
    void removeTopic(String deviceId, String mqttTopic);

    /**
     * Removes the subscription for the specified device.
     *
     * @param deviceId the ID of the device for which the subscription should be removed
     * @return the removed DeviceSubscription object, or null if no subscription was found for the specified device
     */
    DeviceSubscription removeSubscription(String deviceId);

}
