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
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCache;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
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
 * Test class for SubscriptionCallback.
 *
 * @author Neha Khan
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Services.class, HivemqSinkService.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class SubscriptionCallbackTest {

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

    OnSubscribeIntercept subscribeIntercept;

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
     * This method tests the behavior of the `onInboundSubscribe` method.
     * It sets up the necessary mocks and verifies that the handler's `handle` method is called
     * with the correct parameters.
     */
    @Test
    public void testOnInboundSubscribe() {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(registry);
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        Mockito.mock(SubscriptionStatusHandler.class);

        subscribeIntercept = new OnSubscribeIntercept();
        subscribeIntercept.setHandler(handler);
        DeviceSubscriptionCache deviceSubscriptionCache = DeviceSubscriptionCacheFactory.getInstance();
        deviceSubscriptionCache.addSubscription(clientId, new DeviceSubscription(clientId));
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
}