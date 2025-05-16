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

package org.eclipse.ecsp.hivemq.transform;

import org.eclipse.ecsp.domain.AbstractBlobEventData.Encoding;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;

/**
 * When any publish request comes, then Hivemq Plugin needs to know the source
 * OEM and its payload encoding of this mqtt message. This class holds the
 * information about mapping of Mqtt topic (only serviceid i.e.
 * /haa/harman/deviceid/serviceid in mqtt topic) to source i.e. ignite, telematics
 * and encoding i.e. gpb,json.
 */
public class MqttTopicBlobSourceTypeMapper {
    private MqttTopicBlobSourceTypeMapper() {
    }

    /**
     * Retrieves the encoding associated with the given MQTT topic.
     *
     * @param mqttTopic the MQTT topic for which to retrieve the encoding
     * @return the encoding associated with the MQTT topic, or null if no encoding is found
     */
    public static Encoding getEncoding(String mqttTopic) {
        return PropertyLoader.getMqttTopicToEncodingMap().get(mqttTopic);
    }

    /**
     * Retrieves the event source associated with the given MQTT topic.
     *
     * @param mqttTopic the MQTT topic for which to retrieve the event source
     * @return the event source associated with the MQTT topic, or null if no mapping exists
     */
    public static String getEventSource(String mqttTopic) {
        return PropertyLoader.getMqttTopicToEventSourceMap().get(mqttTopic);
    }

}
