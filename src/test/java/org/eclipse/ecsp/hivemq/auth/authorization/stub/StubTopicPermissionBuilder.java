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
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission.MqttActivity;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission.PermissionType;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission.Qos;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission.Retain;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission.SharedSubscription;
import com.hivemq.extension.sdk.api.services.builder.TopicPermissionBuilder;

/**
 * Stub class to mock hivemq TopicPermission for juints.
 */
public class StubTopicPermissionBuilder implements TopicPermissionBuilder, Cloneable {
    private String topicFilter;
    private MqttActivity activity;
    private Qos qos;
    private PermissionType permissionType;
    private Retain retain;
    private SharedSubscription sharedSubscription;
    private String sharedGroup;
    private StubTopicPermissionBuilder stubTopicPermissionBuilder;

    public StubTopicPermissionBuilder() {
        stubTopicPermissionBuilder = this;
    }

    @Override
    public @NotNull TopicPermissionBuilder topicFilter(@NotNull String topicFilter) {
        this.topicFilter = topicFilter;
        return stubTopicPermissionBuilder;
    }

    @Override
    public @NotNull TopicPermissionBuilder activity(@NotNull MqttActivity activity) {
        this.activity = activity;
        return stubTopicPermissionBuilder;
    }

    @Override
    public @NotNull TopicPermission build() {
        return new TopicPermission() {

            @Override
            public @NotNull PermissionType getType() {
                return permissionType;
            }

            @Override
            public @NotNull String getTopicFilter() {
                return topicFilter;
            }

            @Override
            public @NotNull SharedSubscription getSharedSubscription() {
                return sharedSubscription;
            }

            @Override
            public @NotNull String getSharedGroup() {
                return sharedGroup;
            }

            @Override
            public @NotNull Qos getQos() {
                return qos;
            }

            @Override
            public @NotNull Retain getPublishRetain() {
                return retain;
            }

            @Override
            public @NotNull MqttActivity getActivity() {
                return activity;
            }
        };
    }

    @Override
    public @NotNull TopicPermissionBuilder qos(@NotNull Qos arg0) {
        this.qos = arg0;
        return stubTopicPermissionBuilder;
    }

    @Override
    public @NotNull TopicPermissionBuilder type(@NotNull PermissionType type) {
        this.permissionType = type;
        return stubTopicPermissionBuilder;
    }

    @Override
    public @NotNull TopicPermissionBuilder retain(@NotNull Retain retain) {
        this.retain = retain;
        return stubTopicPermissionBuilder;
    }

    @Override
    public @NotNull TopicPermissionBuilder sharedSubscription(@NotNull SharedSubscription sharedSubscription) {
        this.sharedSubscription = sharedSubscription;
        return stubTopicPermissionBuilder;
    }

    @Override
    public @NotNull TopicPermissionBuilder sharedGroup(@NotNull String sharedGroup) {
        this.sharedGroup = sharedGroup;
        return stubTopicPermissionBuilder;
    }

}
