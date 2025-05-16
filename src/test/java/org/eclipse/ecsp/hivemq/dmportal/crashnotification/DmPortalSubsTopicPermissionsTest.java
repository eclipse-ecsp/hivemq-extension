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
import com.hivemq.extension.sdk.api.services.builder.Builders;
import org.eclipse.ecsp.hivemq.auth.authorization.stub.StubTopicPermissionBuilder;
import org.eclipse.ecsp.hivemq.exceptions.ClientIdException;
import org.eclipse.ecsp.hivemq.exceptions.InvalidClientIdFormatException;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.hivemq.utils.UserManagementUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.util.List;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * Test class for DmPortalSubsTopicPermissions.
 */
@SuppressWarnings(value = { "unused" })
@RunWith(PowerMockRunner.class)
@PrepareForTest(Builders.class)
@PowerMockIgnore({ "javax.security.auth.x500.X500Principal", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*",
    "javax.management.*" })
public class DmPortalSubsTopicPermissionsTest {

    @InjectMocks
    private DmPortalSubsTopicPermissions portalPermissions;

    @Mock
    private UserManagementUtil userManagement;

    /**
     * Sets up the test environment before each test case.
     *
     * @throws Exception if an error occurs during setup.
     */
    @Before
    public void setUp() throws Exception {
        initMocks(this);
        portalPermissions.init(PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties"));
    }

    /**
     * Test case to verify the successful retrieval of permissions for a DM Portal client.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testGetPermissionsForDmPortalClient_Success() throws Exception {
        // given - preparation
        when(userManagement.getUserCountry(Mockito.anyString())).thenReturn("ID");

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(new StubTopicPermissionBuilder());

        // when-execution
        String clientId = "dmportal_l1-admin@test.com_HUXOIDDN4HUN18";
        List<TopicPermission> mqttTopicPermission = portalPermissions.getTopicPermissions(clientId);
        List<String> topicsList = mqttTopicPermission.stream().map(topicPermission -> topicPermission.getTopicFilter())
                .toList();
        // then-verification
        Assert.assertTrue(topicsList.contains("ID/notification"));
    }

    /**
     * Test case to verify the behavior of getting permissions for an invalid DM Portal client ID.
     * 
     * <p>
     * This test checks if the method correctly returns an empty list of topic permissions when an invalid 
     * client ID is provided.
     * The client ID should start with the prefix "dmportal", but in this test,
     * it is set to "l3-admin@test.com_HUXOIDDN4HUN18".
     * </p>
     *
     * @throws Exception if an error occurs during the test execution
     */
    @Test
    public void testGetPermissionsForDmPortalClient_InvalidClientIdone() throws Exception {
        // given - preparation -It is invalid clientId as clientId is not starting with
        // prefix dmportal so acc. to code we will be
        // getting empty permissions list.
        String clientId = "l3-admin@test.com_HUXOIDDN4HUN18";

        // when-execution
        List<TopicPermission> mqttTopicPermission = portalPermissions.getTopicPermissions(clientId);

        // then-verification
        Assert.assertTrue(mqttTopicPermission.isEmpty());
    }

    /**
     * Test case to verify the behavior of the getPermissionsForDmPortalClient method when an empty 
     * client ID is provided.
     * It expects a ClientIdException to be thrown.
     *
     * @throws Exception if an error occurs during the test.
     */
    @Test(expected = ClientIdException.class)
    public void testGetPermissionsForDmPortalClient_emptyClientId() throws Exception {
        // given - preparation
        String clientId = "";

        // when-execution
        List<TopicPermission> mqttTopicPermission = portalPermissions.getTopicPermissions(clientId);

        // then-verification - expected ClientIdException exception.
    }

    /**
     * Test case to verify the behavior of getting permissions for a DM Portal client with an invalid client ID.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test(expected = InvalidClientIdFormatException.class)
    public void testGetPermissionsForDmPortalClient_InvalidClientId() throws Exception {
        // given - preparation - It is invalid format of clientId, the correct format is
        // dmportal_l3-admin@test.com_HUXOIDDN4HUN18.
        String clientId = "dmportal_l3-admin@test.com-HUXOIDDN4HUN18";

        // when-execution
        List<TopicPermission> mqttTopicPermission = portalPermissions.getTopicPermissions(clientId);

        // then-verification - expected InvalidClientIdFormatException exception.
    }

}
