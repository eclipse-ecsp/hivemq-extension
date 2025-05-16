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

package org.eclipse.ecsp.hivemq.routing;

import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for TopicMapperIgniteServiceBased.
 */
public class TopicMapperIgniteServiceBasedTest {

    TopicMapperIgniteServiceBased serviceBaseTopicMapper;
    Properties prop = new Properties();

    private String roKafkaTopic = "haa-harman-dev-ro";
    private String ecallKafkaTopic = "haa-harman-dev-ecall";
    private static final String NEWTEST_MQTT_TOPIC = "haa/harman/dev/DEVICEID/2c/newtest/appid1";
    private static final String NEWTEST_KAFKA_TOPIC = "newtest";

    /**
     * This setup method gets called before each test case and load required properties.
     *
     * @throws Exception - if not able to read property file.
     */
    @Before
    public void setup() throws Exception {
        initMocks(this);
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        prop.put("mqtt.topic.generalevents.suffix", "/events");
        prop.put("mqtt.topic.alerts.suffix", "/alerts");
        prop.put("mqtt.topic.prefix", "haa/harman/dev/");

        prop.put("kafka.sink.topic.connect", "connect");
        prop.put("kafka.sink.topic.disconnect", "disconnect");
        serviceBaseTopicMapper = new TopicMapperIgniteServiceBased();
        serviceBaseTopicMapper.init(prop);
    }

    /**
        * Test case to verify the correctness of the getStreamTopic method in the TopicMapperIgniteService class.
        * It checks if the returned stream topic matches the expected Kafka topic for different input mappings.
        */
    @Test
    public void testGetStreamTopic() {
        String streamTopic = serviceBaseTopicMapper.getTopicMapping("haa/harman/dev/DEVICEID/ecall").getStreamTopic();
        Assert.assertEquals(ecallKafkaTopic, streamTopic);

        streamTopic = serviceBaseTopicMapper.getTopicMapping("haa/harman/dev/DEVICEID/ro").getStreamTopic();
        Assert.assertEquals(roKafkaTopic, streamTopic);

    }

    /**
     * Test case to verify the behavior of the getTopicMapping method.
     * It checks if the stream topic returned by the serviceBaseTopicMapper is equal to the expected Kafka topic.
     */
    @Test
    public void testGetStreamTopicWithAppId() {
        String streamTopic = serviceBaseTopicMapper.getTopicMapping(NEWTEST_MQTT_TOPIC).getStreamTopic();
        Assert.assertEquals(NEWTEST_KAFKA_TOPIC, streamTopic);
    }

    /**
        * Test case to verify the correctness of the getDeviceId method.
        */
    @Test
    public void testGetDeviceId() {
        String deviceId = "deviceId-123";
        String deviceIdFromMqttTopic = serviceBaseTopicMapper.getTopicMapping("haa/harman/dev/" + deviceId + "/events")
                .getDeviceId();
        System.out.println("deviceIdFromMqttTopic :" + deviceIdFromMqttTopic);
        Assert.assertEquals(deviceId, deviceIdFromMqttTopic);
    }

    /**
        * Test case for the getConnectTopic method.
        * 
        * <p>This test verifies that the getConnectTopic method of the serviceBaseTopicMapper
        * returns the expected value "connect".
        */
    @Test
    public void testGetConnectTopic() {
        Assert.assertEquals("connect", serviceBaseTopicMapper.getConnectTopic());
    }

    /**
        * Test case to verify the behavior of the getDisconnectTopic method.
        * It asserts that the returned disconnect topic is equal to "disconnect".
        */
    @Test
    public void testGetDisconnectTopic() {
        Assert.assertEquals("disconnect", serviceBaseTopicMapper.getDisconnectTopic());
    }

}
