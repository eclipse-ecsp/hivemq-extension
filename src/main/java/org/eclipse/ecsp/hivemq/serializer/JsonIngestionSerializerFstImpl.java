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

package org.eclipse.ecsp.hivemq.serializer;

import org.eclipse.ecsp.entities.IgniteBlobEvent;
import org.eclipse.ecsp.serializer.IngestionSerializer;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectOutput;
import org.nustaq.serialization.util.FSTUtil;

import java.io.IOException;
import java.io.ObjectStreamConstants;

/**
 * This class Serialize/deserialize IgniteBlobEvents.
 */
public class JsonIngestionSerializerFstImpl implements IngestionSerializer {

    private static final byte[] STREAM_MAGIC_IN_BYTES;
    public static final String STREAM_MAGIC_STR = "aced"; // 0xaced get from
    public static final int STREAM_MAGIC_BYTES_LEN = 2;
    public static final int FOUR = 4;
    public static final int EIGHT = 8;
    public static final int RADIX = 16;
    public static final short SH_255 = 0xff;
    private static final FSTConfiguration CONF = FSTConfiguration.createJsonConfiguration();

    static {
        // Do not use shortpath for the common string while doing serialization.
        CONF.setShareReferences(false);
        // convert the hex aced to byte[]
        STREAM_MAGIC_IN_BYTES = hexStringToByteArray(STREAM_MAGIC_STR);
    }

    /**
     * Serializes an IgniteBlobEvent object into a byte array.
     *
     * @param obj The IgniteBlobEvent object to be serialized.
     * @return A byte array representing the serialized IgniteBlobEvent object.
     */
    @Override
    public byte[] serialize(IgniteBlobEvent obj) {
        FSTObjectOutput objectOutput = CONF.getObjectOutput();
        try {
            // Add the magic bytes in output stream.
            objectOutput.write(STREAM_MAGIC_IN_BYTES);
            objectOutput.writeObject(obj);
            return objectOutput.getCopyOfWrittenBuffer();
        } catch (IOException e) {
            FSTUtil.rethrow(e);
            return new byte[0];
        }
    }

    /**
     * Deserializes a byte array into an IgniteBlobEvent object.
     *
     * @param b The byte array to be deserialized.
     * @return The deserialized IgniteBlobEvent object.
     */
    @Override
    public IgniteBlobEvent deserialize(byte[] b) {
        try {
            // discard the first two magic bytes from input stream.
            return (IgniteBlobEvent) CONF
                    .getObjectInputCopyFrom(b, STREAM_MAGIC_BYTES_LEN, b.length - STREAM_MAGIC_BYTES_LEN).readObject();
        } catch (Exception e) {
            FSTUtil.rethrow(e);
        }
        return null;
    }

    /**
     * Converts a hexadecimal string to a byte array.
     *
     * @param s the hexadecimal string to convert
     * @return the byte array representation of the hexadecimal string
     */
    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / STREAM_MAGIC_BYTES_LEN];
        for (int i = 0; i < len; i += STREAM_MAGIC_BYTES_LEN) {
            data[i / STREAM_MAGIC_BYTES_LEN] 
                    = (byte) ((Character.digit(s.charAt(i), RADIX) << FOUR) + Character.digit(s.charAt(i + 1), RADIX));
        }

        return data;
    }

    /**
    * Checks if the given byte array is serialized.
    *
    * @param bytes the byte array to check
    * @return true if the byte array is serialized, false otherwise
    */
    @Override
    public boolean isSerialized(byte[] bytes) {
        // Convert to short from reading the first two bytes from byte array
        short sm = (short) (((bytes[0] & SH_255) << EIGHT) + (bytes[1] & SH_255));
        return sm == ObjectStreamConstants.STREAM_MAGIC;
    }
}
