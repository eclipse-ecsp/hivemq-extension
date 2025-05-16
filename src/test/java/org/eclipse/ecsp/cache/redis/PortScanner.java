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

package org.eclipse.ecsp.cache.redis;

import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;

import java.io.IOException;
import java.net.Socket;

/**
 * This class checks for available ports for junits.
 */
public class PortScanner {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(PortScanner.class);

    /**
     * This method checks if a given port is available, if not then increment that port by 1 and return.
     *
     * @param port - port that need to be checked
     * @return available port
     */
    public int getAvailablePort(int port) {
        while (true) {
            try (Socket s = new Socket("localhost", port)) {
                LOGGER.debug("Port {} is not available", port);
            } catch (IOException e) {
                return port;
            }
            port = port + 1;
        }
    }
}
