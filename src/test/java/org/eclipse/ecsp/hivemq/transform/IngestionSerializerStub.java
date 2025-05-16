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

package org.eclipse.ecsp.hivemq.transform;

import org.eclipse.ecsp.entities.IgniteBlobEvent;
import org.eclipse.ecsp.serializer.IngestionSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stub class to mock IngestionSerializer for junits.
 */
public class IngestionSerializerStub implements IngestionSerializer {
    private static Logger LOG = LoggerFactory.getLogger(IngestionSerializerStub.class);

    /**
        * Serializes the given IgniteBlobEvent into a byte array.
        *
        * @param igniteBlobEvent the IgniteBlobEvent to be serialized
        * @return a byte array representing the serialized IgniteBlobEvent
        */
    @Override
    public byte[] serialize(IgniteBlobEvent igniteBlobEvent) {
        return new byte[0];
    }

    /**
        * Deserializes the given byte array into an IgniteBlobEvent object.
        *
        * @param bytes the byte array to be deserialized
        * @return the deserialized IgniteBlobEvent object
        */
    @Override
    public IgniteBlobEvent deserialize(byte[] bytes) {
        return new IgniteBlobEvent();
    }

    /**
     * Checks if the given byte array is serialized.
     *
     * @param bytes the byte array to be checked
     * @return true if the byte array is serialized, false otherwise
     */
    @Override
    public boolean isSerialized(byte[] bytes) {
        LOG.info("*** Using IngestionSerializerStub class for payload verification. ***");
        return bytes[0] == 1;
    }
}
