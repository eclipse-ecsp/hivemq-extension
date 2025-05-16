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

import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundOutput;
import com.hivemq.extension.sdk.api.packets.unsubscribe.UnsubscribePacket;
import org.eclipse.ecsp.domain.DeviceConnStatusV1_0.ConnectionStatus;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCache;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
import org.eclipse.ecsp.hivemq.callbacks.SubscriptionStatusHandler;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for SimulatorUnsubscribeInboundIntercept.
 */
public class SimulatorUnsubscribeInboundInterceptTest {

    @Mock
    UnsubscribeInboundInput unsubscribeInboundInput;
    @Mock
    UnsubscribeInboundOutput unsubscribeInboundOutput;
    @Mock
    HivemqSinkService hivemqSinkService;
    @Mock
    SubscriptionStatusHandler handler;

    @InjectMocks
    SimulatorUnsubscribeInboundIntercept unsubscribeInboundIntercept;

    String clientId = "simulator-device";
    String mqttTopic = "haa/harman/dev/haa_api/2d/ro";
    String serviceName = "ro";

    /**
     * This setup method gets called before each test case and load required properties.
     */
    @Before
    public void setUp() {
        initMocks(this);
        List<String> topics = new ArrayList<>();
        topics.add(mqttTopic);
        UnsubscribePacket unsubscribePacket = Mockito.mock(UnsubscribePacket.class);
        when(unsubscribeInboundInput.getUnsubscribePacket()).thenReturn(unsubscribePacket);
        when(unsubscribePacket.getTopicFilters()).thenReturn(topics);
        when(unsubscribeInboundInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        byte[] payload = "data".getBytes();
        doNothing().when(hivemqSinkService).sendMsgToSink(payload, payload, "test");
    }

    /**
     * Test case to verify the behavior of the onInboundUnsubscribe method when unsubscribing a vehicle.
     */
    @Test
    public void testOnUnSubscribeVehicle() {
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        DeviceSubscription deviceSubscription = new DeviceSubscription(clientId);
        deviceSubscription.setSsdpVehicle(false);
        deviceSubscriptionCache.addSubscription(clientId, deviceSubscription);
        deviceSubscriptionCache.addTopic(clientId, clientId, serviceName);
        unsubscribeInboundIntercept.onInboundUnsubscribe(unsubscribeInboundInput, unsubscribeInboundOutput);
        Set<String> topicSet = new HashSet<>();
        topicSet.add(mqttTopic);
        verify(handler, times(1)).handle(clientId, topicSet, ConnectionStatus.INACTIVE);
        deviceSubscriptionCache.removeSubscription(clientId);
    }

    /**
     * Test case to verify the behavior of the onInboundUnsubscribe method when the device is an SSDP vehicle.
     */
    @Test
    public void testOnUnSubscribeSsdpVehicle() {
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        DeviceSubscription deviceSubscription = new DeviceSubscription(clientId);
        deviceSubscription.setSsdpVehicle(true);
        deviceSubscriptionCache.addSubscription(clientId, deviceSubscription);
        deviceSubscriptionCache.addTopic(clientId, clientId, serviceName);
        unsubscribeInboundIntercept.onInboundUnsubscribe(unsubscribeInboundInput, unsubscribeInboundOutput);
        Set<String> topicSet = new HashSet<>();
        topicSet.add("ro");
        verify(hivemqSinkService, times(1)).sendMsgToSink(Mockito.anyString(), Mockito.any(), Mockito.anyString());
        deviceSubscriptionCache.removeSubscription(clientId);
    }

}
