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

import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundInput;
import com.hivemq.extension.sdk.api.interceptor.connect.parameter.ConnectInboundOutput;
import com.hivemq.extension.sdk.api.packets.publish.ModifiableConnectPacket;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import java.util.Properties;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for ConnectInterceptor.
 */
public class ConnectInterceptorTest {

    @Mock
    private ConnectInboundInput connectInboundInput;

    @Mock
    private ConnectInboundOutput connectInboundOutput;

    @Mock
    private ClientInformation clientInformation;

    Properties prop = new Properties();

    /**
     * Sets up the test environment before each test case is executed.
     *
     * @throws Exception if an error occurs during setup.
     */
    @Before
    public void setUp() throws Exception {
        initMocks(this);
        prop = PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
    }

    /**
     * Test case to verify the behavior of the onConnect method when the clean session is enabled.
     *
     * <p>This test sets the clean session property to "true" in the prop object, mocks the connect packet,
     * and verifies that the onConnect method sets the clean start flag in the connect packet.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testOnConnectCleanSession() {
        prop.put(AuthConstants.CLEAN_SESSION, "true");
        ModifiableConnectPacket connectPacket = Mockito.mock(ModifiableConnectPacket.class);
        Mockito.when(connectInboundOutput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(connectInboundInput.getClientInformation()).thenReturn(clientInformation);
        Mockito.when(clientInformation.getClientId()).thenReturn("haa_api");
        ConnectInterceptor connectInterceptor = new ConnectInterceptor();
        connectInterceptor.onConnect(connectInboundInput, connectInboundOutput);
        prop.remove(AuthConstants.CLEAN_SESSION);
    }

    /**
     * Test case to verify the behavior of the onConnect method when the clean session is set to false.
     */
    @Test
    public void testOnConnectUnCleanSession() {
        prop.put(AuthConstants.CLEAN_SESSION, "false");
        ModifiableConnectPacket connectPacket = Mockito.mock(ModifiableConnectPacket.class);
        Mockito.when(connectInboundOutput.getConnectPacket()).thenReturn(connectPacket);
        Mockito.when(connectInboundInput.getClientInformation()).thenReturn(clientInformation);
        Mockito.when(clientInformation.getClientId()).thenReturn("haa_api");
        ConnectInterceptor connectInterceptor = new ConnectInterceptor();
        connectInterceptor.onConnect(connectInboundInput, connectInboundOutput);
        prop.remove(AuthConstants.CLEAN_SESSION);
    }

}
