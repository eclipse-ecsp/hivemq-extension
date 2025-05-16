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

package org.eclipse.ecsp.hivemq.cache;

import lombok.Getter;
import lombok.ToString;

/**
 * IgniteAuthInfo class contains user is authenticated and its valid till.
 *
 * @author Binoy Mandal
 */
@Getter
@ToString
public class IgniteAuthInfo {

    private Long exp;

    private boolean isAuthenticated;


    /**
     * Sets the expiration time for the authentication information.
     *
     * @param exp the expiration time in milliseconds
     * @return the updated IgniteAuthInfo object
     */
    public IgniteAuthInfo setExp(Long exp) {
        this.exp = exp;
        return this;
    }

    /**
     * Sets the authentication status for this IgniteAuthInfo object.
     *
     * @param isAuthenticated the authentication status to set
     * @return the updated IgniteAuthInfo object
     */
    public IgniteAuthInfo setAuthenticated(boolean isAuthenticated) {
        this.isAuthenticated = isAuthenticated;
        return this;
    }
}
