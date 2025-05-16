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

/**
 * This class contains all constant fields name to create kafka events.
 */
public class EventMetadataConstants {
    private EventMetadataConstants() {
    }
    
    public static final String PDID = "PDID";

    public static final String EVENT_ID = "EventID";
    public static final String COMMCHECK_RESPONSE_ACK_EVENT = "CommCheckResponseAckEvent";
    public static final String CONNECTION_STATUS_EVENT_ID = "ConnectionStatus";
    public static final String EVENT_UPLOAD_TIMESTAMP = "uploadTimeStamp";
    public static final String EVENT_TIMESTAMP = "Timestamp";
    public static final String EVENT_TIMEZONE = "Timezone";
    public static final String EVENT_PAYLOAD = "data";
    public static final String EVENT_DATA = "Data";
    public static final String EVENT_PDID = "PDID";
    public static final String EVENT_VALUE = "value";
    public static final String EVENT_VERSION = "Version";
    public static final String EVENT_VERSION_VALUE = "1.0";
    public static final String UTC_TIMEZONE_DIFF = "0";
    public static final String ONLINE = "Online";
    public static final String OFFLINE = "Offline";
    public static final String OFFLINE_ABRUPT = "OfflineAbrupt";
}
