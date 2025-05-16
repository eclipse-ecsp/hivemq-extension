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

import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

/**
 * It stores the mapping of deviceid and all its Mqtt subscribed topics.
 */
public class DeviceSubscriptionCacheInMemoryImpl implements DeviceSubscriptionCache {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(DeviceSubscriptionCacheInMemoryImpl.class);
    // <deviceid, <DeviceSubscription>>
    private ConcurrentHashMap<String, DeviceSubscription> deviceToMqttTopicMap = new ConcurrentHashMap<>();

    /**
     * It adds subscribed mqtt topic of a device to cache.
     *
     * @param deviceId - device id
     * @param vehicleId - vehicle id
     * @param mqttTopic - mqtt topic which needs to be added in local cache
     */
    @Override
    public void addTopic(String deviceId, String vehicleId, String mqttTopic) {
        DeviceSubscription deviceSubscription = deviceToMqttTopicMap.get(deviceId);
        if (deviceSubscription == null) {
            // While removing a serviceid from list of value, set list as
            // synchronized
            deviceSubscription = new DeviceSubscription(vehicleId);
            LOGGER.debug("Adding vehicleId {} in cache for deviceId {}", vehicleId, deviceId);
            boolean isAdded = (null == deviceToMqttTopicMap.putIfAbsent(deviceId, deviceSubscription));
            LOGGER.debug("Added/updated vehicleId {} in cache for deviceId {} , isAdded {}", vehicleId, deviceId,
                    isAdded);
        }
        LOGGER.debug("Adding mqtt topic subscription {} in cache for deviceId {}", mqttTopic, deviceId);
        deviceSubscription.addSubscription(mqttTopic);
    }

    /**
     * It remove all subscribed mqtt topic details of a deviceId from cache.
     *
     * @param deviceId - device id
     * @return device subscription local cache
     */
    @Override
    public DeviceSubscription removeSubscription(String deviceId) {
        LOGGER.debug("Removing all subscriptions data from cache for deviceId {}", deviceId);
        return deviceToMqttTopicMap.remove(deviceId);
    }

    /**
     * It only removes the serviceid from the list of subscription values.
     *
     * @param deviceId - device id
     * @param mqttTopic - topic needs to be removed from local cache
     */
    @Override
    public void removeTopic(String deviceId, String mqttTopic) {
        LOGGER.debug("Removing only subscription data {} from cache for deviceId {}", mqttTopic, deviceId);
        DeviceSubscription deviceSubscription = deviceToMqttTopicMap.get(deviceId);
        if (deviceSubscription != null) {
            deviceSubscription.removeSusbcription(mqttTopic);
        }
    }

    /**
        * Retrieves the subscription for the specified device ID.
        *
        * @param deviceId the ID of the device
        * @return the device subscription associated with the device ID, or null if not found
        */
    @Override
    public DeviceSubscription getSubscription(String deviceId) {
        return deviceToMqttTopicMap.get(deviceId);
    }

    /**
     * Adds a subscription for a device in the cache.
     *
     * @param deviceId The ID of the device.
     * @param deviceSubscription The subscription details for the device.
     */
    @Override
    public void addSubscription(String deviceId, DeviceSubscription deviceSubscription) {
        LOGGER.debug("Adding deviceId {} and DeviceSubscription {} in cache", deviceId, deviceSubscription);
        // There is chance of vehicleId set as null (such as vehicle profile
        // timeout, so vehicleId will be null and might be that client
        // disconnect ungracefully), so in each connect request override the
        // existing cache
        deviceToMqttTopicMap.put(deviceId, deviceSubscription);
    }
}
