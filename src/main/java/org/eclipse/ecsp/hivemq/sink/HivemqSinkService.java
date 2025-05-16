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

package org.eclipse.ecsp.hivemq.sink;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.analytics.stream.base.dao.SinkNode;
import org.eclipse.ecsp.analytics.stream.base.dao.impl.KafkaSinkNode;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.kafka.KafkaConfigProvider;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * This class creates kafka produces to send hivemq messages.
 */
public class HivemqSinkService {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(HivemqSinkService.class);

    private static HivemqSinkService hivemqSinkService = null;
    private SinkNode<byte[], byte[]> sinkNode;

    static {
        initializeSinkService();
    }

    /**
     * The HivemqSinkService class represents a service for handling sink operations in HiveMQ.
     * It initializes the sink services and provides methods for initializing the sink node based 
     * on the configured implementation class.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private HivemqSinkService() {
        LOGGER.info("Initializing HiveMQ sink services");

        String className = StringUtils.EMPTY;
        try {
            className = PropertyLoader.getValue(ApplicationConstants.HIVEMQ_SINK_NODE_IMPL_CLASS);
            sinkNode = (SinkNode) HivemqSinkService.class.getClassLoader().loadClass(className)
                    .getDeclaredConstructor().newInstance();
            if (className.contains(KafkaSinkNode.class.getName())) {
                Properties props = KafkaConfigProvider.setKafkaProperties(PropertyLoader.getProperties());
                sinkNode.init(props);
            } else {
                sinkNode.init(PropertyLoader.getProperties());
            }
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException(
                    ApplicationConstants.HIVEMQ_SINK_NODE_IMPL_CLASS + " refers to a class [" + className
                            + "] that is not available on the classpath");
        }
    }

    /**
     * This method initialize HivemqSinkService class.
     */
    public static synchronized void initializeSinkService() {
        if (hivemqSinkService == null) {
            hivemqSinkService = new HivemqSinkService();
            LOGGER.info("HiveMQ sink services initialized successfully");
        }
    }

    /**
     * Returns the singleton instance of the HivemqSinkService.
     *
     * @return the singleton instance of the HivemqSinkService
     */
    public static HivemqSinkService getInstance() {
        return hivemqSinkService;
    }

    /**
     * Sends a message to the sink.
     *
     * @param pdid The unique identifier for the message.
     * @param payload The message payload as a byte array.
     * @param sinkTopic The topic to which the message should be sent.
     */
    public void sendMsgToSink(String pdid, byte[] payload, String sinkTopic) {
        sinkNode.put(pdid.getBytes(StandardCharsets.UTF_8), payload, sinkTopic, null);
    }

    /**
     * Sends a message to the sink.
     *
     * @param pdid      the PDID (Product Device ID) associated with the message
     * @param payload   the payload of the message
     * @param sinkTopic the topic to which the message should be sent
     */
    public void sendMsgToSink(byte[] pdid, byte[] payload, String sinkTopic) {
        sinkNode.put(pdid, payload, sinkTopic, null);
    }

    /**
     * Flushes the Kafka Producer.
     * This method is used to ensure that all pending records are sent to the Kafka broker.
     */
    public void flush() {
        LOGGER.info("Flushing Kafka Producer");
        sinkNode.flush();
    }

    /**
     * This method closes kafka producer.
     */
    public void close() {
        LOGGER.info("Closing Kafka Producer :");
        sinkNode.close();
        LOGGER.info("Closed Kafka Producer :");
    }
}
