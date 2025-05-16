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

package org.eclipse.ecsp.hivemq.base;

import com.hivemq.extension.sdk.api.auth.Authorizer;
import com.hivemq.extension.sdk.api.auth.parameter.AuthorizerProviderInput;
import com.hivemq.extension.sdk.api.services.auth.provider.AuthorizerProvider;
import org.eclipse.ecsp.hivemq.callbacks.AbstractSubscribeInboundInterceptor;

/**
 * This class provides instance of subscribe request authorizer.
 */
public class SubscribeAuthProvider implements AuthorizerProvider {

    private AbstractSubscribeInboundInterceptor subscriptionAuthorizer;

    /**
     * The SubscribeAuthProvider class is responsible for providing authorization
     * for subscribe operations in HiveMQ.
     * It takes an instance of AbstractSubscribeInboundInterceptor as a parameter in its constructor,
     * which is used for subscription authorization.
     */
    public SubscribeAuthProvider(AbstractSubscribeInboundInterceptor subscriptionAuthorizer) {
        this.subscriptionAuthorizer = subscriptionAuthorizer;
    }

    /**
        * Returns the authorizer for handling subscription requests.
        *
        * @param authorizerProviderInput the input for the authorizer provider
        * @return the authorizer for handling subscription requests
        */
    @Override
    public Authorizer getAuthorizer(AuthorizerProviderInput authorizerProviderInput) {
        return subscriptionAuthorizer;
    }

}
