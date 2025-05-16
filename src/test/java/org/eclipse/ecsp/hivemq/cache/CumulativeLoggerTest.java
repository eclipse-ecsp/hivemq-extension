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

package org.eclipse.ecsp.hivemq.cache;

import org.eclipse.ecsp.analytics.stream.base.PropertyNames;
import org.eclipse.ecsp.analytics.stream.base.metrics.reporter.CumulativeLogger;
import org.junit.Assert;
import org.junit.Test;
import java.util.Properties;

/**
 * Test class for CumulativeLogger.
 */
public class CumulativeLoggerTest {
    private static final int SLEEP_MS = 100;

    /**
        * Test case to verify that an IllegalArgumentException is thrown when initializing CumulativeLogger
        * with an invalid value for LOG_COUNTS_MINUTES property.
        */
    @Test(expected = IllegalArgumentException.class)
    public void testInit_ExceptionCase() {
        Properties properties = new Properties();
        properties.setProperty(PropertyNames.LOG_COUNTS_MINUTES, "0");
        CumulativeLogger.init(properties);
    }

    /**
     * Test case for the {@link CumulativeLogger#getLogger()} method.
     * 
     * <p>This test verifies that the {@link CumulativeLogger#getLogger()} method returns a non-null logger instance.
     * It also tests the functionality of the logger by incrementing a count and waiting for a specified duration.
     */
    @Test
    public void testGetLogger() throws InterruptedException {
        Properties properties = new Properties();
        properties.setProperty(PropertyNames.LOG_COUNTS_MINUTES, "5");
        CumulativeLogger.init(properties);
        CumulativeLogger cl = CumulativeLogger.getLogger();
        Thread.sleep(SLEEP_MS);
        Assert.assertNotNull(cl);
        cl.incrementByOne("1");
        Thread.sleep(SLEEP_MS);

    }

}
