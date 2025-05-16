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

package org.eclipse.ecsp.hivemq.callbacks;

import org.eclipse.ecsp.cache.GetMapOfEntitiesRequest;
import org.eclipse.ecsp.cache.redis.IgniteCacheRedisImpl;
import org.eclipse.ecsp.domain.DeviceConnStatusV1_0.ConnectionStatus;
import org.eclipse.ecsp.domain.Version;
import org.eclipse.ecsp.entities.IgniteEntity;
import org.eclipse.ecsp.entities.dma.VehicleIdDeviceIdMapping;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCache;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
import org.eclipse.ecsp.hivemq.exceptions.InvalidSubscriptionException;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.utils.ConcurrentHashSet;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import static org.junit.Assert.assertTrue;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for SubscriptionStatusHandler.
 */
public class SubscriptionStatusHandlerTest {

    @Mock
    private HivemqSinkService hivemqSinkService;

    @Mock
    private IgniteCacheRedisImpl igniteCacheRedisImpl;

    @InjectMocks
    private SubscriptionStatusHandler subscriptionStatusHandler;

    /**
     * This setup method gets called before each test case and load required properties.
     */
    @Before
    public void setup() {
        initMocks(this);
        File path = new File(
                SubscriptionStatusHandlerTest.class.getClassLoader().getResource("hivemq-plugin-base.properties")
                .getFile());
        PropertyLoader.reload(path).put(AuthConstants.GLOBAL_SUB_TOPICS, Arrays.asList("ro, dvp"));
        PropertyLoader.loadRedisProperties("src/test/resources/");
        SubscriptionStatusHandler.setIgniteCacheRedisImpl(igniteCacheRedisImpl);
    }

    /**
     * Test case to verify the behavior of the subscription handle method.
     * 
     * <p>This method tests the functionality of sending status updates to a device.
     * It adds a subscription for a specific client ID, creates a set of topics,
     * and then calls the subscriptionStatusHandler to handle the client ID and topic list.
     * Finally, it asserts that the device subscription contains the expected topics.
     */
    @Test
    public void testSendStatus() {
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        PropertyLoader.getProperties().put(AuthConstants.GLOBAL_SUB_TOPICS, "CRNS");
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();

        String clientId = "StatusDeviceId";
        deviceSubscriptionCache.addSubscription(clientId, new DeviceSubscription(clientId));

        String topic1 = "haa/harman/dev/StatusDeviceId/2d/ro";
        String topic2 = "haa/harman/dev/StatusDeviceId/2d/ecall";
        String globalTopic = "haa/harman/dev/CRNS";
        Set<String> topicList = new HashSet<>();
        topicList.add(topic1);
        topicList.add(topic2);
        topicList.add(globalTopic);
        try {
            subscriptionStatusHandler.handle(clientId, topicList, ConnectionStatus.ACTIVE);
        } catch (InvalidSubscriptionException e) {
            e.printStackTrace();
        }
        DeviceSubscription deviceSubscription = deviceSubscriptionCache.removeSubscription(clientId);
        assertTrue(deviceSubscription.getSubscriptionTopics().contains(topic1));
        assertTrue(deviceSubscription.getSubscriptionTopics().contains(topic2));
        PropertyLoader.getProperties().remove(AuthConstants.GLOBAL_SUB_TOPICS);
    }

    /**
     * In this test case, we are testing the scenario in which connection status of
     * a devices goes to ACTIVE.
     */
    @Test
    public void testRedisActiveStatus() {
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        String clientId = "StatusDeviceIdActiveTest";
        deviceSubscriptionCache.addSubscription(clientId, new DeviceSubscription(clientId));

        String topic = "haa/harman/dev/StatusDeviceId/2d/ro";
        Set<String> topicSet = new HashSet<>();
        topicSet.add(topic);

        Map<String, IgniteEntity> map = new HashMap<>();
        ConcurrentHashSet<String> concurrentHashSet = new ConcurrentHashSet<>();
        concurrentHashSet.add("StatusDeviceIdActiveTest");
        VehicleIdDeviceIdMapping deviceIdMapping = new VehicleIdDeviceIdMapping(Version.V1_0, concurrentHashSet);

        map.put("StatusDeviceIdActiveTest", deviceIdMapping);
        Mockito.when(igniteCacheRedisImpl.getMapOfEntities(Mockito.any(GetMapOfEntitiesRequest.class))).thenReturn(map);
        try {
            subscriptionStatusHandler.handle(clientId, topicSet, ConnectionStatus.ACTIVE);
        } catch (InvalidSubscriptionException e) {
            e.printStackTrace();
        }
        String mapkey = "VEHICLE_DEVICE_MAPPING:ro";
        GetMapOfEntitiesRequest req = new GetMapOfEntitiesRequest();
        req.withKey(mapkey);
        Set<String> fields = new HashSet<String>();
        req.withFields(fields);
        Map<String, VehicleIdDeviceIdMapping> valueMap = igniteCacheRedisImpl.getMapOfEntities(req);
        VehicleIdDeviceIdMapping vehicleIdDeviceIdMapping = valueMap.get(clientId);
        assertTrue(vehicleIdDeviceIdMapping.getDeviceIds().contains(clientId));

    }

    /**
     * In this test case, we are testing the scenario in which connection status of
     * a devices goes from ACTIVE to INACTIVE. We have only one device for a given
     * key, hence once the device/client is removed, there shouldn't be any key left
     * in Cache.
     */
    @Test
    public void testRedisInActiveStatus() {
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        String clientId = "StatusDeviceIdInActiveTest";
        deviceSubscriptionCache.addSubscription(clientId, new DeviceSubscription(clientId));

        String topic = "haa/harman/dev/StatusDeviceId/2d/ecall";
        Set<String> topicList = new HashSet<>();
        topicList.add(topic);

        Map<String, IgniteEntity> map = new HashMap<>();
        ConcurrentHashSet<String> concurrentHashSet = new ConcurrentHashSet<>();
        concurrentHashSet.add("StatusDeviceIdInActiveTest");
        VehicleIdDeviceIdMapping deviceIdMapping = new VehicleIdDeviceIdMapping(Version.V1_0, concurrentHashSet);

        map.put("StatusDeviceIdInActiveTest", deviceIdMapping);
        Mockito.when(igniteCacheRedisImpl.getMapOfEntities(Mockito.any(GetMapOfEntitiesRequest.class))).thenReturn(map);
        try {
            subscriptionStatusHandler.handle(clientId, topicList, ConnectionStatus.ACTIVE);
            subscriptionStatusHandler.handle(clientId, topicList, ConnectionStatus.INACTIVE);
        } catch (InvalidSubscriptionException e) {
            e.printStackTrace();
        }

        String mapkey = "VEHICLE_DEVICE_MAPPING:ecall";
        GetMapOfEntitiesRequest req = new GetMapOfEntitiesRequest();
        req.withKey(mapkey);
        Set<String> fields = new HashSet<String>();
        req.withFields(fields);
        Map<String, VehicleIdDeviceIdMapping> valueMap = igniteCacheRedisImpl.getMapOfEntities(req);

        VehicleIdDeviceIdMapping vehicleIdDeviceIdMapping = null;
        if (valueMap != null && valueMap.containsKey(clientId)) {
            vehicleIdDeviceIdMapping = valueMap.get(clientId);
        }
    }

    /**
     * Test case to verify the removal of a device subscription.
     */
    @Test
    public void testRemoveDeviceSubscription() {
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        String clientId = "StatusDeviceIdInActiveTest";
        deviceSubscriptionCache.addSubscription(clientId, new DeviceSubscription(clientId));

        subscriptionStatusHandler.removeSubscription(clientId, false);

        Assert.assertNull(deviceSubscriptionCache.getSubscription(clientId));
    }

    /**
     * Test case to verify the behavior of the publish topic received.
     * 
     * <p>This test method performs the following steps:
     * 1. Loads the properties from the "hivemq-plugin-base.properties" file.
     * 2. Sets up the necessary variables and data structures.
     * 3. Mocks the behavior of the "igniteCacheRedisImpl" object.
     * 4. Calls the "handle" method of the "subscriptionStatusHandler" object with the specified parameters.
     * 5. Verifies the expected behavior by asserting that the device mapping contains the specified client ID.
     *
     * @throws InvalidSubscriptionException if an invalid subscription is encountered
     */
    @Test
    public void testPublishTopicReceived() {
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        String clientId = "StatusDeviceIdActiveTest";

        String topic = "haa/harman/dev/StatusDeviceId/2c/ro";
        Set<String> topicSet = new HashSet<>();
        topicSet.add(topic);

        Map<String, IgniteEntity> map = new HashMap<>();
        ConcurrentHashSet<String> concurrentHashSet = new ConcurrentHashSet<>();
        concurrentHashSet.add("StatusDeviceIdActiveTest");
        VehicleIdDeviceIdMapping deviceIdMapping = new VehicleIdDeviceIdMapping(Version.V1_0, concurrentHashSet);

        map.put("StatusDeviceIdActiveTest", deviceIdMapping);
        Mockito.when(igniteCacheRedisImpl.getMapOfEntities(Mockito.any(GetMapOfEntitiesRequest.class))).thenReturn(map);
        try {
            subscriptionStatusHandler.handle(clientId, topicSet, ConnectionStatus.ACTIVE);
        } catch (InvalidSubscriptionException e) {
            e.printStackTrace();
        }
        String mapkey = "VEHICLE_DEVICE_MAPPING:ro";
        GetMapOfEntitiesRequest req = new GetMapOfEntitiesRequest();
        req.withKey(mapkey);
        Set<String> fields = new HashSet<String>();
        req.withFields(fields);
        Map<String, VehicleIdDeviceIdMapping> valueMap = igniteCacheRedisImpl.getMapOfEntities(req);
        VehicleIdDeviceIdMapping vehicleIdDeviceIdMapping = valueMap.get(clientId);
        assertTrue(vehicleIdDeviceIdMapping.getDeviceIds().contains(clientId));
    }

    /**
     * Test case to verify the behavior of the method when the device status is not required.
     */
    @Test
    public void testDeviceStatusRequiredFalse() {
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        String clientId = "StatusDeviceIdActiveTest";
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        deviceSubscriptionCache.addSubscription(clientId, new DeviceSubscription(""));

        String topic = "haa/harman/dev/StatusDeviceId/2c/tcushieldevents";
        Set<String> topicSet = new HashSet<>();
        topicSet.add(topic);

        Map<String, IgniteEntity> map = new HashMap<>();
        ConcurrentHashSet<String> concurrentHashSet = new ConcurrentHashSet<>();
        concurrentHashSet.add("StatusDeviceIdActiveTest");
        VehicleIdDeviceIdMapping deviceIdMapping = new VehicleIdDeviceIdMapping(Version.V1_0, concurrentHashSet);

        map.put("StatusDeviceIdActiveTest", deviceIdMapping);
        Mockito.when(igniteCacheRedisImpl.getMapOfEntities(Mockito.any(GetMapOfEntitiesRequest.class))).thenReturn(map);
        try {
            subscriptionStatusHandler.handle(clientId, topicSet, ConnectionStatus.ACTIVE);
        } catch (InvalidSubscriptionException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test case to verify the behavior when an invalid topic mapping is encountered.
     * It expects an {@link InvalidSubscriptionException} to be thrown.
     */
    @Test(expected = InvalidSubscriptionException.class)
    public void testInvalidTopicMapping() {
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        String clientId = "StatusDeviceIdActiveTest";
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        deviceSubscriptionCache.addSubscription(clientId, new DeviceSubscription(""));

        String topic = "dummy";
        Set<String> topicSet = new HashSet<>();
        topicSet.add(topic);

        Map<String, IgniteEntity> map = new HashMap<>();
        ConcurrentHashSet<String> concurrentHashSet = new ConcurrentHashSet<>();
        concurrentHashSet.add("StatusDeviceIdActiveTest");
        VehicleIdDeviceIdMapping deviceIdMapping = new VehicleIdDeviceIdMapping(Version.V1_0, concurrentHashSet);

        map.put("StatusDeviceIdActiveTest", deviceIdMapping);
        Mockito.when(igniteCacheRedisImpl.getMapOfEntities(Mockito.any(GetMapOfEntitiesRequest.class))).thenReturn(map);

        subscriptionStatusHandler.handle(clientId, topicSet, ConnectionStatus.ACTIVE);

    }
}
