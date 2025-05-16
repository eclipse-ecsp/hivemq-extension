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

package org.eclipse.ecsp.analytics.stream.base.dao.impl;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.hivemq.extension.sdk.api.services.Services;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.config.internals.BrokerSecurityConfigs;
import org.eclipse.ecsp.analytics.stream.base.PropertyNames;
import org.eclipse.ecsp.analytics.stream.base.dao.SinkNode;
import org.eclipse.ecsp.analytics.stream.base.utils.ObjectUtils;
import org.eclipse.ecsp.hivemq.exceptions.ClassNotFoundRuntimeException;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * This class loads kafka producer properties and initialize producer.
 */
public class KafkaSinkNode implements SinkNode<byte[], byte[]> {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(KafkaSinkNode.class);
    private boolean replaceClassloader = false;
    private boolean isSyncPut;
    private String kafkaPartitionerClassName = null;
    private KafkaProducer<byte[], byte[]> producer = null;

    /**
     * This method loads all kafka producer properties and pass them to kafka producer initializer method..
     */
    @Override
    public void init(Properties props) {
        kafkaPartitionerClassName = props.getProperty(PropertyNames.KAFKA_PARTITIONER);
        replaceClassloader = Boolean.parseBoolean(props.getProperty(PropertyNames.KAFKA_REPLACE_CLASSLOADER));
        isSyncPut = Boolean.parseBoolean(props.getProperty(PropertyNames.KAFKA_DEVICE_EVENTS_ASYNC_PUTS));
        if (Boolean.parseBoolean(props.getProperty(PropertyNames.KAFKA_SSL_ENABLE, "true"))) {
            String keystore = props.getProperty(PropertyNames.KAFKA_CLIENT_KEYSTORE);
            ObjectUtils.requireNonEmpty(keystore, "Kafka client key store must be provided");
            String keystorePwd = props.getProperty(PropertyNames.KAFKA_CLIENT_KEYSTORE_CRED);
            ObjectUtils.requireNonEmpty(keystorePwd, "Kafka client key store password must be provided");
            String keyPwd = props.getProperty(PropertyNames.KAFKA_CLIENT_KEY_CRED);
            ObjectUtils.requireNonEmpty(keyPwd, "Kafka client key password must be provided");
            String truststore = props.getProperty(PropertyNames.KAFKA_CLIENT_TRUSTSTORE);
            ObjectUtils.requireNonEmpty(truststore, "Kafka client trust store must be provided");
            String truststorePwd = props.getProperty(PropertyNames.KAFKA_CLIENT_TRUSTSTORE_CRED);
            ObjectUtils.requireNonEmpty(truststorePwd, "Kafka client trust store password must be provided");
            String sslClientAuth = props.getProperty(PropertyNames.KAFKA_SSL_CLIENT_AUTH, "required");
            props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
            props.put(BrokerSecurityConfigs.SSL_CLIENT_AUTH_CONFIG, sslClientAuth);
            props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, keystore);
            props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, keystorePwd);
            props.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, keyPwd);
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, truststore);
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, truststorePwd);
        }
        initKafkaProducer(props);
    }

    /**
     * This method puts a message to given kafka topic with the help of common kafka producer.
     */
    @Override
    public void put(byte[] key, byte[] messageInBytes, String kafkaTopic, String primaryKeyMapping) {
        String keyString = Arrays.toString(key);

        final MetricRegistry metricRegistry = Services.metricRegistry();
        final Timer methodTimer = metricRegistry.timer(ApplicationConstants.KAFKA_PUT_TIMER_JMX);
        final Timer.Context timeMethodContext = methodTimer.time();
        metricRegistry.counter(ApplicationConstants.KAFKA_PUT_COUNTER_JMX).inc();
        LOGGER.debug("Sending message to kafka. Topic: {}, Message: {}, key: {}", kafkaTopic, keyString);
        try {
            final Timer callbackTimer = metricRegistry.timer(ApplicationConstants.KAFKA_PUT_CALLBACK_TIMER_JMX);
            final Timer.Context timeCallbackContext = callbackTimer.time();
            java.util.concurrent.Future<RecordMetadata> f =
                    producer.send(new ProducerRecord<>(kafkaTopic, key, messageInBytes),
                            (recordMetadata, exception) -> {
                            timeCallbackContext.stop();
                            if (exception != null) {
                                LOGGER.error("Exception occurred in KafkaProducerByPartition callback for key : {}",
                                        keyString,
                                        exception);
                            }
                        });

            if (isSyncPut) {
                f.get();
            }
            LOGGER.debug("Successfully sent message to kafka. Topic: {}, key: {}", kafkaTopic, keyString);
        } catch (ExecutionException | InterruptedException ee) {
            LOGGER.error("Failed when putting message to kafka for PDID : {} " + keyString, ee);
        } catch (Exception e) {
            LOGGER.error("Unable to send messages on Kafka for key : {} ", keyString, e);
        }
        timeMethodContext.stop();
    }

    /**
     * This method initialize kafka producer with given properties on application startup.
     */
    private void initKafkaProducer(Properties props) {
        LOGGER.info("Initializing Kafka Producer");
        try {
            Class.forName(kafkaPartitionerClassName);
        } catch (Exception e) {
            LOGGER.error("Failed when loading partitioner", e);
            throw new ClassNotFoundRuntimeException(e);
        }
        ClassLoader ccl = Thread.currentThread().getContextClassLoader();
        if (replaceClassloader) {
            Thread.currentThread().setContextClassLoader(null);
        }
        producer = new KafkaProducer<>(props);
        if (replaceClassloader) {
            Thread.currentThread().setContextClassLoader(ccl);
        }
    }

    /**
     * This method flushes all records to kafka, and is being called before hivemq broker stop.
     */
    public void flush() {
        LOGGER.info("Flushing Kafka Producer");
        producer.flush();
    }

    /**
     * This method closes kafka producer and is being called before stopping hivemq broker.
     */
    public void close() {
        if (producer != null) {
            LOGGER.info("Closing Kafka Producer :");
            producer.close();
            LOGGER.info("Closed Kafka Producer :");
        }
    }

}
