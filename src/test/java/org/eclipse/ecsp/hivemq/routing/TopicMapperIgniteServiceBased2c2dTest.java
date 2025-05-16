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
public class TopicMapperIgniteServiceBased2c2dTest {

    TopicMapperIgniteServiceBased serviceBaseTopicMapper;
    Properties prop = new Properties();

    private String roKafkaTopic = "haa-harman-dev-ro";
    private String ecallKafkaTopic = "haa-harman-dev-ecall";

    /**
     * This setup method gets called before each test case and load required properties.
     *
     * @throws Exception throws when not able to read property file
     */
    @Before
    public void setup() throws Exception {
        initMocks(this);
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        prop.put("mqtt.topic.generalevents.suffix", "/events");
        prop.put("mqtt.topic.alerts.suffix", "/alerts");
        prop.put("mqtt.topic.prefix", "haa/harman/dev/");
        prop.put("mqtt.topic.infix.2d", "2d/");
        prop.put("mqtt.topic.infix.2c", "2c/");

        prop.put("kafka.sink.topic.connect", "connect");
        prop.put("kafka.sink.topic.disconnect", "disconnect");
        prop.put("mqtt.topic.ro", roKafkaTopic);
        prop.put("mqtt.topic.ecall", ecallKafkaTopic);

        serviceBaseTopicMapper = new TopicMapperIgniteServiceBased();
        serviceBaseTopicMapper.init(prop);
    }

    /**
        * Test case to verify the topic mapping for client IDs containing '2D' and '2c' characters.
        */
    @Test
    public void testClientIdContains_2D_2c_Char() {
        String streamTopic = serviceBaseTopicMapper.getTopicMapping("haa/harman/dev/DEVICEID2c/2c/ecall")
                .getStreamTopic();
        Assert.assertEquals(ecallKafkaTopic, streamTopic);

        streamTopic = serviceBaseTopicMapper.getTopicMapping("haa/harman/dev/DEVICEID2d/2c/ro").getStreamTopic();
        Assert.assertEquals(roKafkaTopic, streamTopic);

    }

    /**
        * Test case to verify the correctness of the getStreamTopic method in the serviceBaseTopicMapper class.
        * It checks if the returned stream topic matches the expected Kafka topic for different input scenarios.
        */
    @Test
    public void testGetStreamTopic() {
        String streamTopic = serviceBaseTopicMapper.getTopicMapping("haa/harman/dev/DEVICEID/2c/ecall")
                .getStreamTopic();
        Assert.assertEquals(ecallKafkaTopic, streamTopic);

        streamTopic = serviceBaseTopicMapper.getTopicMapping("haa/harman/dev/DEVICEID/2c/ro").getStreamTopic();
        Assert.assertEquals(roKafkaTopic, streamTopic);

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
        * Test case to verify the correctness of the getConnectTopic method.
        * It asserts that the returned connect topic is equal to "connect".
        */
    @Test
    public void testGetConnectTopic() {
        Assert.assertEquals("connect", serviceBaseTopicMapper.getConnectTopic());
    }

    /**
        * Test case to verify the behavior of the getDisconnectTopic method.
        * It checks if the returned disconnect topic matches the expected value.
        */
    @Test
    public void testGetDisconnectTopic() {
        Assert.assertEquals("disconnect", serviceBaseTopicMapper.getDisconnectTopic());
    }

    /**
     * Test case to verify the functionality of the getTopicMapping method.
     * It checks if the device ID extracted from the MQTT topic matches the expected device ID.
     */
    @Test
    public void testGetTcuServiceId() {
        String deviceId = "deviceId-123";
        String deviceIdFromMqttTopic = serviceBaseTopicMapper
                .getTopicMapping("haa/harman/dev/" + deviceId + "/2c/TCUShieldevents").getDeviceId();
        System.out.println("deviceIdFromMqttTopic :" + deviceIdFromMqttTopic);
        Assert.assertEquals(deviceId, deviceIdFromMqttTopic);
    }
}
