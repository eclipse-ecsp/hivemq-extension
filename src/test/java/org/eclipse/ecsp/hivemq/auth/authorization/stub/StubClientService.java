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

package org.eclipse.ecsp.hivemq.auth.authorization.stub;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.annotations.Nullable;
import com.hivemq.extension.sdk.api.packets.disconnect.DisconnectReasonCode;
import com.hivemq.extension.sdk.api.services.general.IterationCallback;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import com.hivemq.extension.sdk.api.services.session.SessionInformation;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Stub class to mocke hivemq ClientService for junits.
 */
public class StubClientService implements ClientService {

    @Override
    public @NotNull CompletableFuture<Boolean> isClientConnected(@NotNull String clientId) {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Optional<SessionInformation>> getSession(@NotNull String clientId) {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> disconnectClient(@NotNull String clientId) {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> disconnectClient(@NotNull String clientId, boolean preventWillMessage) {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> disconnectClient(@NotNull String clientId, boolean preventWillMessage,
            @Nullable DisconnectReasonCode reasonCode, @Nullable String reasonString) {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Boolean> invalidateSession(@NotNull String clientId) {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllClients(@NotNull IterationCallback<SessionInformation> callback) {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<Void> iterateAllClients(@NotNull IterationCallback<SessionInformation> callback,
            @NotNull Executor callbackExecutor) {
        return null;
    }

}
