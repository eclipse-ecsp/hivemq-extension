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

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.services.Services;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.base.VehicleProfileDataExtractionImpl;
import org.eclipse.ecsp.hivemq.d2v.DeviceToVehicleMapperVpImpl;
import org.eclipse.ecsp.hivemq.d2v.VehicleInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for VehicleProfileDataExtractionImpl.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({Services.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*",
    "jdk.internal.net.http.common.*"})
public class VehicleIdentityViaVpMapperTest {

    @Mock
    private VehicleProfileDataExtractionImpl vehicleProfileDataExtraction;
    public MockWebServer mockWebServer;

    Properties properties;
    private static final int PORT = 8080;

    /**
     * This setup method gets called before each test case and load required properties.
     *
     * @throws Exception Throws exception when property file not found.
     */
    @Before
    public void setUp() throws Exception {
        properties = PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");

        this.setupWebServer();
    }

    /**
     * Sets up a mock web server for testing purposes.
     *
     * @throws Exception if an error occurs while setting up the web server.
     */
    protected void setupWebServer() throws Exception {
        this.mockWebServer = new MockWebServer();
        mockWebServer.start(InetAddress.getByName("localhost"), PORT);
        String mockApiUrl = mockWebServer.url(properties.getProperty(AuthConstants.VEHICLE_PROFILE_URL))
                .toString();
        System.out.print("mock url1" + mockApiUrl);

        this.mockWebServer.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) throws InterruptedException {
                MockResponse response = null;
                try {
                    response = new MockResponse().setBody(
                            new String(Files.readAllBytes(
                                    Paths.get("src/test/resources/VehicleProfileResponseV2.txt"))));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return response;
            }
        });

    }

    /**
     * Test case for the `getVehicleId` method.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testGetVehicleId() throws Exception {
        final MetricRegistry metricRegistry = mock(MetricRegistry.class);
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(metricRegistry);
        final Timer timer = mock(Timer.class);
        when(metricRegistry.timer(anyString())).thenReturn(timer);
        final Timer.Context context = mock(Timer.Context.class);
        when(timer.time()).thenReturn(context);
        when(context.stop()).thenReturn(1L);
        when(metricRegistry.counter(anyString())).thenReturn(mock(Counter.class));
        try {
            MockitoAnnotations.initMocks(this);
            DeviceToVehicleMapperVpImpl identityViaVpMapper = new DeviceToVehicleMapperVpImpl();
            identityViaVpMapper.init(properties);
            VehicleProfileApiClient.setVehicleProfileDataExtraction(vehicleProfileDataExtraction);
            CompletableFuture<VehicleInfo> vinfo = identityViaVpMapper
                    .getVehicleId("device123", mock(ConnectionAttributeStore.class));
            Assert.assertNotNull(vinfo);
            final VehicleInfo vehicleInfo = vinfo.get();
            Assert.assertNotNull(vehicleInfo);
            Assert.assertEquals("5A8HR44H08R828625", vehicleInfo.getVehicleId());
        } finally {
            mockWebServer.close();
        }
    }

    /**
     * Test case to verify the behavior of the getVehicleId method when the URL is incorrect.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testGetVehicleIdWhenUrlIncorrect() throws IOException {
        final MetricRegistry metricRegistry = mock(MetricRegistry.class);
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(metricRegistry);
        final Timer timer = mock(Timer.class);
        when(metricRegistry.timer(anyString())).thenReturn(timer);
        final Timer.Context context = mock(Timer.Context.class);
        when(timer.time()).thenReturn(context);
        when(context.stop()).thenReturn(1L);
        when(metricRegistry.counter(anyString())).thenReturn(mock(Counter.class));
        try {
            DeviceToVehicleMapperVpImpl identityViaVpMapper = new DeviceToVehicleMapperVpImpl();
            properties.put(AuthConstants.VEHICLE_PROFILE_URL, "http://localhost:8082/v1.0/vehicle");
            identityViaVpMapper.init(properties);

            VehicleInfo vehicleInfo = null;
            try {
                vehicleInfo = identityViaVpMapper.getVehicleId("device123", mock(ConnectionAttributeStore.class)).get();
            } catch (Exception e) {
                System.out.println("Error while fetching vehicle data " + e);
            }
            Assert.assertNull(vehicleInfo);
        } finally {
            mockWebServer.close();
        }
    }

}