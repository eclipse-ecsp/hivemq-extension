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

package org.eclipse.ecsp.hivemq.callbacks;

import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.eclipse.ecsp.hivemq.utils.HivemqUtils;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.File;
import java.util.concurrent.CompletableFuture;

/**
 * Test class for HiveStopCallback.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ HivemqUtils.class, Services.class, HivemqSinkService.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class HiveStopCallbackTest {

    private static final int WAIT_BEFORE_BROKER_STOP_MS = 5000;
    private static final int WAIT_COUNT_BEFORE_BROKER_STOP = 20;
    private static final int ONE_THOUSAND_MS = 1000;
    private static final int TWO = 2;
    
    static {
        File path = new File(
                HiveStopCallbackTest.class.getClassLoader().getResource("hivemq-plugin-base.properties").getFile());
        PropertyLoader.reload(path);
    }

    /**
     * Test case for the {@link HiveStopCallback#onBrokerStop()} method.
     * 
     * <p>This test verifies the behavior of the `onBrokerStop` method in different scenarios.
     * 
     * <p>Scenarios:
     * 1. Case_1: Client Connected is false
     *    - Mocks the behavior of `isClientsConnected` method to return false.
     *    - Calls the `onBrokerStop` method.
     *    - Verifies that the `onBrokerStop` method is called once.
     * 
     * <p>2. Case_2: Client Connected is true
     *    - Mocks the behavior of `isClientsConnected` method to return true.
     *    - Mocks the behavior of `HivemqUtils.getPropertyIntValue` method to return specific values.
     *    - Calls the `onBrokerStop` method.
     * 
     * <p>Note: This test assumes the availability of the necessary dependencies and mocks.
     */
    @Test
    public void testOnBrokerStop() {
        PowerMockito.mockStatic(HivemqUtils.class);
        PowerMockito.when(HivemqUtils.getPropertyIntValue("waitTimeMsBeforeBrokerStop"))
            .thenReturn(WAIT_BEFORE_BROKER_STOP_MS);
        PowerMockito.when(HivemqUtils.getPropertyIntValue("waitCountBeforeBrokerStop"))
            .thenReturn(WAIT_COUNT_BEFORE_BROKER_STOP);

        // Case_1: Client Connected is false
        HiveStopCallback callback = Mockito.spy(new HiveStopCallback());
        Mockito.doReturn(false).when(callback).isClientsConnected();
        callback.onBrokerStop();
        Mockito.verify(callback, Mockito.times(1)).onBrokerStop();

        // Case_2: Client Connected is true
        PowerMockito.when(HivemqUtils.getPropertyIntValue("waitTimeMsBeforeBrokerStop")).thenReturn(ONE_THOUSAND_MS);
        PowerMockito.when(HivemqUtils.getPropertyIntValue("waitCountBeforeBrokerStop")).thenReturn(TWO);

        Mockito.doReturn(true).when(callback).isClientsConnected();
        callback.onBrokerStop();
    }

    /**
     * Test case for the {@link HiveStopCallback#testIsConnected()} method.
     * This method tests the behavior of the {@link HiveStopCallback#testIsConnected()} method
     * when the broker is stopped.
     */
    @Test
    public void testIsConnected() {
        PowerMockito.mockStatic(HivemqUtils.class);
        PowerMockito.when(HivemqUtils.getPropertyIntValue("waitTimeMsBeforeBrokerStop"))
            .thenReturn(WAIT_BEFORE_BROKER_STOP_MS);
        PowerMockito.when(HivemqUtils.getPropertyIntValue("waitCountBeforeBrokerStop"))
            .thenReturn(WAIT_COUNT_BEFORE_BROKER_STOP);
        PowerMockito.mockStatic(Services.class);
        ClientService clientService = Mockito.mock(ClientService.class);
        PowerMockito.when(Services.clientService()).thenReturn(clientService);
        PowerMockito.when(clientService.iterateAllClients(Mockito.any()))
                .thenReturn(Mockito.mock(CompletableFuture.class));
        HiveStopCallback callback = new HiveStopCallback();
        callback.onBrokerStop();

    }

}
