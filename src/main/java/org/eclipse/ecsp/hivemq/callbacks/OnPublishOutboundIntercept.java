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
import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.interceptor.publish.PublishOutboundInterceptor;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishOutboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableOutboundPublish;
import org.eclipse.ecsp.entities.IgniteBlobEvent;
import org.eclipse.ecsp.hivemq.transform.IngestionSerializerFactory;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.serializer.IngestionSerializer;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.stereotype.Component;
import javax.inject.Inject;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * This class intercept all outgoing messages from hivemq to client.
 */
@Component
public class OnPublishOutboundIntercept implements PublishOutboundInterceptor {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(OnPublishOutboundIntercept.class);

    private boolean outboundLatencyMetricEnabled;
    private IngestionSerializer transformer;
    private final Histogram histogramOutboundLatency;

    /**
     * This constructor get required serialize class instance and creates a metric for latency.
     *
     * @param metricRegistry - metric registry to register a jmx metrics
     */
    @Inject
    public OnPublishOutboundIntercept(final MetricRegistry metricRegistry) {
        transformer = IngestionSerializerFactory.getInstance();
        outboundLatencyMetricEnabled = Boolean.parseBoolean(PropertyLoader.getValue("outbound.latency.metric.enabled"));
        histogramOutboundLatency = metricRegistry.histogram("outbound_latency_ms");
    }

    /**
     * This method is called when a publish message is being sent from the HiveMQ broker to a client.
     * It intercepts the outbound publish message and performs additional processing if necessary.
     *
     * @param publishOutboundInput  The input parameters for the outbound publish message.
     * @param publishOutboundOutput The output parameters for the outbound publish message.
     */
    @Override
    public void onOutboundPublish(@NotNull PublishOutboundInput publishOutboundInput,
            @NotNull PublishOutboundOutput publishOutboundOutput) {
        ModifiableOutboundPublish publishPacket = publishOutboundOutput.getPublishPacket();
        String clientId = publishOutboundInput.getClientInformation().getClientId();
        Optional<ByteBuffer> publishPayloadBuffer = publishPacket.getPayload();
        
        if (outboundLatencyMetricEnabled && publishPayloadBuffer.isPresent()) {
            byte[] mqttPayload = StandardCharsets.ISO_8859_1.decode(publishPayloadBuffer.get()).toString()
                    .getBytes(StandardCharsets.ISO_8859_1);
            if (mqttPayload.length > 0 && transformer.isSerialized(mqttPayload)) {
                LOGGER.debug("Event received is wrapped as IgniteBlobEvent for clientId {}", clientId);
                IgniteBlobEvent blobEvent = transformer.deserialize(mqttPayload);
                long diff = System.currentTimeMillis() - blobEvent.getTimestamp();
                histogramOutboundLatency.update(diff);
                LOGGER.debug("Event for clientId: {}, processed in: {}ms", clientId, diff);
            }
        }

        LOGGER.debug("Hivemq broker is publishing to topic: {} with qos: {} for clientId: {} for PacketId: {}",
                publishPacket.getTopic(), publishPacket.getQos(), clientId, publishPacket.getPacketId());

    }
}
