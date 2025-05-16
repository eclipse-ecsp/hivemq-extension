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
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Dummy implementation class for providing dmportal topic permissions.
 */
public class DmPortalSubsTopicPermissionDummyImpl implements ClientTopicPermissions {

    /**
        * Retrieves the topic permissions for a given client ID.
        *
        * @param clientId the ID of the client
        * @return the list of topic permissions
        */
    @Override
    public List<TopicPermission> getTopicPermissions(String clientId) {
        return Collections.unmodifiableList(Collections.emptyList());
    }

    /**
        * Initializes the object with the given properties.
        *
        * @param prop the properties to initialize the object with
        */
    @Override
    public void init(Properties prop) {
    }

}
