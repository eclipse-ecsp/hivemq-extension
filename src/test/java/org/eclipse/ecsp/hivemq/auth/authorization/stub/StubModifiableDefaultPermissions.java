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

package org.eclipse.ecsp.hivemq.auth.authorization.stub;

import com.hivemq.extension.sdk.api.annotations.NotNull;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.packets.auth.DefaultAuthorizationBehaviour;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Stub class to mock ModifiableDefaultPermissions for junits.
 */
public class StubModifiableDefaultPermissions implements ModifiableDefaultPermissions {

    List<@NotNull TopicPermission> topicPermissions;
    DefaultAuthorizationBehaviour defaultAuthorizationBehaviour;

    public StubModifiableDefaultPermissions() {
        topicPermissions = new ArrayList<>();
    }

    @Override
    public @NotNull List<@NotNull TopicPermission> asList() {
        return topicPermissions;
    }

    @Override
    public void add(@NotNull TopicPermission permission) {
        topicPermissions.add(permission);
    }

    @Override
    public void addAll(@NotNull Collection<? extends TopicPermission> permissions) {
        topicPermissions.addAll(permissions);
    }

    @Override
    public void remove(@NotNull TopicPermission permission) {
        topicPermissions.remove(permission);

    }

    @Override
    public void clear() {
        topicPermissions.clear();
    }

    @Override
    public @NotNull DefaultAuthorizationBehaviour getDefaultBehaviour() {
        return defaultAuthorizationBehaviour;
    }

    @Override
    public void setDefaultBehaviour(@NotNull DefaultAuthorizationBehaviour defaultBehaviour) {
        defaultAuthorizationBehaviour = defaultBehaviour;
    }

}
