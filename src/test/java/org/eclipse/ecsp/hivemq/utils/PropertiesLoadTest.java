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

import org.eclipse.ecsp.analytics.stream.base.PropertyNames;
import org.junit.Assert;
import org.junit.Test;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * PropertyLoader can load the properties from a default file or from a given
 * properties file or from vault.
 */
public class PropertiesLoadTest {

    private static final String TOPIC_MAPPER_CLASS = "org.eclipse.ecsp.hivemq.routing.TopicMapperIgniteServiceBased";
    private static final String SIMULATOR_TOPIC_FORMATTER = "org.eclipse.ecsp.hivemq.simulator.SimulatorTopicFormatter";
    private static final String NEW_TOPIC_FORMATTER = "org.eclipse.ecsp.hivemq.routing.NewTopicMapper";

    /**
     * Test the load(file) method to load the properties from a given properties
     * file.
     */
    @Test
    public void testLoad() {

        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream("src/test/resources/hivemq-plugin-base.properties"));
            boolean ssdpSimulator = Boolean.parseBoolean(properties.getProperty("ssdp.simulator"));
            if (!ssdpSimulator) {
                Assert.assertEquals(TOPIC_MAPPER_CLASS, properties.getProperty("topic.mapper.impl.class"));
            } else {
                Assert.assertEquals(SIMULATOR_TOPIC_FORMATTER, properties.getProperty("topic.mapper.impl.class"));
            }
            properties.load(new FileInputStream("src/test/resources/hivemq-plugin-harman-dev.properties"));
            Assert.assertEquals(NEW_TOPIC_FORMATTER, properties.getProperty("topic.mapper.impl.class"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Test the getProperties() method to check if it is able to relay the correct
     * properties to the user.
     */
    @Test
    public void testLoadPropertyLoader() {

        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        boolean ssdpSimulator = Boolean.parseBoolean(PropertyLoader.getValue("ssdp.simulator"));
        if (!ssdpSimulator) {
            Assert.assertEquals(TOPIC_MAPPER_CLASS, PropertyLoader.getValue("topic.mapper.impl.class"));
        } else {
            Assert.assertEquals(SIMULATOR_TOPIC_FORMATTER, PropertyLoader.getValue("topic.mapper.impl.class"));
        }

        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-harman-dev.properties");
        Assert.assertEquals(NEW_TOPIC_FORMATTER, PropertyLoader.getValue("topic.mapper.impl.class"));
        Assert.assertNotNull(PropertyLoader.getProperties().getProperty(PropertyNames.REDIS_MODE));
    }

    /**
     * Test reload() method.
     */
    @Test
    public void testReLoadPropertyLoader() {
        File filePath = new File("src/test/resources/hivemq-plugin-base.properties");
        PropertyLoader.reload(filePath);
        boolean ssdpSimulator = Boolean.parseBoolean(PropertyLoader.getValue("ssdp.simulator"));
        if (!ssdpSimulator) {
            Assert.assertEquals(TOPIC_MAPPER_CLASS, PropertyLoader.getValue("topic.mapper.impl.class"));
        } else {
            Assert.assertEquals(SIMULATOR_TOPIC_FORMATTER, PropertyLoader.getValue("topic.mapper.impl.class"));
        }
    }

    /**
     * Test load() when tanentEnvConfFile does not exists. The load() method tries
     * to load the properties from BASE_FILE_NAME which is
     * hivemq-plugin-base.properties Next it tries to load properties from
     * TENANT_CONFIG_FILE (for example /opt/hivemq/conf/hivemq-plugin.properties)
     * and then from the redisPropLoader. However when these property files are not
     * available then we expect the propMap to be empty. then we expect the
     */
    @Test
    public void testGetPropertiesMapWhenEnvFilesNotExists() throws IOException {
        Map<String, List<String>> propMap = PropertyLoader.getPropertiesMap();
        Assert.assertFalse(propMap.isEmpty());
    }

    /**
     * Testing for the values from the loaded properties.
     */
    @Test
    public void testGetValueWhenDefaultValueNotUsed() throws IOException {
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        boolean ssdpSimulator = Boolean.parseBoolean(PropertyLoader.getValue("ssdp.simulator"));
        if (!ssdpSimulator) {
            Assert.assertEquals(TOPIC_MAPPER_CLASS, PropertyLoader.getValue("topic.mapper.impl.class", "defaultValue"));
        } else {
            Assert.assertEquals(SIMULATOR_TOPIC_FORMATTER,
                    PropertyLoader.getValue("topic.mapper.impl.class", "defaultValue"));
        }
    }

    /**
     * Testing for the values from the loaded properties.
     */
    @Test
    public void testGetValueWhenDefaultValueUsed() throws IOException {
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        Assert.assertEquals("defaultValue", PropertyLoader.getValue("topic.mapper.impl.classNew", "defaultValue"));

    }

    /**
     * Testing the service map.
     */
    @Test
    public void testGetSeviceMap() {
        PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        Map<String, ServiceMap> serviceMap = PropertyLoader.getSeviceMap();
        Assert.assertNotNull(serviceMap.get("alerts"));
        Assert.assertEquals("haa-harman-dev-alerts", serviceMap.get("alerts").getStreamTopic());
        Assert.assertEquals("", serviceMap.get("alerts").getServiceName());
        Assert.assertEquals(false, serviceMap.get("alerts").isDeviceStatusRequired());

        Assert.assertNotNull(serviceMap.get("ro"));
        Assert.assertEquals("haa-harman-dev-ro", serviceMap.get("ro").getStreamTopic());
        Assert.assertEquals("ro", serviceMap.get("ro").getServiceName());
        Assert.assertEquals(true, serviceMap.get("ro").isDeviceStatusRequired());
        Assert.assertTrue(serviceMap.toString().contains("alerts"));
    }

    /**
     * Test case to verify the behavior of the `mask` method in the `PropertyLoader` class.
     * It checks whether the method correctly masks the given secret string.
     */
    @Test
    public void shouldMaskStringValue() {
        // given
        String secret = "verySecret";
        // when
        String masked = PropertyLoader.mask(secret);
        // then
        Assert.assertEquals("3c8696407283ec83ae9f43c343f66366", masked);
    }

    /**
        * Test case to verify that the PropertyLoader.mask() method does not fail
        * when provided with an empty or null secret.
        */
    @Test
    public void shouldNotFailOnEmptyNorNullSecret() {
        // given
        String secret = "";
        // when
        String masked = PropertyLoader.mask(secret);
        String maskedNull = PropertyLoader.mask(null);
        // then
        Assert.assertEquals("", masked);
        Assert.assertEquals("", maskedNull);
    }
}