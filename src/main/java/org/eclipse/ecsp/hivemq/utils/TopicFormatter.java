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

import org.eclipse.ecsp.hivemq.routing.TopicMapping;

/**
 * Base class for TopicFormatter. Declare methods for publish and subscribe topic format.
 */
public interface TopicFormatter {

    String MQTT_TOPIC_INFIX2C = TopicMapping.Route.TO_CLOUD.toString();
    String MQTT_TOPIC_INFIX2D = TopicMapping.Route.TO_DEVICE.toString();

    /**
     * For formatting publish topics.
     *
     * @param clientId - client id
     * @param userName - username
     * @param topic - topic
     * @return - publish topic
     */
    public String formatPublishTopic(String clientId, String userName, String topic);

    /**
     * For formatting subscribe topics.
     *
     * @param clientId - client id
     * @param userName - username
     * @param topic - topic
     * @return - subscribe topic
     */
    public String formatSubscribeTopic(String clientId, String userName, String topic);

    /**
     * For user topic.
     *
     * @param userId - user id
     * @param topic - topic
     * @return - user topic
     */
    public String formatUserTopic(String userId, String topic);
}
