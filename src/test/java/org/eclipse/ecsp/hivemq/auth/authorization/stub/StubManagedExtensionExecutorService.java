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
import com.hivemq.extension.sdk.api.services.CompletableScheduledFuture;
import com.hivemq.extension.sdk.api.services.ManagedExtensionExecutorService;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Stub class to mock hivemq ManagedExtensionExecutorService for junits.
 */
public class StubManagedExtensionExecutorService implements ManagedExtensionExecutorService {

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    @Override
    public void execute(Runnable command) {

    }

    @Override
    public @NotNull CompletableFuture<?> submit(@NotNull Runnable task) {
        return new CompletableFuture<>();
    }

    @Override
    public <T> @NotNull CompletableFuture<T> submit(@NotNull Callable<T> task) {
        return new CompletableFuture<>();
    }

    @Override
    public <T> @NotNull CompletableFuture<T> submit(@NotNull Runnable task, @NotNull T result) {
        return new CompletableFuture<>();
    }

    @Override
    public @NotNull CompletableScheduledFuture<?> schedule(@NotNull Runnable command, long delay,
            @NotNull TimeUnit unit) {
        return null;
    }

    @Override
    public <V> @NotNull CompletableScheduledFuture<V> schedule(@NotNull Callable<V> callable, long delay,
            @NotNull TimeUnit unit) {
        return null;
    }

    @Override
    public @NotNull CompletableScheduledFuture<?> scheduleAtFixedRate(@NotNull Runnable command, long initialDelay,
            long period, @NotNull TimeUnit unit) {
        return null;
    }

    @Override
    public @NotNull CompletableScheduledFuture<?> scheduleWithFixedDelay(@NotNull Runnable command, long initialDelay,
            long delay, @NotNull TimeUnit unit) {
        return null;
    }

}
