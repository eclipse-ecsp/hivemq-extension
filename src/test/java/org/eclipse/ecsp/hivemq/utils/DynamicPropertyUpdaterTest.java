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

import org.apache.commons.io.FileUtils;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertEquals;

/**
 * Test class for DynamicPropertyUpdater.
 */
public class DynamicPropertyUpdaterTest {

    private static final String NAME = "HARMAN";

    private static final String BLOB_SOURCE = "ignite,telematics1,test";

    private static final String PROPERTY_FILE_NAME = "hivemq-plugin-base.properties";

    private static final String FILE_SEPARATOR = File.separator;

    private static final File PROPERTY_FILE = new File(
            DynamicPropertyUpdaterTest.class.getClassLoader().getResource(PROPERTY_FILE_NAME).getPath());

    private static final String WATCHER_FOLDER = AuthConstants.PROPERTY_FILE_PATH;

    private static String PROPERTY_FILE_CONTENT;

    private static List<String> cache = new ArrayList<>();
    private static final int ONE_THOUSAND_MS = 1000;
    private static final int TWO_THOUSAND_MS = 2000;

    static {
        cache.add(toString(PROPERTY_FILE));
    }

    /**
     * This method will test if we modify any property that should detect and reload.
     */
    @Test
    public void testDynamicUpdatePropertySuccess()
            throws InterruptedException, IOException {
        PropertyLoader.reload(new File(WATCHER_FOLDER + FILE_SEPARATOR + PROPERTY_FILE_NAME));
        DynamicPropertyUpdater.init();
        Thread.sleep(ONE_THOUSAND_MS);
        write("\nname=HARMAN\nallowed.blob.sources=ignite,telematics1,test\n", true);
        Thread.sleep(TWO_THOUSAND_MS);
        assertEquals(NAME, PropertyLoader.getValue("name"));
        assertEquals(BLOB_SOURCE, PropertyLoader.getValue("allowed.blob.sources"));
        DynamicPropertyUpdater.shutdown();
    }

    /**
     * If path is not a valid then folderwatcher should not start and property must
     * be not reloaded.
     */
    @Test
    public void testDynamicUpdatePropertyForInvalidFolder()
            throws InterruptedException, IOException {
        PropertyLoader.reload(new File(WATCHER_FOLDER + FILE_SEPARATOR + PROPERTY_FILE_NAME));
        DynamicPropertyUpdater.init();
        write("mqtt.user.prefix=harman/dev/test\n", true);
        assertEquals("harman/dev/", PropertyLoader.getValue("mqtt.user.prefix"));
        DynamicPropertyUpdater.shutdown();
    }

    /**
     * Cleans up any resources used by the test case after each test method.
     * This method is annotated with the `@After` annotation, which ensures that it is executed after each test method.
     * It throws an `IOException` if an error occurs while deleting the test folder.
     */
    @After
    public void tearDown() throws IOException {
        deleteTestFolder();
    }

    /**
     * This setup method gets called before each test case and load required properties.
     *
     * @throws IOException - if not able to read file.
     */
    @Before
    public void setup() throws IOException {
        PROPERTY_FILE_CONTENT = cache.get(0);
        createTestFolder();
        recreateFile();
    }

    /**
     * Converts the contents of a file to a string.
     *
     * @param path the file to read
     * @return the contents of the file as a string, or an empty string if an error occurs
     */
    private static String toString(File path) {
        try {
            return path == null ? "" : FileUtils.readFileToString(path, Charset.defaultCharset());
        } catch (IOException e) {
            return "";
        }
    }

    /**
     * write content to given property file.
     *
     * @param content - content to write in file
     * @param append -  boolean if true, then data will be written to the end of 
     *     the file rather than the beginning.
     * 
     * @throws IOException - if not able to load file.
     */
    public void write(String content, boolean append) throws IOException {
        FileWriter fw = new FileWriter(WATCHER_FOLDER + FILE_SEPARATOR + PROPERTY_FILE_NAME, append);
        BufferedWriter bw = new BufferedWriter(fw);
        bw.write(content);
        bw.close();
    }

    /**
     * Creates a new file in the specified watcher folder with the given property file name.
     * If the file already exists, it will be recreated.
     * The content of the property file is written using the write method.
     *
     * @throws IOException if an I/O error occurs while creating the file.
     */
    public void recreateFile() throws IOException {
        new File(WATCHER_FOLDER, PROPERTY_FILE_NAME).createNewFile();
        write(PROPERTY_FILE_CONTENT, false);
    }

    /**
     * Creates a test folder if it doesn't already exist.
     */
    private void createTestFolder() {
        File tempFolder = new File(WATCHER_FOLDER);
        if (!tempFolder.exists()) {
            tempFolder.mkdirs();
        }
    }

    /**
     * Deletes the test folder used for testing purposes.
     */
    private void deleteTestFolder() {
        try {
            FileUtils.deleteDirectory(new File(WATCHER_FOLDER));
        } catch (IOException e) {
            System.out.println("Error in deletTestFolder");
        }
    }

}
