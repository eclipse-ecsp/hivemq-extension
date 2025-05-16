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

package redis.embedded;

import java.io.IOException;

/**
 * Supporting class for Redis version 4.0.8.
 *
 * @author ssasidharan
 */
public class RedisServer408 extends RedisServer {

    /**
     * Represents a Redis server instance for version 4.0.8.
     */
    public RedisServer408(RedisExecProvider redisExecProvider, Integer port) throws IOException {
        super(redisExecProvider, port);
    }

    /**
     * Constructs a new RedisServer408 object based on the provided RedisServer object.
     *
     * @param r the RedisServer object to create RedisServer408 from
     * @throws IOException if an I/O error occurs while creating the RedisServer408
     */
    public RedisServer408(RedisServer r) throws IOException {
        super(r.ports().get(0));
        this.args = r.args;
    }

    /**
     * Returns the regular expression pattern used to determine if the Redis server is ready to accept connections.
     *
     * @return The regular expression pattern.
     */
    @Override
    protected String redisReadyPattern() {
        return ".*[rR]eady to accept connections.*";
    }

}
