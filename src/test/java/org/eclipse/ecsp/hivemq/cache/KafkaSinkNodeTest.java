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

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.hivemq.extension.sdk.api.services.Services;
import org.apache.kafka.common.KafkaException;
import org.eclipse.ecsp.analytics.stream.base.PropertyNames;
import org.eclipse.ecsp.analytics.stream.base.dao.impl.KafkaSinkNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.util.Properties;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for KafkaSinkNode.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Services.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*"})
public class KafkaSinkNodeTest {

    private static final int BUFFER_MEMORY = 33554432;
    private static final int BATCH_SIZE = 16384;
    
    /**
     * This method tests the initialization of the Kafka sink node.
     * It sets up the necessary properties for the Kafka producer and initializes the KafkaSinkNode.
     * Then, it puts a message into the Kafka sink node and flushes the messages.
     * Finally, it closes the Kafka sink node.
     * If any exception occurs during the process, the test fails.
     */
    @Test
    public void testInitKafka() {
        final MetricRegistry metricRegistry = mock(MetricRegistry.class);
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(metricRegistry);
        final Timer timer = mock(Timer.class);
        when(metricRegistry.timer(anyString())).thenReturn(timer);
        final Timer.Context context = mock(Timer.Context.class);
        when(timer.time()).thenReturn(context);
        when(context.stop()).thenReturn(1L);
        when(metricRegistry.counter(anyString())).thenReturn(mock(Counter.class));
        boolean isException = false;
        try {
            Properties properties = new Properties();
            properties.setProperty(PropertyNames.KAFKA_PARTITIONER,
                    "org.apache.kafka.clients.producer.internals.DefaultPartitioner");
            properties.setProperty(PropertyNames.KAFKA_REPLACE_CLASSLOADER, "true");
            properties.setProperty(PropertyNames.KAFKA_DEVICE_EVENTS_ASYNC_PUTS, "false");
            properties.setProperty(PropertyNames.KAFKA_SSL_ENABLE, "false");
            properties.setProperty(PropertyNames.KAFKA_CLIENT_KEYSTORE, "/kafka/ssl/kafka.client.keystore.jks");

            properties.setProperty(PropertyNames.KAFKA_CLIENT_KEYSTORE_CRED, "shcuwNHARcNuag8SgYdsG8cWuPExY3Tx");
            properties.setProperty(PropertyNames.KAFKA_CLIENT_KEY_CRED, "pUBPHXM9mP5PrRBrTEpF5cV2TpjvWtb5");
            properties.setProperty(PropertyNames.KAFKA_CLIENT_TRUSTSTORE, "/kafka/ssl/kafka.client.truststore.jks");
            properties.setProperty(PropertyNames.KAFKA_CLIENT_TRUSTSTORE_CRED, "9vq9ghbSFd7JMFSgGMSCEuAzE3q27Xd3");
            properties.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
            properties.put("bootstrap.servers", "localhost:9092");
            properties.put("acks", "all");
            properties.put("retries", 0);
            properties.put("batch.size", BATCH_SIZE);
            properties.put("linger.ms", 1);
            properties.put("buffer.memory", BUFFER_MEMORY);
            KafkaSinkNode kafkaSinkNode = new KafkaSinkNode();
            kafkaSinkNode.init(properties);
            kafkaSinkNode.put("abc".getBytes(), "hi message".getBytes(), "notification", "mapping");
            kafkaSinkNode.flush();
            kafkaSinkNode.close();
        } catch (Exception e) {
            isException = true;
        }
        Assert.assertFalse(isException);
    }

    /**
     * Test case to verify the initialization of Kafka with SSL enabled.
     * This method sets up the necessary properties for Kafka configuration,
     * including SSL settings, key and truststore paths, and serializers.
     * It then initializes a KafkaSinkNode object with the properties and
     * calls the init() method to perform the initialization.
     * The test expects a KafkaException to be thrown.
     */
    @Test(expected = KafkaException.class)
    public void testInitKafka_ssl() {
        Properties properties = new Properties();
        properties.setProperty(PropertyNames.KAFKA_PARTITIONER,
                "org.apache.kafka.clients.producer.internals.DefaultPartitioner");
        properties.setProperty(PropertyNames.KAFKA_REPLACE_CLASSLOADER, "true");
        properties.setProperty(PropertyNames.KAFKA_DEVICE_EVENTS_ASYNC_PUTS, "false");
        properties.setProperty(PropertyNames.KAFKA_SSL_ENABLE, "true");
        properties.setProperty(PropertyNames.KAFKA_CLIENT_KEYSTORE, "/kafka/ssl/kafka.client.keystore.jks");

        properties.setProperty(PropertyNames.KAFKA_CLIENT_KEYSTORE_CRED, "shcuwNHARcNuag8SgYdsG8cWuPExY3Tx");
        properties.setProperty(PropertyNames.KAFKA_CLIENT_KEY_CRED, "pUBPHXM9mP5PrRBrTEpF5cV2TpjvWtb5");
        properties.setProperty(PropertyNames.KAFKA_CLIENT_TRUSTSTORE, "/kafka/ssl/kafka.client.truststore.jks");
        properties.setProperty(PropertyNames.KAFKA_CLIENT_TRUSTSTORE_CRED, "9vq9ghbSFd7JMFSgGMSCEuAzE3q27Xd3");
        properties.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("acks", "all");
        properties.put("retries", 0);
        properties.put("batch.size", BATCH_SIZE);
        properties.put("linger.ms", 1);
        properties.put("buffer.memory", BUFFER_MEMORY);
        KafkaSinkNode kafkaSinkNode = new KafkaSinkNode();
        kafkaSinkNode.init(properties);
    }

}
