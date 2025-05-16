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

import org.eclipse.ecsp.hivemq.callbacks.MessageStoreCallbackTest;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

/**
 * Test class for DeviceSubscriptionCache.
 */
public class DeviceSubscriptionCacheTest {

    private DeviceSubscriptionCache deviceSubscriptionCache;
    private static final int CONNECTION_COUNTER = 2;

    /**
     * This setup method gets called before each test case and load required properties.
     */
    @Before
    public void setup() {
        File path = new File(
                MessageStoreCallbackTest.class.getClassLoader().getResource("hivemq-plugin-base.properties").getFile());
        PropertyLoader.reload(path);
        deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
    }

    /**
     * Test case to verify the addition of a topic for a device to the cache.
     * It adds a topic for a device, retrieves the device subscription from the cache,
     * and asserts that the added topic is present in the subscription's list of topics.
     */
    @Test
    public void testAddTopicForDeviceToCache() {
        String deviceId = "Device12";
        String mqttTopic = "haa/harman/dev/Device12/2c/ro";
        deviceSubscriptionCache.addTopic(deviceId, deviceId, mqttTopic);
        DeviceSubscription deviceSubscription = deviceSubscriptionCache.removeSubscription(deviceId);

        Assert.assertTrue(deviceSubscription.getSubscriptionTopics().contains(mqttTopic));
    }

    /**
     * Test case to verify the removal of a device from the cache.
     *
     * <p>This test adds multiple topics to the device subscription cache for a specific device.
     * Then, it removes one of the topics and verifies that the removed topic is no longer present 
     * in the device's subscription.
     * Finally, it asserts that the remaining topics are still present in the device's subscription.
     */
    @Test
    public void testRemoveDeviceFromCache() {
        String deviceId = "Device12";
        String mqttTopicRo = "haa/harman/dev/Device12/2c/ro";
        String mqttTopicEcall = "haa/harman/dev/Device12/2c/ecall";
        String mqttTopicBcall = "haa/harman/dev/Device12/2c/bcall";
        deviceSubscriptionCache.addTopic(deviceId, deviceId, mqttTopicRo);
        deviceSubscriptionCache.addTopic(deviceId, deviceId, mqttTopicEcall);
        deviceSubscriptionCache.addTopic(deviceId, deviceId, mqttTopicBcall);
        deviceSubscriptionCache.removeTopic(deviceId, mqttTopicRo);
        DeviceSubscription deviceSubscription = deviceSubscriptionCache.removeSubscription(deviceId);
        Assert.assertTrue(deviceSubscription.getSubscriptionTopics().contains(mqttTopicEcall));
        Assert.assertTrue(deviceSubscription.getSubscriptionTopics().contains(mqttTopicBcall));
    }

    /**
     * Test case for decrementing the device subscription.
     *
     * @throws NoSuchFieldException     if a field with the specified name is not found.
     * @throws SecurityException        if a security manager exists and its checkPermission method doesn't 
     *      allow access to the field.
     * @throws IllegalArgumentException if the specified object is not an instance of the class or interface 
     *      declaring the underlying field.
     * @throws IllegalAccessException if the field is inaccessible.
     */
    @Test
    public void testDecrementDeviceSubscription()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        DeviceSubscription deviceSubscription = new DeviceSubscription("vehicle-1", CONNECTION_COUNTER);
        deviceSubscription.decrConnectionsInfoCounter();
        Assert.assertEquals(1, deviceSubscription.getConnectionsInfoCounter());
    }
}
