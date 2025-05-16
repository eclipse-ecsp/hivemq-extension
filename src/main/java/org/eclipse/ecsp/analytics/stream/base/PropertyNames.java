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

package org.eclipse.ecsp.analytics.stream.base;

/**
 * This class contains all db and kafka constant names.
 */
public class PropertyNames {
    private PropertyNames() {
    }

    // DB Connection constant --> start (FROM CFMS)
    public static final String MONGODB_URL = "db.url";
    public static final String MONGODB_PORT = "db.port";
    public static final String MONGODB_AUTH_USERNAME = "db.auth.username";
    public static final String MONGODB_AUTH_PSWD = "db.auth.password";
    public static final String MONGODB_AUTH_DB = "db.auth.db";
    public static final String MONGODB_DBNAME = "db.name";
    public static final String MONGO_CLIENT_MAX_WAIT_TIME_MS = "db.client.max.wait.time.ms";
    public static final String MONGO_CLIENT_CONNECTION_TIMEOUT_MS = "db.client.connection.timeout.ms";
    public static final String MONGO_CLIENT_SOCKET_TIMEOUT_MS = "db.client.socket,timeout.ms";
    public static final String MONGO_MAX_CONNECTIONS = "db.client.max.connections";
    // DB Connection constant --> end
    public static final String KAFKA_PARTITIONER = "kafka.partitioner";
    public static final String KAFKA_REPLACE_CLASSLOADER = "kafka.replace.classloader";
    public static final String KAFKA_DEVICE_EVENTS_ASYNC_PUTS = "kafka.device.events.sync.puts";
    public static final String REDIS_MODE = "redis.mode";
    public static final String KAFKA_SSL_ENABLE = "kafka.ssl.enable";
    public static final String KAFKA_CLIENT_KEYSTORE = "kafka.client.keystore";
    public static final String KAFKA_CLIENT_KEYSTORE_CRED = "kafka.client.keystore.password";
    public static final String KAFKA_CLIENT_KEY_CRED = "kafka.client.key.password";
    public static final String KAFKA_CLIENT_TRUSTSTORE = "kafka.client.truststore";
    public static final String KAFKA_CLIENT_TRUSTSTORE_CRED = "kafka.client.truststore.password";
    public static final String KAFKA_SSL_CLIENT_AUTH = "kafka.ssl.client.auth";
    public static final String LOG_COUNTS_MINUTES = "log.counts.minutes";
    public static final String LOG_PER_PDID = "log.per.pdid";
}
