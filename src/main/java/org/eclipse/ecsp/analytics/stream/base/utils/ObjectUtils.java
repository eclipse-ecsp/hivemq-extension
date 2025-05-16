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

package org.eclipse.ecsp.analytics.stream.base.utils;

import org.eclipse.ecsp.hivemq.exceptions.RequiredCriteriaNotMatchException;
import java.util.Collection;
import java.util.Objects;

/**
 * This is a utility class.
 */
public class ObjectUtils {
    private ObjectUtils() {
    }
    
    private static final String METHOD_REQUIRE_NON_NEGATIVE_SUPPPORTS_ONLY_INTEGERS_OR_STRINGS 
        = "Method requireNonNegative suppports only integers or strings.";

    /**
     * This method checks if given object is not null or empty.
     *
     * @param <T> - type of object
     * @param obj - object
     * @param errorMsg - error message string
     * @return - object if object is not null or empty
     */
    public static <T> T requireNonEmpty(T obj, String errorMsg) {
        Objects.requireNonNull(obj, errorMsg);
        if (obj instanceof String str && str.isEmpty()) {
            throw new RequiredCriteriaNotMatchException(errorMsg);
        }
        return obj;
    }

    /**
     * This method checks if given collection has required size or not.
     *
     * @param <T> - type of collection
     * @param t - collection to be checked
     * @param expectedSize - expected size
     * @param errorMsg - error message, if throwing error
     * @return - true if collection size is same as expected
     */
    public static <T> boolean requireSizeOf(Collection<T> t, int expectedSize, String errorMsg) {
        if (t.size() != expectedSize) {
            throw new RequiredCriteriaNotMatchException(errorMsg);
        }
        return true;
    }

    /**
     * This method checks if given object is null or not.
     *
     * @param <T> - the type of the reference
     * @param obj - the object reference to check for nullity
     * @param errorMsg - detail message to be used in the event that a {@code
     *                NullPointerException} is thrown
     * @return {@code obj} if not {@code null}
     */
    public static <T> T requireNonNull(T obj, String errorMsg) {
        return Objects.requireNonNull(obj, errorMsg);
    }

    /**
     * This method checks if given collection has required minimum size or not.
     *
     * @param <T> - type of collection
     * @param t - collection to be checked
     * @param expectedSize - expected minimum size
     * @param errorMsg - error message, if throwing error
     * @return - true if collection size is same as expected
     */
    public static <T> boolean requireMinSize(Collection<T> t, int expectedSize, String errorMsg) {
        if (t.size() < expectedSize) {
            throw new RequiredCriteriaNotMatchException(errorMsg);
        }
        return true;
    }

    /**
     * This method checks if given collection is not null or empty.
     *
     * @param <T> - type of collection
     * @param t - collection to be checked
     * @param errorMsg - error message, if throwing error
     * @return - true if collection is not empty
     */
    public static <T> boolean requiresNotNullAndNotEmpy(Collection<T> t, String errorMsg) {
        Objects.requireNonNull(t, errorMsg);
        if (t.isEmpty()) {
            throw new RequiredCriteriaNotMatchException(errorMsg);
        }
        return true;
    }

    /**
     * to check if a integer number is negative or not.
     *
     * @param obj - integer object
     * @param errorMessage - error message if object is an negative integer
     * @return object
     */
    public static <T> T requireNonNegative(T obj, String errorMessage) {
        Objects.requireNonNull(obj, errorMessage);

        Integer num;
        if (obj instanceof Integer value) {
            num = value;
        } else if (obj instanceof String str) {
            num = Integer.parseInt(str);
        } else {
            throw new RequiredCriteriaNotMatchException(METHOD_REQUIRE_NON_NEGATIVE_SUPPPORTS_ONLY_INTEGERS_OR_STRINGS);
        }

        if (num < 0) {
            throw new RequiredCriteriaNotMatchException(errorMessage);
        }
        return obj;

    }

}