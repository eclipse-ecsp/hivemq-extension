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

package org.eclipse.ecsp.hivemq.base;

import org.eclipse.ecsp.hivemq.exceptions.IllegalTopicArgumentException;
import org.eclipse.ecsp.hivemq.routing.TopicMapping;

import java.util.Properties;

/**
 * Implementation of this interface contains the MQTT topic mapping with Sink
 * Stream topic (i.e. kafka topic, kinesis topic)
 */
public interface TopicMapper {

    void init(Properties prop);

    /**
     * It returns the TopicMapping which has deviceId, stream topic name etc.
     *
     * @param mqttTopic - mqtt topic
     * @return - topic mapping
     */
    TopicMapping getTopicMapping(String mqttTopic) throws IllegalTopicArgumentException;

    /*
     * Set kafka topic in which onconnect callback, plugin send the device
     * connnect info. So that device messaging get to know whether device is now
     * connected.
     */
    String getConnectTopic();

    /*
     * Set kafka topic in which ondisconnect callback, plugin send the device
     * disconnect info. So that device messaging get to know whether device is
     * now disconnected.
     */
    String getDisconnectTopic();
}
