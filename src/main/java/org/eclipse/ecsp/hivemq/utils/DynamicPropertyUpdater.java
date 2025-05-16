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

import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;


/**
 * This is a folder watcher class which will scan a specified folder when a
 * particular event is met. Supported events are CREATE,DELETE and UPDATE.
 * Example to use this class . String folderToWatch = "D:\\"; '
 * DynamicPropertyUpdater.watchFolder(folderToWatch, ".txt", true, new
 * FileAttributes[] { FileAttributes.UPDATE }, s -> { System.out.println(s); });
 *
 * @author BinoyMandal
 */
public class DynamicPropertyUpdater {

    private DynamicPropertyUpdater() {
    }
    
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(DynamicPropertyUpdater.class);

    private static final String FOLDER_TO_WATCH = PropertyLoader.getFolderPath();

    private static ExecutorService threadPool;

    private static final String FILTER_PREFIX = "hivemq";

    private static final String FILTER_SUFFIX = ".properties";

    private static volatile boolean initialized = false;

    /**
     * initialize folder watcher.
     */
    public static synchronized void init() {
        if (initialized) {
            LOGGER.info("FolderWatcher is already initialied");
            return;
        }
        LOGGER.info("FolderWatcher is initializing...");
        if (!new File(FOLDER_TO_WATCH).isDirectory()) {
            LOGGER.error("Provided path must be a folder. but looks like a file.");
            return;
        }
        threadPool = getExecutorService();
        Objects.requireNonNull(threadPool, "Executor service is not started. so exited!!!}");
        LOGGER.info("Executor service is started {}", threadPool);
        threadPool.execute(() -> {
            try {
                Path path = Paths.get(FOLDER_TO_WATCH);
                WatchService watchService = FileSystems.getDefault().newWatchService();
                path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
                LOGGER.info("Folder watcher is registered with folder {}", FOLDER_TO_WATCH);
                while (true) {
                    WatchKey key = null;
                    try {
                        key = watchService.take();
                    } catch (InterruptedException ie) {
                        LOGGER.error("DynamicPropertyUpdater interuppted, will be retried on update...", ie);
                    }
                    if (key != null) {
                        watchEvents(key);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("DynamicPropertyUpdater is not initialized", e);
            }
        });

        initialized = true;

    }

    /**
     * Returns the executor service for the DynamicPropertyUpdater class.
     * If the executor service is not initialized, a new single-thread executor is created and returned.
     *
     * @return the executor service for the DynamicPropertyUpdater class
     */
    private static synchronized ExecutorService getExecutorService() {
        return threadPool == null ? Executors.newSingleThreadExecutor() : threadPool;
    }

    /**
     * This method shutdown folder watcher thread tasks.
     */
    public static void shutdown() {
        if (threadPool != null && (!threadPool.isShutdown() || !threadPool.isTerminated())) {
            threadPool.shutdownNow();
        }
        threadPool = null;
    }

    /**
     * This method watch for all events happening in folder and reload property file if it
     * matches given criteria.
     *
     * @param key - update event
     */
    public static void watchEvents(WatchKey key) {
        LOGGER.info("FolderWatcer got bunch of event {}.", key);
        for (WatchEvent<?> event : key.pollEvents()) {
            String fileName = event.context().toString();
            LOGGER.info("FolderWatcer got a modified event for {}", fileName);
            File updatedFile = new File(FOLDER_TO_WATCH + File.separator + fileName);

            if (Pattern.compile(FILTER_SUFFIX).matcher(fileName).find() && updatedFile.exists() && updatedFile.isFile()
                    && fileName.startsWith(FILTER_PREFIX)) {
                LOGGER.info("File {} has been modifed. Going to reload the file", updatedFile.getPath());
                PropertyLoader.reload(updatedFile);
            } else {
                LOGGER.info("File {} has been modifed. But going to ignore this event.", updatedFile.getPath());
            }
        }
        key.reset();
    }

}