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

import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundInput;
import com.hivemq.extension.sdk.api.interceptor.unsubscribe.parameter.UnsubscribeInboundOutput;
import com.hivemq.extension.sdk.api.packets.unsubscribe.UnsubscribePacket;
import org.eclipse.ecsp.domain.DeviceConnStatusV1_0.ConnectionStatus;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCache;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
import org.eclipse.ecsp.hivemq.exceptions.InvalidSubscriptionException;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.File;
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
 * Test class for UnSubscriptionCallback.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ HivemqSinkService.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class UnSubscriptionCallbackTest {

    @Mock
    UnsubscribeInboundInput unsubscribeInboundInput;
    @Mock
    UnsubscribeInboundOutput unsubscribeInboundOutput;
    @Mock
    HivemqSinkService hivemqSinkService;
    @Mock
    SubscriptionStatusHandler handler;

    @InjectMocks
    UnsubscribeInboundIntercept unsubscribeInboundIntercept;

    String clientId = "deviceid";
    String topic = "haa/harman/dev/haa_api/2d/ro";

    static {
        File path = new File(UnSubscriptionCallbackTest.class.getClassLoader()
                .getResource("hivemq-plugin-base.properties").getFile());
        PropertyLoader.reload(path);
        PropertyLoader.loadRedisProperties("src/test/resources");
    }

    /**
     * This setup method gets called before each test case and load required properties.
     */
    @Before
    public void setUp() {
        initMocks(this);
        List<String> topics = new ArrayList<>();
        topics.add(topic);
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
     * Test case for the {@code onUnSubscribe} method.
     *
     * @throws InvalidSubscriptionException if an invalid subscription is encountered
     */
    @Test
    public void testOnUnSubscribe() throws InvalidSubscriptionException {
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        deviceSubscriptionCache.addSubscription(clientId, new DeviceSubscription(clientId));
        deviceSubscriptionCache.addTopic(clientId, clientId, topic);
        unsubscribeInboundIntercept.onInboundUnsubscribe(unsubscribeInboundInput, unsubscribeInboundOutput);
        Set<String> topicSet = new HashSet<>();
        topicSet.add(topic);
        verify(handler, times(1)).handle(clientId, topicSet, ConnectionStatus.INACTIVE);
    }
}