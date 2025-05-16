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

import org.eclipse.ecsp.analytics.stream.base.utils.ObjectUtils;
import org.eclipse.ecsp.hivemq.exceptions.RequiredCriteriaNotMatchException;
import org.junit.Assert;
import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.assertTrue;

/**
 * This class contains unit tests for the ObjectUtils class.
 */
public class ObjectUtilsTest {
    
    private static final int REQ_LIST_SIZE = 2;
    private static final int NEG_ONE = -1;

    /**
     * Test case to verify the behavior of the ObjectUtils.requireNonEmpty() method.
     * It checks if the method returns a non-null value when provided with a non-empty string.
     */
    @Test
    public void testObjectUtils_requireNonEmpty() {
        Assert.assertNotNull(ObjectUtils.requireNonEmpty("str", "no message"));
    }

    /**
     * Test case to verify that the `requireNonEmpty` method throws the `RequiredCriteriaNotMatchException`
     * when an empty string is passed as the input.
     *
     * @throws RequiredCriteriaNotMatchException if the required criteria is not met
     */
    @Test(expected = RequiredCriteriaNotMatchException.class)
    public void testObjectUtils_requireNonEmpty_Exception() {
        ObjectUtils.requireNonEmpty("", "no message");
    }

    /**
     * Test case for the {@link ObjectUtils#requireSizeOf(List, int, String)} method.
     * 
     * <p>This test verifies that the {@link ObjectUtils#requireSizeOf(List, int, String)} method
     * returns true when the size of the given list matches the expected size.
     */
    @Test
    public void testObjectUtils_requireSizeOf() {
        List<String> arrList = new ArrayList<>();
        arrList.add("test");
        assertTrue(ObjectUtils.requireSizeOf(arrList, 1, "no message"));
    }

    /**
     * Test case to verify that the `requireSizeOf` method throws a `RequiredCriteriaNotMatchException`
     * when the size of the given list does not match the required size.
     */
    @Test(expected = RequiredCriteriaNotMatchException.class)
    public void testObjectUtils_requireSizeOf_Exception() {
        List<String> arrList = new ArrayList<>();
        arrList.add("test");
        ObjectUtils.requireSizeOf(arrList, REQ_LIST_SIZE, "no message");
    }

    /**
        * Test case for the {@link ObjectUtils#requireNonNull(Object, String)} method.
        * It verifies that the method returns a non-null object when a non-null object is passed as the first argument.
        */
    @Test
    public void testObjectUtils_requireNonNull() {
        Assert.assertNotNull(ObjectUtils.requireNonNull("ack", "no message"));
    }

    /**
     * Test case for the {@link ObjectUtils#requireMinSize(List, int, String)} method.
     * It verifies that the method returns true when the input list has a minimum size of 1.
     */
    @Test
    public void testObjectUtils_requireMinSize() {
        List<String> arrList = new ArrayList<>();
        arrList.add("test1");
        arrList.add("test2");
        assertTrue(ObjectUtils.requireMinSize(arrList, 1, "no message"));
    }

    /**
    * Test case to verify that the {@link ObjectUtils#requireMinSize(List, int, String)} 
    * method throws a {@link RequiredCriteriaNotMatchException}
    * when the size of the input list is less than the required minimum size.
    */
    @Test(expected = RequiredCriteriaNotMatchException.class)
    public void testObjectUtils_requireMinSize_Exception() {
        List<String> arrList = new ArrayList<>();
        arrList.add("test");
        ObjectUtils.requireMinSize(arrList, REQ_LIST_SIZE, "no message");
    }

    /**
     * Test case for the {@link ObjectUtils#requiresNotNullAndNotEmpy(List, String)} method.
     * 
     * <p>This test verifies that the {@link ObjectUtils#requiresNotNullAndNotEmpy(List, String)} method
     * returns true when a non-null and non-empty list is passed as the first argument.
     * 
     * <p>@see ObjectUtils#requiresNotNullAndNotEmpy(List, String)
     */
    @Test
    public void testObjectUtils_requiresNotNullAndNotEmpy() {
        List<String> arrList = new ArrayList<>();
        arrList.add("test");
        assertTrue(ObjectUtils.requiresNotNullAndNotEmpy(arrList, "no message"));
    }

    /**
    * Test case to verify that the ObjectUtils.requiresNotNullAndNotEmpy() method 
    * throws the RequiredCriteriaNotMatchException
    * when the input list is null or empty.
    */
    @Test(expected = RequiredCriteriaNotMatchException.class)
    public void testObjectUtils_requiresNotNullAndNotEmpy_Exception() {
        List<String> arrList = new ArrayList<>();

        ObjectUtils.requiresNotNullAndNotEmpy(arrList, "no message");
    }

    /**
     * Test case for the {@link ObjectUtils#requireNonNegative(int, String)} method.
     * 
     * <p>This test verifies that the method returns a non-null value when a positive integer is provided.
     * 
     * <p>@see ObjectUtils#requireNonNegative(int, String)
     */
    @Test
    public void testObjectUtils_requireNonNegative() {
        Assert.assertNotNull(ObjectUtils.requireNonNegative(1, "no message"));
    }

    /**
        * Test case to verify the behavior of the {@link ObjectUtils#requireNonNegative(String, String)} method.
        * It checks if the method returns a non-null value when a non-negative string is provided.
        */
    @Test
    public void testObjectUtils_requireNonNegativeString() {
        Assert.assertNotNull(ObjectUtils.requireNonNegative("10", "no message"));
    }

    /**
     * Test case to verify that the ObjectUtils.requireNonNegative() method throws the RequiredCriteriaNotMatchException
     * when a non-negative string is required but a negative string is provided.
     */
    @Test(expected = RequiredCriteriaNotMatchException.class)
    public void testObjectUtils_requireNonNegativeStringException() {
        List<String> arrList = new ArrayList<>();
        arrList.add("test");
        ObjectUtils.requireNonNegative(arrList, "no message");
    }

    /**
        * Test case to verify that the {@link ObjectUtils#requireNonNegative(int, String)} method
        * throws a {@link RequiredCriteriaNotMatchException} when a negative value is passed.
        */
    @Test(expected = RequiredCriteriaNotMatchException.class)
    public void testObjectUtils_requireNonNegative_Exception() {
        ObjectUtils.requireNonNegative(NEG_ONE, "no message");
    }
}
