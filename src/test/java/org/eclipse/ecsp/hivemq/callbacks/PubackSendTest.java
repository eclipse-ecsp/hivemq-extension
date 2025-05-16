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
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackOutboundInput;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackOutboundOutput;
import com.hivemq.extension.sdk.api.packets.puback.PubackPacket;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test class for PubackSend.
 */
public class PubackSendTest {

    private PubackSend pubackSend;
    private PubackOutboundInput pubackOutboundInput;
    private PubackOutboundOutput pubackOutboundOutput;
    private PubackPacket pubackPacket;
    private ClientInformation clientInformation;
    private static final int TEN = 10;

    /**
     * This setup method mocks all required hivemq classes.
     */
    @Before
    public void setUp() {
        pubackSend = new PubackSend();
        pubackOutboundInput = Mockito.mock(PubackOutboundInput.class);
        pubackOutboundOutput = Mockito.mock(PubackOutboundOutput.class);
        pubackPacket = Mockito.mock(PubackPacket.class);
        clientInformation = Mockito.mock(ClientInformation.class);

        Mockito.when(pubackOutboundInput.getClientInformation()).thenReturn(clientInformation);
        Mockito.when(pubackOutboundInput.getPubackPacket()).thenReturn(pubackPacket);
    }

    /**
     * Test case for the `onOutboundPuback` method.
     * Verifies the behavior of the method when an outbound PUBACK packet is received.
     *
     * @throws InterruptedException if the test is interrupted
     */
    @Test
    public void testOnOutboundPuback() throws InterruptedException {
        Mockito.when(clientInformation.getClientId()).thenReturn("testClient");
        Mockito.when(pubackPacket.getPacketIdentifier()).thenReturn(TEN);
        pubackSend.onOutboundPuback(pubackOutboundInput, pubackOutboundOutput);
    }
}
