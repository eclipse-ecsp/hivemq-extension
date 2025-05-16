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

package org.eclipse.ecsp.analytics.stream.base.dao;

import java.util.Properties;

/**
 * The SinkNode interface represents a node that receives data and stores it in a sink.
 *
 * @param <K> the type of the key
 * @param <V> the type of the value
 */
public interface SinkNode<K, V> {

    /**
     * Initializes the SinkNode with the given properties.
     *
     * @param prop the properties to initialize the SinkNode
     */
    void init(Properties prop);

    /**
     * Flushes any pending data in the SinkNode.
     */
    void flush();

    /**
     * Closes the SinkNode and releases any resources.
     */
    void close();

    /**
     * Puts the specified key-value pair into the sink.
     *
     * @param id                the key
     * @param value             the value
     * @param tableName         the name of the table in the sink
     * @param primaryKeyMapping the mapping of primary key in the sink table
     */
    void put(K id, V value, String tableName, String primaryKeyMapping);

}
