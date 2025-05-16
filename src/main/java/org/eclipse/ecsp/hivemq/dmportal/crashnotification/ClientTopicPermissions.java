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

package org.eclipse.ecsp.hivemq.dmportal.crashnotification;

import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import java.util.List;
import java.util.Properties;

/**
 * This interface provides method to prepare dm portal permissions.
 */
public interface ClientTopicPermissions {

    /**
     * Retrieves the topic permissions for a specific client.
     *
     * @param clientId the ID of the client
     * @return a list of TopicPermission objects representing the topic permissions for the client
     */
    List<TopicPermission> getTopicPermissions(String clientId);

    /**
     * Initializes the ClientTopicPermissions object with the specified properties.
     *
     * @param prop the properties to initialize the object with
     */
    void init(Properties prop);
}
