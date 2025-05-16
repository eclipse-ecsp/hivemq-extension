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

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

/**
 * Utility class.
 *
 * @author Binoy mandal
 */
public class CommonUtil {
    private CommonUtil() {
    }

    /**
     * This method Extracts node value from second level.
     *
     * @param node - json node
     * @param firstField - first level node value
     * @param secondField - second level field value
     * @return json value
     */
    public static String getField(JsonNode node, String firstField, String secondField) {
        JsonNode fst = getFieldNode(node, firstField);
        JsonNode res = getFieldNode(fst, secondField);
        return Objects.nonNull(res) ? res.asText() : null;
    }

    /**
     * Retrieves the value of a field from a JSON node.
     *
     * @param node      the JSON node to retrieve the field from
     * @param fieldName the name of the field to retrieve
     * @return the value of the field as a string, or null if the field does not exist
     */
    public static String getField(JsonNode node, String fieldName) {
        JsonNode f = getFieldNode(node, fieldName);
        return (Objects.nonNull(f)) ? f.asText() : null;
    }

    /**
     * Retrieves the value of a specific field from a JSON node.
     *
     * @param node      The JSON node to search in.
     * @param fieldName The name of the field to retrieve.
     * @return The value of the specified field, or null if the field is not found or the input node is null.
     */
    public static JsonNode getFieldNode(JsonNode node, String fieldName) {
        JsonNode fst = Objects.nonNull(node) ? node.findValue(fieldName) : null;
        return (Objects.nonNull(fst)) ? fst : null;
    }

}
