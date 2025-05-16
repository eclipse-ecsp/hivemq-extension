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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.util.Properties;

/**
 * Test class for TenantBasedTopicFormatter.
 */
public class TenantBasedTopicFormatterTest {

    TenantBasedTopicFormatter tenantBasedTopicFormatter;

    Properties properties;

    /**
     * Sets up the test environment before each test case is executed.
     * This method is annotated with `@Before` to indicate that it should be executed before each test case.
     * It loads the properties from the specified file path to initialize the `properties` variable.
     *
     * @throws Exception if an error occurs during the setup process.
     */
    @Before
    public void setUp() throws Exception {
        properties = PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
    }

    /**
     * Test case to verify the behavior of the formatPublishTopic method in the TenantBasedTopicFormatter class.
     */
    @Test
    public void testFormatPublishTopic() {
        properties.put(AuthConstants.MQTT_UNAME_PREFIX, "harman/dev");
        tenantBasedTopicFormatter = new TenantBasedTopicFormatter();
        Assert.assertEquals("HUXOIDDN4HUN18/2c/events",
                tenantBasedTopicFormatter.formatPublishTopic("HUXOIDDN4HUN18", "harman/dev/haa_api", "events"));
    }

    /**
     * Test case for the formatSubscribeTopic method in the TenantBasedTopicFormatter class.
     * This method tests the formatting of the subscribe topic based on different scenarios.
     */
    @Test
    public void testFormatSubscribeTopic() {
        properties.put(AuthConstants.MQTT_UNAME_PREFIX, "harman/dev");
        // Case_1: mqttUserName.startsWith(userPrefix) = true
        tenantBasedTopicFormatter = new TenantBasedTopicFormatter();
        Assert.assertEquals("haa_api/2d/notification", tenantBasedTopicFormatter.formatSubscribeTopic(StringUtils.EMPTY,
                "harman/dev/haa_api", "notification"));

        // Case_2: mqttUserName.startsWith(userPrefix) = false
        tenantBasedTopicFormatter = new TenantBasedTopicFormatter();
        Assert.assertEquals("haa/harman/dev/haa_api/2d/notification",
                tenantBasedTopicFormatter.formatSubscribeTopic(StringUtils.EMPTY, "haa_api", "notification"));

        // Case_3: mqttUserName is Empty
        tenantBasedTopicFormatter = new TenantBasedTopicFormatter();
        Assert.assertEquals("haa/harman/dev//2d/notification",
                tenantBasedTopicFormatter.formatSubscribeTopic(StringUtils.EMPTY, StringUtils.EMPTY, "notification"));
    }

}
