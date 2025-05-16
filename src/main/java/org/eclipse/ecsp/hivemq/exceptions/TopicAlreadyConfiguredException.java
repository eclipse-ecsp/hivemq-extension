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
 * This custom exception is thrown when we are trying to map blob source with topic 
 * and that source/topic mapping is already present.
 */
public class TopicAlreadyConfiguredException extends RuntimeException {

    private static final long serialVersionUID = -8143116501389491585L;
    
    public TopicAlreadyConfiguredException(String message) {
        super(message);
    }

}
