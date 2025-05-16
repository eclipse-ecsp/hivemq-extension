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

package org.eclipse.ecsp.analytics.stream.base.metrics.reporter;

import lombok.NoArgsConstructor;
import org.eclipse.ecsp.analytics.stream.base.PropertyNames;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The `CumulativeLogger` class is responsible for logging cumulative metrics.
 * It provides methods to increment counters and reset them periodically.
 * The logged values are accumulated over a specified time interval and then reset.
 */
public final class CumulativeLogger {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(CumulativeLogger.class);
    private static final String SPACE = " ";
    private static final Map<String, AtomicLong> STATE = new ConcurrentHashMap<>();
    private static int logEveryxMinute = 5;

    @NoArgsConstructor
    private static class CumulativeLoggerHolder {
        private static final CumulativeLogger CLOGGER = new CumulativeLogger(logEveryxMinute);
    }

    /**
     * Constructs a new CumulativeLogger with the specified log count.
     *
     * @param logEveryxMinute the log count in minutes
     */
    private CumulativeLogger(int logEveryxMinute) {
        ScheduledExecutorService ses = Executors.newSingleThreadScheduledExecutor(
                runnable -> {
                    Thread t = Executors.defaultThreadFactory().newThread(runnable);
                    t.setDaemon(true);
                    t.setName("CumulativeLogger:" + Thread.currentThread().getName());
                    return t;
                });
        ses.scheduleAtFixedRate(CumulativeLogger::resetAndLog, logEveryxMinute, logEveryxMinute, TimeUnit.MINUTES);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            CumulativeLogger.resetAndLog();
            LOGGER.info("Flushed Cumulative Logger state");
        }));
        LOGGER.info("Cumulative logger initialized.");
    }

    /**
     * Initializes the CumulativeLogger with the specified properties.
     *
     * @param properties the properties to initialize the logger with
     * @throws IllegalArgumentException if the log count is less than 1
     */
    public static void init(Properties properties) {
        logEveryxMinute = Integer.parseInt(properties.getProperty(PropertyNames.LOG_COUNTS_MINUTES, "5"));
        if (logEveryxMinute < 1) {
            throw new IllegalArgumentException("Log count must be greater than 0");
        }
    }

    /**
     * Returns the instance of CumulativeLogger.
     *
     * @return the CumulativeLogger instance
     */
    public static final CumulativeLogger getLogger() {
        return CumulativeLoggerHolder.CLOGGER;
    }

    /**
     * Increments the specified counter by one.
     *
     * @param counter the counter to increment
     */
    public void incrementByOne(String counter) {
        incrementBy(counter, 1);
    }

    /**
     * Increments the specified counter by the specified count.
     *
     * @param counter the counter to increment
     * @param count   the count to increment by
     */
    public void incrementBy(String counter, int count) {
        STATE.putIfAbsent(counter, new AtomicLong(0));
        STATE.get(counter).addAndGet(count);
    }

    /**
     * Resets the counters and logs the values.
     */
    private static void resetAndLog() {
        StringBuilder str = new StringBuilder();
        STATE.forEach((k, v) -> {
            str.delete(0, str.length());
            long count = v.getAndSet(0);
            if (count > 0) {
                str.append(k).append(SPACE).append(count);
                LOGGER.info(str.toString());
            }
        });
    }
}
