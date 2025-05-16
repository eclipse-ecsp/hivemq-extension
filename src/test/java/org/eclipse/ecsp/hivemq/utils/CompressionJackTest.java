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

package org.eclipse.ecsp.hivemq.utils;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.eclipse.ecsp.hivemq.kafka.util.CompressionJack;
import org.junit.Assert;
import org.junit.Test;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.DeflaterInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Test class for CompressionJack.
 */
public class CompressionJackTest {
    
    private static final int BYTES_LENGTH = 10;

    /**
     * Test case for GZIP input stream.
     * This method tests the functionality of GZIP input stream by compressing and decompressing data.
     * It verifies that the compressed data is of type GZIP and the decompressed data matches the original data.
     *
     * @throws IOException if an I/O error occurs during the test
     */
    @Test
    public void testGzipInputStream() throws IOException {
        String data = "test";
        ByteArrayOutputStream compressedData = new ByteArrayOutputStream();
        GZIPOutputStream gis = new GZIPOutputStream(compressedData);
        gis.write(data.getBytes());
        gis.flush();
        gis.close();

        CompressionJack compressionJack = new CompressionJack();
        CompressionJack.CompressionType compressionType = compressionJack.sniff(compressedData.toByteArray());
        Assert.assertEquals(CompressionJack.CompressionType.GZIP, compressionType);
        byte[] decompressedData = compressionJack.decompress(compressedData.toByteArray());
        Assert.assertEquals(data, new String(decompressedData));
    }

    /**
     * Test case for the `testZipInputStream` method.
     * This method tests the functionality of decompressing data using ZipInputStream.
     * It verifies that the compressed data can be correctly decompressed and matches the original data.
     *
     * @throws IOException if an I/O error occurs during the test
     */
    @Test
    public void testZipInputStream() throws IOException {
        String data = "test";
        ByteArrayOutputStream compressedData = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(compressedData);
        ZipOutputStream gis = new ZipOutputStream(bos);
        gis.putNextEntry(new ZipEntry("textBinaryData"));
        gis.write(data.getBytes());
        gis.flush();
        gis.closeEntry();
        gis.close();

        CompressionJack compressionJack = new CompressionJack();
        CompressionJack.CompressionType compressionType = compressionJack.sniff(compressedData.toByteArray());
        Assert.assertEquals(CompressionJack.CompressionType.ZIP, compressionType);
        byte[] decompressedData = compressionJack.decompress(compressedData.toByteArray());
        Assert.assertEquals(data, new String(decompressedData));
    }

    /**
     * Test case for the BZip2InputStream functionality.
     * This method tests the behavior of the BZip2InputStream by compressing a string of binary data,
     * and then checks if the compression type is correctly identified as BZIP2.
     *
     * @throws IOException if an I/O error occurs during the test.
     */
    @Test
    public void testBzip2InputStream() throws IOException {
        final String data = "textBinaryData";
        ByteArrayOutputStream compressedData = new ByteArrayOutputStream();
        BufferedOutputStream bos = new BufferedOutputStream(compressedData);
        BZip2CompressorOutputStream bzip = new BZip2CompressorOutputStream(bos);
        bzip.write(data.getBytes());
        bzip.flush();
        bzip.close();

        CompressionJack compressionJack = new CompressionJack();
        CompressionJack.CompressionType compressionType = compressionJack.sniff(compressedData.toByteArray());
        Assert.assertEquals(CompressionJack.CompressionType.BZIP2, compressionType);
    }

    /**
     * Test case for the Zlib decompressor.
     *
     * <p>This method tests the functionality of the Zlib decompressor by compressing and decompressing 
     * a given data string.
     * It verifies that the decompressed data matches the original data.
     *
     * @throws IOException if an I/O error occurs during compression or decompression.
     */
    @Test
    public void testZlibDecompressor() throws IOException {

        String data = "test";
        ByteArrayInputStream bin = new ByteArrayInputStream(data.getBytes());
        DeflaterInputStream din = new DeflaterInputStream(bin);
        byte[] c = new byte[BYTES_LENGTH];
        din.read(c);
        din.close();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InflaterOutputStream ios = new InflaterOutputStream(bos);
        ios.write(c);
        ios.flush();
        ios.close();

        CompressionJack compressionJack = new CompressionJack();
        CompressionJack.CompressionType compressionType = compressionJack.sniff(bos.toByteArray());
        Assert.assertEquals(CompressionJack.CompressionType.PLAIN, compressionType);

        byte[] decompressedData = compressionJack.decompress(bos.toByteArray());
        Assert.assertEquals(data, new String(decompressedData));

    }
}
