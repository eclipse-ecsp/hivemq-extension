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
import org.eclipse.ecsp.ignite.serializer.JsonIngestionSerializerFstImpl;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.util.Properties;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for JsonIngestionSerializerFSTImpl.
 */
public class JsonIngestionSerializerFstTest {

    Properties prop = new Properties();
    org.eclipse.ecsp.serializer.IngestionSerializer mapper;

    /**
     * Sets up the test environment before each test case.
     * Initializes the necessary mocks and creates an instance of the JsonIngestionSerializerFstImpl class.
     *
     * @throws Exception if an error occurs during setup.
     */
    @Before
    public void setUp() throws Exception {
        initMocks(this);
        mapper = new JsonIngestionSerializerFstImpl();
    }

    /**
        * Test case to verify the serialization of an IgniteBlobEvent using the JsonIngestionSerializerFst.
        * It asserts that the mapper does not consider the serialized object as already serialized.
        */
    @Test
    public void testSerialize() {
        Assert.assertFalse(mapper.isSerialized(mapper.serialize(createIgniteBlobEvent())));

    }

    /**
     * Test case to verify the failure of deserialization.
     * It expects a RuntimeException to be thrown.
     */
    @Test(expected = RuntimeException.class)
    public void testDeSerializeFail() {
        mapper.deserialize(mapper.serialize(createIgniteBlobEvent()));
    }

    /**
     * Creates an instance of IgniteBlobEvent.
     *
     * @return The created IgniteBlobEvent instance.
     */
    private IgniteBlobEvent createIgniteBlobEvent() {
        IgniteBlobEvent blobEvent = new IgniteBlobEvent();
        blobEvent.setEventId("eventId");
        return blobEvent;
    }
}
