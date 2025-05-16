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

package org.eclipse.ecsp.hivemq.kafka;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;

import java.util.Properties;

/**
 * This class provide kafka producer configuration.
 */
public class KafkaConfigProvider {
    private KafkaConfigProvider() {
    }

    private static Properties kafkaProperties = null;

    /**
     * This method sets kafka producer configuration in properties.
     *
     * @param props - properties
     * @return properties
     */
    public static Properties setKafkaProperties(Properties props) {
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                PropertyLoader.getValue(ApplicationConstants.KAFKA_BROKER_URL));
        props.put(ApplicationConstants.KAFKA_KEY_ACKS, PropertyLoader.getValue(ApplicationConstants.KAFKA_AKS));
        props.put(ApplicationConstants.KAFKA_KEY_LINGER_MS,
                Integer.valueOf(PropertyLoader.getValue(ApplicationConstants.KAFKA_LINGER_MS)));
        props.put(ApplicationConstants.KAFKA_KEY_RETRIES,
                Integer.valueOf(PropertyLoader.getValue(ApplicationConstants.KAFKA_NUM_OF_RETRIES)));
        props.put(ApplicationConstants.KAFKA_KEY_KEY_SERIALIZER,
                PropertyLoader.getValue(ApplicationConstants.KAFKA_KEY_SERIALIZER));
        props.put(ApplicationConstants.KAFKA_KEY_VALUE_SERIALIZER,
                PropertyLoader.getValue(ApplicationConstants.KAFKA_VALUE_SERIALIZER));
        props.put(ApplicationConstants.KAFKA_KEY_REQUEST_TIME_OUT_MS,
                PropertyLoader.getValue(ApplicationConstants.KAFKA_REQUEST_TIME_OUT_MS));
        props.put(ApplicationConstants.KAFKA_KEY_COMPRESSION_TYPE,
                PropertyLoader.getValue(ApplicationConstants.KAFKA_COMPRESSION_TYPE));
        props.put(ApplicationConstants.KAFKA_KEY_BATCH_SIZE,
                PropertyLoader.getValue(ApplicationConstants.KAFKA_BATCH_SIZE));
        props.put(ApplicationConstants.KAFKA_KEY_MAX_BLOCK_MS,
                PropertyLoader.getValue(ApplicationConstants.KAFKA_MAX_BLOCK_MS));
        props.put(ApplicationConstants.KAFKA_KEY_MAX_INFLIGHT_REQUEST_PER_CONN,
                PropertyLoader.getValue(ApplicationConstants.KAFKA_MAX_INFLIGHT_REQUEST_PER_CONN));
        kafkaProperties = props;
        return props;
    }

    /**
    * Returns the Kafka initializer properties.
    *
    * @return the Kafka initializer properties
    */
    public static Properties getKafkaInitializerProperties() {
        return kafkaProperties;
    }
}
