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

package org.eclipse.ecsp.hivemq.exceptions;

/**
 * Custom exception thrown when client tries to subscribe on an invalid topic.
 */
public class InvalidSubscriptionException extends RuntimeException {

    private static final long serialVersionUID = 947713754914575846L;

    public InvalidSubscriptionException(String message) {
        super(message);
    }

    public InvalidSubscriptionException(String message, Exception exception) {
        super(message, exception);
    }
}
