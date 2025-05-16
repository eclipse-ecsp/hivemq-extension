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

package org.eclipse.ecsp.hivemq.simulator;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerInput;
import com.hivemq.extension.sdk.api.auth.parameter.SubscriptionAuthorizerOutput;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission.MqttActivity;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission.PermissionType;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.subscribe.Subscription;
import com.hivemq.extension.sdk.api.services.Services;
import org.eclipse.ecsp.cache.redis.IgniteCacheRedisImpl;
import org.eclipse.ecsp.domain.DeviceConnStatusV1_0.ConnectionStatus;
import org.eclipse.ecsp.hivemq.auth.authorization.stub.StubTopicPermissionBuilder;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCache;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
import org.eclipse.ecsp.hivemq.callbacks.SubscriptionCallbackTest;
import org.eclipse.ecsp.hivemq.callbacks.SubscriptionStatusHandler;
import org.eclipse.ecsp.hivemq.exceptions.InvalidSubscriptionException;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for SimulatorOnSubscribeIntercept. 
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Services.class, HivemqSinkService.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class SimulatorOnSubscribeInterceptTest {

    @Mock
    private SubscriptionAuthorizerInput subscribeInboundInput;
    @Mock
    private SubscriptionAuthorizerOutput subscribeInboundOutput;
    @Mock
    private ConnectionInformation connectionInformation;
    @Mock
    private ConnectionAttributeStore connectionAttributeStore;

    @Mock
    HivemqSinkService hivemqSinkService;
    @Mock
    IgniteCacheRedisImpl igniteCacheRedisImpl;
    @Mock
    private MetricRegistry registry;

    @Mock
    private SubscriptionStatusHandler handler;

    SimulatorOnSubscribeIntercept subscribeIntercept;

    private Subscription subscription;

    String clientId = "7fd12f24-3341-40f6-8714-a54515e92d2b";
    String topicName = "haa/harman/dev/haa_api/2d/ro";

    static {
        File path = new File(
                SubscriptionCallbackTest.class.getClassLoader().getResource("hivemq-plugin-base.properties").getFile());
        PropertyLoader.reload(path);
        PropertyLoader.loadRedisProperties("src/test/resources");
    }

    /**
     * This setup method gets called before each test case and load required properties.
     */
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ModifiableDefaultPermissions defaultPermissions = Mockito.mock(ModifiableDefaultPermissions.class);
        List<TopicPermission> topicPermissions = new ArrayList<>();
        TopicPermission topicPermission = new StubTopicPermissionBuilder().topicFilter(topicName)
                .activity(MqttActivity.SUBSCRIBE).type(PermissionType.ALLOW).build();

        topicPermissions.add(topicPermission);
        when(defaultPermissions.asList()).thenReturn(topicPermissions);
        subscription = Mockito.mock(Subscription.class);
        when(subscription.getTopicFilter()).thenReturn(topicName);
        when(subscribeInboundInput.getSubscription()).thenReturn(subscription);
        String data = "test data";
        byte[] payload = data.getBytes();
        doNothing().when(hivemqSinkService).sendMsgToSink(payload, payload, "test");
    }

    /**
     * Test case for the onInboundSubscribe for Vehicle method.
     * This method tests the behavior of the SimulatorOnSubscribeIntercept class when subscribing to a vehicle.
     * It mocks the necessary dependencies and verifies that the handler is called with the correct parameters.
     */
    @Test
    public void testOnInboundSubscribeVehicle() {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(registry);
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        Mockito.mock(SubscriptionStatusHandler.class);

        subscribeIntercept = new SimulatorOnSubscribeIntercept();
        subscribeIntercept.setHandler(handler);
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        DeviceSubscription deviceSubscription = new DeviceSubscription(clientId);
        deviceSubscription.setSsdpVehicle(false);
        deviceSubscriptionCache.addSubscription(clientId, deviceSubscription);
        when(subscribeInboundInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        when(subscribeInboundInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(subscribeInboundInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        when(subscribeInboundInput.getConnectionInformation().getConnectionAttributeStore().getAsString("username"))
                .thenReturn(Optional.of("admin"));
        subscribeIntercept.doSubscribe(clientId, subscription);
        Set<String> topicSet = new HashSet<>();
        topicSet.add("haa/harman/dev/haa_api/2d/ro");
        verify(handler, times(1)).handle(clientId, topicSet, ConnectionStatus.ACTIVE);
    }

    /**
     * Test case for the `onInboundSubscribe` for SsdpVehicle method.
     * This method tests the behavior of the `doSubscribe` method in the `SimulatorOnSubscribeIntercept` class
     * when the device subscription has the `ssdpVehicle` flag set to true.
     * It verifies that the appropriate message is sent to the sink service and the device subscription is removed.
     */
    @Test
    public void testOnInboundSubscribeSsdpVehicle() {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(registry);
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        Mockito.mock(SubscriptionStatusHandler.class);
        PropertyLoader.getProperties().put(AuthConstants.GLOBAL_SUB_TOPICS, "csrn");
        subscribeIntercept = new SimulatorOnSubscribeIntercept();
        subscribeIntercept.setHandler(handler);
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        DeviceSubscription deviceSubscription = new DeviceSubscription(clientId);
        deviceSubscription.setSsdpVehicle(true);
        deviceSubscriptionCache.addSubscription(clientId, deviceSubscription);
        when(subscribeInboundInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        when(subscribeInboundInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(subscribeInboundInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        when(subscribeInboundInput.getConnectionInformation().getConnectionAttributeStore().getAsString("username"))
                .thenReturn(Optional.of("admin"));
        subscribeIntercept.doSubscribe(clientId, subscription);
        Set<String> topicSet = new HashSet<>();
        topicSet.add("haa/harman/dev/haa_api/2d/ro");
        verify(hivemqSinkService, times(1)).sendMsgToSink(Mockito.anyString(), Mockito.any(), Mockito.anyString());
        deviceSubscriptionCache.removeSubscription(clientId);
        PropertyLoader.getProperties().remove(AuthConstants.GLOBAL_SUB_TOPICS);
    }

    /**
     * Test case to verify the behavior of the SimulatorOnSubscribeIntercept class.
     * when an invalid subscription exception is expected due to a topic format error
     */
    @Test(expected = InvalidSubscriptionException.class)
    public void testOnInboundSubscribeSsdpVehicleTopicFormatError() {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(registry);
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        Mockito.mock(SubscriptionStatusHandler.class);

        subscribeIntercept = new SimulatorOnSubscribeIntercept();
        subscribeIntercept.setHandler(handler);
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        DeviceSubscription deviceSubscription = new DeviceSubscription(clientId);
        deviceSubscription.setSsdpVehicle(true);
        deviceSubscriptionCache.addSubscription(clientId, deviceSubscription);
        when(subscribeInboundInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        when(subscribeInboundInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(subscribeInboundInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        when(subscribeInboundInput.getConnectionInformation().getConnectionAttributeStore().getAsString("username"))
                .thenReturn(Optional.of("admin"));
        String topic = "dummy";
        when(subscription.getTopicFilter()).thenReturn(topic);
        subscribeIntercept.doSubscribe(clientId, subscription);
        Set<String> topicSet = new HashSet<>();
        topicSet.add(topic);
        deviceSubscriptionCache.removeSubscription(clientId);
    }

    /**
     * Test case to verify the behavior of the `onInboundSubscribe` method when the SSDP vehicle 
     * device status is not required.
     */
    @Test
    public void testOnInboundSubscribeSsdpVehicleDeviceStatusNotRequired() {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(registry);
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        Mockito.mock(SubscriptionStatusHandler.class);

        subscribeIntercept = new SimulatorOnSubscribeIntercept();
        subscribeIntercept.setHandler(handler);
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        DeviceSubscription deviceSubscription = new DeviceSubscription(clientId);
        deviceSubscription.setSsdpVehicle(true);
        deviceSubscriptionCache.addSubscription(clientId, deviceSubscription);
        when(subscribeInboundInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        when(subscribeInboundInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(subscribeInboundInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        when(subscribeInboundInput.getConnectionInformation().getConnectionAttributeStore().getAsString("username"))
                .thenReturn(Optional.of("admin"));
        String topic = "haa/harman/dev/haa_api/2d/events";
        when(subscription.getTopicFilter()).thenReturn(topic);
        subscribeIntercept.doSubscribe(clientId, subscription);
        Set<String> topicSet = new HashSet<>();
        topicSet.add(topic);
        verify(hivemqSinkService, times(0)).sendMsgToSink(Mockito.anyString(), Mockito.any(), Mockito.anyString());
        deviceSubscriptionCache.removeSubscription(clientId);
    }

    /**
     * Test case for the `onInboundSubscribe` method when the device is marked as an SSDP vehicle and suspicious.
     * This test verifies that the `doSubscribe` method is not called when the device is marked as an SSDP 
     * vehicle and suspicious.
     * It sets up the necessary mock objects and test data, and then invokes the `doSubscribe` method with
     * the given client ID and subscription.
     * After the method call, it verifies that the `sendMsgToSink` method of the `HivemqSinkService` is not called.
     * Finally, it cleans up the test data by removing the subscription and the property related to profile 
     * check disabled topics.
     */
    @Test
    public void testOnInboundSubscribeSsdpVehicleSuspiciousDevice() {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(registry);
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        Mockito.mock(SubscriptionStatusHandler.class);

        PropertyLoader.getProperties().put(AuthConstants.PROFILE_CHECK_DISABLED_TOPICS, "ro");
        subscribeIntercept = new SimulatorOnSubscribeIntercept();
        subscribeIntercept.setHandler(handler);
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        DeviceSubscription deviceSubscription = new DeviceSubscription(clientId);
        deviceSubscription.setSsdpVehicle(true);
        deviceSubscription.setSuspicious(true);
        deviceSubscriptionCache.addSubscription(clientId, deviceSubscription);
        when(subscribeInboundInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        when(subscribeInboundInput.getConnectionInformation()).thenReturn(connectionInformation);
        when(subscribeInboundInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        when(subscribeInboundInput.getConnectionInformation().getConnectionAttributeStore().getAsString("username"))
                .thenReturn(Optional.of("admin"));
        String topic = "haa/harman/dev/haa_api/2d/ecall";
        when(subscription.getTopicFilter()).thenReturn(topic);
        subscribeIntercept.doSubscribe(clientId, subscription);
        Set<String> topicSet = new HashSet<>();
        topicSet.add(topic);
        verify(hivemqSinkService, times(0)).sendMsgToSink(Mockito.anyString(), Mockito.any(), Mockito.anyString());
        deviceSubscriptionCache.removeSubscription(clientId);
        PropertyLoader.getProperties().remove(AuthConstants.PROFILE_CHECK_DISABLED_TOPICS);
    }
}
