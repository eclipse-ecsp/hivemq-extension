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

import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for TopicMapperCloudNative.
 */
public class TopicMapperCloudNativeTest {

    TopicMapperCloudNative cloudNative;
    Properties prop = new Properties();

    /**
     * This setup method gets called before each test case and load required properties.
     *
     * @throws Exception throws when not able to read property file
     */
    @Before
    public void setup() throws Exception {
        initMocks(this);

        prop.put("mqtt.topic.generalevents.suffix", "/events");
        prop.put("mqtt.topic.alerts.suffix", "/alerts");
        prop.put("mqtt.topic.prefix", "haa/harman/dev/");
        prop.put("kinesis.sink.topic.events", "san-harman-dev-events");
        prop.put("kinesis.sink.topic.alerts", "san-harman-dev-alerts");
        prop.put("kinesis.sink.topic.connect", "connect");
        prop.put("kinesis.sink.topic.disconnect", "disconnect");

        cloudNative = new TopicMapperCloudNative();
        cloudNative.init(prop);
    }

    /**
        * Test case to verify the correctness of the getStreamTopic method in the TopicMapperCloudNative class.
        * It checks if the stream topic returned by the method matches the expected value for different input scenarios.
        */
    @Test
    public void testGetStreamTopic() {
        String streamTopic = cloudNative.getTopicMapping("haa/harman/dev/DEVICEID/events").getStreamTopic();
        System.out.println("Stream topic (events):" + prop.getProperty(ApplicationConstants.KINESIS_SINK_TOPIC_EVENTS));
        Assert.assertEquals("san-harman-dev-events", streamTopic);

        streamTopic = cloudNative.getTopicMapping("haa/harman/dev/DEVICEID/alerts").getStreamTopic();
        System.out.println("Stream topic (alerts):" + prop.getProperty(ApplicationConstants.KINESIS_SINK_TOPIC_ALERTS));
        Assert.assertEquals("san-harman-dev-alerts", streamTopic);
    }

    /**
        * Test case to verify the correctness of the getDeviceId method.
        */
    @Test
    public void testGetDeviceId() {
        String deviceId = "deviceId-123";
        String deviceIdFromMqttTopic = cloudNative.getTopicMapping("haa/harman/dev/" + deviceId + "/events")
                .getDeviceId();
        System.out.println("deviceIdFromMqttTopic :" + deviceIdFromMqttTopic);
        Assert.assertEquals(deviceId, deviceIdFromMqttTopic);
    }

    /**
        * Test case to verify the correctness of the getServiceId method in the TopicMapperCloudNative class.
        * It checks if the serviceId obtained from the MQTT topic mapping is equal to the expected serviceId.
        */
    @Test
    public void testGetServiceId() {
        String serviceId = "events";
        String serviceIdFromMqttTopic = cloudNative.getTopicMapping("haa/harman/dev/deviceId/" + serviceId)
                .getServiceId();
        System.out.println("serviceIdFromMqttTopic :" + serviceIdFromMqttTopic);
        Assert.assertEquals(serviceId, serviceIdFromMqttTopic);
    }

    /**
        * Test case to verify the correctness of the getConnectTopic method.
        * It asserts that the returned connect topic is equal to "connect".
        */
    @Test
    public void testGetConnectTopic() {
        Assert.assertEquals("connect", cloudNative.getConnectTopic());
    }

    /**
        * Test case to verify the behavior of the getDisconnectTopic() method.
        * It checks if the returned disconnect topic is equal to "disconnect".
        */
    @Test
    public void testGetDisconnectTopic() {
        Assert.assertEquals("disconnect", cloudNative.getDisconnectTopic());
    }

}
