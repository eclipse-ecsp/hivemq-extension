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

import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.serializer.IngestionSerializer;
import org.eclipse.ecsp.serializer.IngestionSerializerFstImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.util.Properties;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for IngestionSerializerFactory.
 */
public class IngestionSerializerFactoryTest {

    Properties prop = new Properties();

    /**
     * Sets up the test environment before each test case.
     *
     * @throws Exception if an error occurs during setup.
     */
    @Before
    public void setup() throws Exception {
        initMocks(this);
        prop = PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
    }

    /**
     * Test case for the getInstance method of IngestionSerializerFactory.
     * It verifies that the getInstance method returns an instance of IngestionSerializerFSTImpl
     * when the "ingestion.serializer.impl" property is set to 
     * "org.eclipse.ecsp.serializer.IngestionSerializerFSTImpl".
     * It also ensures that the property is removed after the test is executed.
     */
    @Test
    public void testGetInstance() {
        prop.put("ingestion.serializer.impl", "org.eclipse.ecsp.serializer.IngestionSerializerFstImpl");
        IngestionSerializer mapper = IngestionSerializerFactory.getInstance();
        Assert.assertTrue(mapper instanceof IngestionSerializerFstImpl);
        prop.remove("ingestion.serializer.impl");
    }
}
