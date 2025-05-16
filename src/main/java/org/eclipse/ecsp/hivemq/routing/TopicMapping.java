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

package org.eclipse.ecsp.hivemq.routing;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;

import java.util.HashMap;
import java.util.Map;

/**
 * When any publish request reaches, then Hivemq plugin needs to extract the
 * deviceid, serviceid and mapped to stream topic, service name and device
 * status required from the Mqtt topic. This class holds those information.
 */
@Getter
@ToString
@Builder
public class TopicMapping {

    private final String deviceId;
    private final String streamTopic;
    private final String serviceId;
    @Getter(AccessLevel.NONE)
    private final String streamStatusTopic;
    private final String serviceName;
    private final boolean deviceStatusRequired;
    // It represents the message flow from either device to cloud or cloud to
    // device
    private final Route route;
    private static final Map<String, String> MAP = new HashMap<>();

    static {
        MAP.put("ada/ftd", "ada-control-sp");
        MAP.put("ada/ubi", "ada-control-sp");
    }

    /**
     * Enum provides route values, to device or to cloud.
     */
    public enum Route {
        TO_CLOUD("2c/"), TO_DEVICE("2d/");

        String value;

        Route(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return this.value;
        }
    }

    /**
     * Method returns device-status kafka topic name against given service. 
     *
     * @return device-status kafka topic
     */
    public String getStreamStatusTopic() {
        if (MAP.containsKey(serviceName)) {
            return ApplicationConstants.STATUS_PREFIX + MAP.get(serviceName);
        }
        return this.streamStatusTopic == null ? ApplicationConstants.STATUS_PREFIX + serviceName
                : this.streamStatusTopic;
    }

}
