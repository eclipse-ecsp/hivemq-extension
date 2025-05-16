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

package org.eclipse.ecsp.hivemq.simulator;

/**
 * This class contains all constant fields name to create kafka events.
 */
public final class SimulatorEventMetadataConstants {

    private SimulatorEventMetadataConstants() {
    }
    
    public static final String UIN = "uin";
    public static final String STATE = "state";
    public static final String UPDATED = "updated";
    public static final int ONLINE = 1;
    public static final int OFFLINE = 0;
    public static final String SERVICES = "services";
}
