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

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableOutboundPublish;
import org.eclipse.ecsp.hivemq.transform.IngestionSerializerFactory;
import org.eclipse.ecsp.hivemq.transform.IngestionSerializerStub;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import java.nio.ByteBuffer;
import java.util.Optional;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

/**
 * Test class for BrokerBeforePublishSendCallback.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ IngestionSerializerFactory.class })
@SuppressStaticInitializationFor("org.eclipse.ecsp.hivemq.transform.IngestionSerializerFactory")
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class BrokerBeforePublishSendCallbackTest {

    private static final String PROPERTY_FILE 
        = "src/test/resources/broker-before-publish-send-callback-test.properties";

    @Mock
    private MetricRegistry registry;
    @Mock
    private Histogram histogram;

    @Mock
    private PublishOutboundInput publishOutboundInput;

    @Mock
    private PublishOutboundOutput publishOutboundOutput;

    @Mock
    private ModifiableOutboundPublish publishPacket;
    @Mock
    private ClientInformation clientInformation;

    /**
     * Sets up the test environment before running the test cases.
     * This method is annotated with @BeforeClass, which means it will be executed once before any 
     * test case in the class.
     * It loads the properties from the specified property file.
     */
    @BeforeClass
    public static void setUp() {
        PropertyLoader.getProperties(PROPERTY_FILE);
    }

    /**
     * This setup method gets called before each test case and load required properties.
     */
    @Before
    public void init() {
        PowerMockito.mockStatic(IngestionSerializerFactory.class);
        when(IngestionSerializerFactory.getInstance()).thenReturn(new IngestionSerializerStub());
        MockitoAnnotations.initMocks(this);
        when(registry.histogram("outbound_latency_ms")).thenReturn(histogram);
    }

    /**
     * Test case to verify that the metric is ignored when disabled in the configuration.
     */
    @Test
    public void shouldIgnoreMetricWhenDisabledInConfig() {
        // having
        PropertyLoader.getProperties().put("outbound.latency.metric.enabled", "false");
        OnPublishOutboundIntercept callback = new OnPublishOutboundIntercept(registry);

        when(publishOutboundOutput.getPublishPacket()).thenReturn(publishPacket);
        when(publishOutboundInput.getClientInformation()).thenReturn(clientInformation);
        // when
        callback.onOutboundPublish(publishOutboundInput, publishOutboundOutput);

        // then
        Mockito.verify(histogram, never()).update(Mockito.anyLong());
    }

    /**
     * Test case to verify that the metric is updated when enabled in the configuration.
     */
    @Test
    public void shouldUpdateMetricWhenEnabledInConfig() {
        // having
        PropertyLoader.getProperties().put("outbound.latency.metric.enabled", "true");
        byte[] payload = new byte[1];
        payload[0] = 1; // 1 - IngestionSerializerStab.class returns true for isSerialized()

        when(publishOutboundOutput.getPublishPacket()).thenReturn(publishPacket);
        when(publishOutboundInput.getClientInformation()).thenReturn(clientInformation);
        when(publishOutboundOutput.getPublishPacket().getPayload()).thenReturn(Optional.of(ByteBuffer.wrap(payload)));

        OnPublishOutboundIntercept callback = new OnPublishOutboundIntercept(registry);
        // when
        callback.onOutboundPublish(publishOutboundInput, publishOutboundOutput);

        // then
        Mockito.verify(histogram, times(1)).update(Mockito.anyLong());
    }

    /**
     * Test case to verify that the callback should ignore a faulty payload.
     */
    @Test
    public void shouldIgnoreFaultyPayload() {
        // having
        PropertyLoader.getProperties().put("outbound.latency.metric.enabled", "true");
        when(publishOutboundOutput.getPublishPacket()).thenReturn(publishPacket);
        when(publishOutboundInput.getClientInformation()).thenReturn(clientInformation);
        when(clientInformation.getClientId()).thenReturn("HUX111567890");

        when(publishOutboundOutput.getPublishPacket().getPayload()).thenReturn(Optional.empty());
        OnPublishOutboundIntercept callback = new OnPublishOutboundIntercept(registry);
        // when
        callback.onOutboundPublish(publishOutboundInput, publishOutboundOutput);

        // then
        Mockito.verify(histogram, never()).update(Mockito.anyLong());
    }
}