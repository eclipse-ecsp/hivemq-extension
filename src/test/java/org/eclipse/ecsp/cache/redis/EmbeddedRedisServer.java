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

package org.eclipse.ecsp.cache.redis;

import org.junit.rules.ExternalResource;
import redis.embedded.RedisExecProvider;
import redis.embedded.RedisServer408;
import redis.embedded.util.Architecture;
import redis.embedded.util.OS;

/**
 * This class prepares redis for test cases.
 */
public class EmbeddedRedisServer extends ExternalResource {
    private RedisServer408 redis = null;
    private int port = 0;
    private static final int AVAILABLE_PORT = 6379;

    /**
     * Sets up the necessary configurations and starts the embedded Redis server before running the test.
     *
     * @throws Throwable if an error occurs during the setup process.
     */
    @Override
    protected void before() throws Throwable {
        RedisExecProvider igniteProvider = RedisExecProvider.defaultProvider();
        igniteProvider.override(OS.MAC_OS_X, Architecture.x86,
                "redis-server-4.0.8.app");
        igniteProvider.override(OS.MAC_OS_X, Architecture.x86_64,
                "redis-server-4.0.8.app");
        igniteProvider.override(OS.UNIX, Architecture.x86,
                "redis-server-4.0.8");
        igniteProvider.override(OS.UNIX, Architecture.x86_64,
                "redis-server-4.0.8");
        igniteProvider.override(OS.WINDOWS, Architecture.x86, "redis-server.exe");
        igniteProvider.override(OS.WINDOWS, Architecture.x86_64, "redis-server.exe");
        port = new PortScanner().getAvailablePort(AVAILABLE_PORT);
        redis = new RedisServer408(igniteProvider, port);
        redis.start();
        RedisConfig.overridingPort = port;
    }

    /**
     * This method is called after each test case execution.
     * It stops the embedded Redis server.
     */
    @Override
    protected void after() {
        redis.stop();
    }

    /**
     * Returns the port number on which the embedded Redis server is running.
     *
     * @return the port number of the embedded Redis server
     */
    public int getPort() {
        return port;
    }

}
