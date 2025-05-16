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

package org.eclipse.ecsp.hivemq.utils;

import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.junit.Test;
import java.util.Properties;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.MQTT_TOPIC_PREFIX;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Keep Alive handler test.
 *
 * @author Binoy Mandal
 */
public class KeepAliveHandlerTest {

    private Properties props = new Properties();
    protected String mqttTopicPrefix = PropertyLoader.getValue(MQTT_TOPIC_PREFIX);
    String userName = "admin";
    String clientId = "kal";

    /**
     * Test case to verify the successful processing of keep-alive messages.
     */
    @Test
    public void testDoProcessKeepAliveMsg_Success() {
        props.put(AuthConstants.KEEP_ALIVE_TOPIC_NAME, "kal");
        KeepAliveHandler kal = new KeepAliveHandler();
        kal.setTopicFormatter(new IgniteTopicFormatter());
        IgniteTopicFormatter formatter = new IgniteTopicFormatter();
        String topic = formatter.getFormattedTopicName(clientId, mqttTopicPrefix, null, "2c/kal");
        boolean status = kal.doProcessKeepAliveMsg(props, clientId, userName, topic);
        assertTrue(status);
        props.remove(AuthConstants.KEEP_ALIVE_TOPIC_NAME);
    }

    /**
     * Test case to verify the behavior of the `doProcessKeepAliveMsg` method when the `KEEP_ALIVE_TOPIC_NAME` 
     * property is empty.
     * 
     * <p>It removes the `KEEP_ALIVE_TOPIC_NAME` property from the `props` object, creates a new instance 
     * of `KeepAliveHandler`,
     * sets the topic formatter to `IgniteTopicFormatter`, and calls the `doProcessKeepAliveMsg` 
     * method with the provided parameters.
     * 
     * <p>The expected behavior is that the method should return `false`.
     */
    @Test
    public void testDoProcessKeepAliveMsgForEmptyKalTopic() {
        props.remove(AuthConstants.KEEP_ALIVE_TOPIC_NAME);
        KeepAliveHandler kal = new KeepAliveHandler();
        kal.setTopicFormatter(new IgniteTopicFormatter());
        boolean status = kal.doProcessKeepAliveMsg(props, clientId, userName, "");
        assertFalse(status);
    }

}
