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

package org.eclipse.ecsp.hivemq.mapper;

import org.eclipse.ecsp.hivemq.d2v.DeviceToVehicleMapper;
import org.eclipse.ecsp.hivemq.d2v.DeviceToVehicleMapperFactory;
import org.eclipse.ecsp.hivemq.d2v.DeviceToVehicleMapperVpImpl;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for DeviceToVehicleMapperFactory.
 */
public class DeviceToVehicleMapperFactoryTest {

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
     * Test case to verify the behavior of the testInstance method.
     * It checks if the DeviceToVehicleMapperFactory.getInstance() method returns an instance of 
     * DeviceToVehicleMapperVpImpl.
     */
    @Test
    public void testInstance() {
        prop.put("device.to.vehicle.mapper.impl", "org.eclipse.ecsp.hivemq.d2v.DeviceToVehicleMapperVpImpl");
        DeviceToVehicleMapper mapper = DeviceToVehicleMapperFactory.getInstance();
        Assert.assertTrue(mapper instanceof DeviceToVehicleMapperVpImpl);
    }

}
