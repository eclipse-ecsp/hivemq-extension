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

package org.eclipse.ecsp.hivemq.kafka.util;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.util.zip.ZipInputStream;

/**
 * This class identify compression type of payload and decompress it.
 */
public class CompressionJack {
    private static final int BUFFER_CAPACITY = 6144;
    private static final int MINUS_ONE = -1;
    private static final short COMPRESSION_TYPE_GZIP = 0x1f8b;
    private static final short COMPRESSION_TYPE_Z = 0x1f9d;
    private static final short COMPRESSION_TYPE_BZIP2 = 0x425a;
    private static final short COMPRESSION_TYPE_ZLIB_1 = 0x7801;
    private static final short COMPRESSION_TYPE_ZLIB_2 = 0x789c;
    private static final short COMPRESSION_TYPE_ZLIB_3 = 0x78da;
    private static final short COMPRESSION_TYPE_ZIP = 0x504b;
    
    /**
     * This method reads first two bytes of payload and identify compression type.
     *
     * @param bytes - payload
     * @return compression type
     * @throws IOException - IOException
     */
    public CompressionType sniff(byte[] bytes) throws IOException {
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(bytes))) {
            short coupleBytes = dis.readShort();
            switch (coupleBytes) {
                case COMPRESSION_TYPE_GZIP:
                    return CompressionType.GZIP;
                case COMPRESSION_TYPE_Z:
                    return CompressionType.Z;
                case COMPRESSION_TYPE_BZIP2:
                    return CompressionType.BZIP2;
                case COMPRESSION_TYPE_ZLIB_1:
                    return CompressionType.ZLIB;
                case COMPRESSION_TYPE_ZLIB_2:
                    return CompressionType.ZLIB;
                case COMPRESSION_TYPE_ZLIB_3:
                    return CompressionType.ZLIB;
                case COMPRESSION_TYPE_ZIP:
                    return CompressionType.ZIP;
                default:
                    return CompressionType.PLAIN;
            }
        }
    }

    /**
     * Decompresses the given byte array using the appropriate compression codec.
     *
     * @param bytes the byte array to decompress
     * @return the decompressed byte array
     * @throws IOException if an I/O error occurs during decompression
     */
    public byte[] decompress(byte[] bytes) throws IOException {
        CompressionType codec = sniff(bytes);
        return codec.decompress(bytes);
    }

    /**
     * ENUM for different compression types.
     */
    public enum CompressionType {

        GZIP(new GzipDecompressor()), ZLIB(new ZlibDecompressor()), ZIP(new ZipDecompressor()),
        BZIP2(new UnsupportedDecompressor()), Z(new UnsupportedDecompressor()), PLAIN(new NoOpDecompressor());

        private Decompressor inflater;

        private CompressionType(Decompressor inflater) {
            this.inflater = inflater;
        }

        public byte[] decompress(byte[] bytes) throws IOException {
            return inflater.decompress(bytes);
        }
    }

    /**
     * The Decompressor interface represents a compression algorithm used to decompress byte arrays.
     */
    private interface Decompressor {
        /**
         * Decompresses the given byte array.
         *
         * @param in the byte array to decompress
         * @return the decompressed byte array
         * @throws IOException if an I/O error occurs during decompression
         */
        public byte[] decompress(byte[] in) throws IOException;
    }

    /**
     * This is the base abstract class for decompressors.
     * It implements the Decompressor interface and provides a default implementation for the decompress method.
     * Subclasses must implement the createDecompressionInputStream method to create the decompression input stream.
     */
    private abstract static class BaseDecompressor implements Decompressor {

        /**
         * Decompresses the given byte array.
         *
         * @param in The byte array to be decompressed.
         * @return The decompressed byte array.
         * @throws IOException If an I/O error occurs during decompression.
         */
        @Override
        public byte[] decompress(byte[] in) throws IOException {
            try (InputStream gin = createDecompressionInputStream(in)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream(BUFFER_CAPACITY);
                byte[] buffer = new byte[BUFFER_CAPACITY];
                int nr = 0;
                while ((nr = gin.read(buffer, 0, BUFFER_CAPACITY)) != MINUS_ONE) {
                    out.write(buffer, 0, nr);
                }
                return out.toByteArray();
            }
        }

        /**
         * Creates a decompression input stream for the given input byte array.
         *
         * @param in the input byte array to be decompressed
         * @return an InputStream for reading the decompressed data
         * @throws IOException if an I/O error occurs during decompression
         */
        public abstract InputStream createDecompressionInputStream(byte[] in) throws IOException;

    }

    /**
     * A class that represents a Gzip decompressor.
     * This class extends the BaseDecompressor class and provides a method to create a decompression 
     * input stream for Gzip compressed data.
     */
    private static final class GzipDecompressor extends BaseDecompressor {

        /**
         * Creates a decompression input stream for the given byte array.
         *
         * @param in the byte array to be decompressed
         * @return the decompression input stream
         * @throws IOException if an I/O error occurs
         */
        @Override
        public InputStream createDecompressionInputStream(byte[] in) throws IOException {
            return new GZIPInputStream(new BufferedInputStream(new ByteArrayInputStream(in)));
        }
    }

    /**
     * A class that represents a Zlib decompressor.
     * This class extends the BaseDecompressor class.
     */
    private static final class ZlibDecompressor extends BaseDecompressor {

        /**
         * Creates a decompression input stream for the given input byte array.
         *
         * @param in The input byte array to be decompressed.
         * @return An InputStream that can be used to read the decompressed data.
         * @throws IOException If an I/O error occurs.
         */
        @Override
        public InputStream createDecompressionInputStream(byte[] in) throws IOException {
            return new InflaterInputStream(new BufferedInputStream(new ByteArrayInputStream(in)));
        }

    }

    /**
     * This class represents a Zip decompressor that extends the BaseDecompressor class.
     * It provides a method to create a decompression input stream for a given byte array.
     */
    private static final class ZipDecompressor extends BaseDecompressor {

        /**
         * Creates a decompression input stream for the given byte array.
         *
         * @param in the byte array to be decompressed
         * @return an input stream for reading the decompressed data
         * @throws IOException if an I/O error occurs
         */
        @Override
        public InputStream createDecompressionInputStream(byte[] in) throws IOException {
            ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new ByteArrayInputStream(in)));
            zis.getNextEntry();
            return zis;
        }

    }

    /**
     * A decompressor implementation that performs no operation and returns the input byte array as is.
     */
    private static final class NoOpDecompressor implements Decompressor {
        /**
         * Decompresses the given byte array.
         *
         * @param in the byte array to be decompressed
         * @return the decompressed byte array
         * @throws IOException if an I/O error occurs during decompression
         */
        @Override
        public byte[] decompress(byte[] in) throws IOException {
            return in;
        }
    }

    /**
     * A decompressor implementation that throws an exception indicating unsupported compression format.
     */
    private static final class UnsupportedDecompressor implements Decompressor {
        /**
         * Decompresses the given byte array.
         *
         * @param in the byte array to decompress
         * @return the decompressed byte array
         * @throws IOException if an I/O error occurs during decompression
         * @throws UnsupportedOperationException if the compression format is not supported
         */
        @Override
        public byte[] decompress(byte[] in) throws IOException {
            throw new UnsupportedOperationException("Unsupported compression format");
        }
    }
}
