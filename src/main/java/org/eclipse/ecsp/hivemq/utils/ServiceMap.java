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

import lombok.Getter;
import lombok.ToString;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;

/**
 * It holds the mapping of mqtt topic (i.e. serviceId) to service name and
 * device status required or not.
 */
@Getter
@ToString
public class ServiceMap {

    /**
     * It is an mqtt topic. Eg haa/harman/dev/device123/2c/ro, in this case serviceId
     * is ro property mqtt.topic.service.mapping's value format
     * {@literal <}serviceid (mqtttopic){@literal >}, {@literal <}service name (sp){@literal >}, 
     * {@literal <}kafka topic{@literal >}, {@literal <}device status required{@literal >};
     * Eg: ecall,ecall,ecall,true;vf,vf,location,true;
     */
    private final String serviceId;
    private final String serviceName;
    private final String streamTopic;
    private final boolean deviceStatusRequired;
    private static final int TOKEN_LENGTH = 4;
    private static final int DEVICE_STATUS_REQ_POS = 3;
    private static final int STREAM_TOPIC_POS = 2;

    /**
     * This method splits topic mapping into multiple tokens, service id, service
     * name, stream topic and device status required.
     *
     * @param mappingLine - mapping from properties
     */
    public ServiceMap(String mappingLine) {
        String[] tokens = mappingLine.split(AuthConstants.DELIMITER);
        if (tokens.length != TOKEN_LENGTH) {
            throw new IllegalArgumentException("mapping is not proper: {}" + mappingLine);
        }
        serviceId = tokens[0];
        serviceName = tokens[1];
        streamTopic = tokens[STREAM_TOPIC_POS];
        deviceStatusRequired = Boolean.valueOf(tokens[DEVICE_STATUS_REQ_POS]);
    }
}
