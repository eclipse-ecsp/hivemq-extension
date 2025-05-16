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

import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.interceptor.pingreq.parameter.PingReqInboundInput;
import com.hivemq.extension.sdk.api.interceptor.pingreq.parameter.PingReqInboundOutput;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test class for PingServerRequest.
 */
public class PingServerRequestTest {

    @InjectMocks
    private PingServerRequest pingServerRequest;

    @Mock
    private ClientService blockingClientService;

    @Mock
    private ConnectionInformation connectionInformation;

    @Mock
    private PingReqInboundInput pingReqInboundInput;

    /**
     * Sets up the test environment before each test case.
     * Initializes the mock objects and the client service for the ping server request.
     */
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        pingServerRequest.initializeClientService();
    }

    static {
        File path = new File(
                PingServerRequestTest.class.getClassLoader().getResource("hivemq-plugin-base.properties").getFile());
        PropertyLoader.reload(path);
    }

    /**
     * Test case to verify the behavior of the `doPingReceived` method when the forced delete is enabled 
     * and the client is disconnected.
     */
    @Test
    public void testDoPingReceivedWhenForcedDeleteEnabledClientDisconnected() {
        String clientId = "7fd12f24-3341-40f6-8714-a54515e92d2b";
        pingServerRequest.setDisconnectOnPingIfNotConnected(true);
        pingServerRequest.setBlockingClientService(blockingClientService);
        CompletableFuture<Boolean> connectedFuture = CompletableFuture.completedFuture(Boolean.FALSE);
        Mockito.when(pingReqInboundInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        // We want to disconnect when hivemq things its disconnected hence
        // returning false
        Mockito.when(blockingClientService.isClientConnected(clientId)).thenReturn(connectedFuture);
        pingServerRequest.doPingReceived(pingReqInboundInput);
        verify(blockingClientService, times(1)).disconnectClient(clientId);

    }

    /**
     * Test case to verify the behavior of the `doPingReceived` method when the forced delete is enabled 
     * and the client is connected.
     */
    @Test
    public void testDoPingReceivedWhenForcedDeleteEnabledClientConnected() {
        String clientId = "7fd12f24-3341-40f6-8714-a54515e92d2b";
        pingServerRequest.setDisconnectOnPingIfNotConnected(true);
        pingServerRequest.setBlockingClientService(blockingClientService);
        Mockito.when(pingReqInboundInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        // We want to disconnect only when hivemq things its disconnected hence
        // returning false in previous test case we check the same with true
        CompletableFuture<Boolean> connectedFuture = CompletableFuture.completedFuture(Boolean.TRUE);
        Mockito.when(blockingClientService.isClientConnected(clientId)).thenReturn(connectedFuture);
        pingServerRequest.doPingReceived(pingReqInboundInput);
        verify(blockingClientService, times(0)).disconnectClient(clientId);

    }

    /**
     * Test case to verify the behavior of the `onInboundPingReq` method when a forced delete is enabled and 
     * a client exception is encountered.
     * 
     * <p>This test sets up the necessary conditions for the test case by configuring the `pingServerRequest` 
     * object and mocking the required dependencies.
     * It then simulates the scenario where the client is not connected and a `PingFailed` exception is thrown. 
     * The `onInboundPingReq` method is called with the mocked input and output objects.
     * 
     * <p>The test asserts that the `blockingClientService` is not called to disconnect the client and that 
     * the `isForcedClientDisconnectEnabled` flag is set to true.
     */
    @Test
    public void testDoPingReceivedWhenForcedDeleteEnabledClientExceptionEncountered() {
        String clientId = "7fd12f24-3341-40f6-8714-a54515e92d2b";
        pingServerRequest.setDisconnectOnPingIfNotConnected(true);
        pingServerRequest.setBlockingClientService(blockingClientService);
        Mockito.when(pingReqInboundInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        // We want to disconnect only when hivemq things its disconnected hence
        // returning false in previous test case we check the same with true
        CompletableFuture<Boolean> connectedFuture = CompletableFuture.failedFuture(new Exception("PingFailed"));
        Mockito.when(blockingClientService.isClientConnected(clientId)).thenReturn(connectedFuture);
        ConnectionAttributeStore connectionAttributeStore = Mockito.mock(ConnectionAttributeStore.class);
        Mockito.when(pingReqInboundInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(connectionInformation.getConnectionAttributeStore()).thenReturn(connectionAttributeStore);
        Mockito.when(connectionAttributeStore.getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of("haa_api"));
        pingServerRequest.onInboundPingReq(pingReqInboundInput, Mockito.mock(PingReqInboundOutput.class));
        verify(blockingClientService, times(0)).disconnectClient(clientId);
        Assert.assertTrue(pingServerRequest.isForcedClientDisconnectEnabled());
    }

    /**
     * Test case to verify the behavior of the `doPingReceived` method when forced delete is disabled.
     * 
     * <p>This test sets up a mock environment and configures the necessary parameters for the `pingServerRequest` 
     * object.
     * It then calls the `doPingReceived` method with the given `pingReqInboundInput`.
     * Finally, it verifies that the `disconnectClient` method of the `blockingClientService` is not called.
     */
    @Test
    public void testDoPingReceivedWhenForcedDeleteDisabled() {
        String clientId = "7fd12f24-3341-40f6-8714-a54515e92d2b";
        pingServerRequest.setDisconnectOnPingIfNotConnected(false);
        pingServerRequest.setBlockingClientService(blockingClientService);
        Mockito.when(pingReqInboundInput.getClientInformation()).thenReturn(() -> {
            return clientId;
        });
        pingServerRequest.doPingReceived(pingReqInboundInput);
        verify(blockingClientService, times(0)).disconnectClient("clientId");
    }

}
