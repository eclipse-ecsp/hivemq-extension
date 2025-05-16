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

package org.eclipse.ecsp.hivemq.auth.authorization;

import org.eclipse.ecsp.hivemq.base.IgniteAuthorizerExtension;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.util.Properties;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * This class is to test AuthorizationFactory class.
 */
public class AuthorizationFactoryTest {
    private Properties props;

    /**
     * This setup method gets called before each test case and load required properties.
     */
    @Before
    public void setup() {
        initMocks(this);
        try {
            props = PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test case for the {@link AuthorizationFactory#getInstance(AnnotationConfigApplicationContext)} method.
     * This method tests various scenarios for obtaining an instance of the {@link IgniteAuthorizerExtension}.
     * It covers cases such as ClassNotFoundException, null ClassLoader, and successful instance creation.
     *
     * @throws Exception if an error occurs during the test
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testGetAuthorizationInstance() throws Exception {
        // Case 1: ClassNotFoundException
        AnnotationConfigApplicationContext applicationContext = Mockito.mock(AnnotationConfigApplicationContext.class);
        props.remove(ApplicationConstants.HIVEMQ_PLUGIN_AUTHORIZATION_IMPL_CLASS);
        Exception exception = Assert.assertThrows(ClassNotFoundException.class, () -> {
            AuthorizationFactory.getInstance(applicationContext);
        });

        Assert.assertTrue(
                exception.getMessage().equalsIgnoreCase(ApplicationConstants.HIVEMQ_PLUGIN_AUTHORIZATION_IMPL_CLASS
                        + " refers to a class [" + null + "] that is not available on the classpath"));

        props.put(ApplicationConstants.HIVEMQ_PLUGIN_AUTHORIZATION_IMPL_CLASS,
                "org.eclipse.ecsp.hivemq.auth.authorization.Authorizer");

        // Case 2: ClassLoader is Null
        exception = Assert.assertThrows(RuntimeException.class, () -> {
            AuthorizationFactory.getInstance(applicationContext);
        });

        Assert.assertTrue(exception.getMessage().equalsIgnoreCase("ClassLoader is null"));

        // Case 3: ClassLoader and properties are present
        ClassLoader classLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(applicationContext.getClassLoader()).thenReturn(classLoader);
        Mockito.when(classLoader.loadClass(Mockito.anyString())).thenReturn((Class) Authorizer.class);
        Mockito.when(applicationContext.getBean(Mockito.anyString())).thenReturn(new Authorizer());

        IgniteAuthorizerExtension authorization = AuthorizationFactory.getInstance(applicationContext);
        Assert.assertTrue(authorization instanceof Authorizer);

        // Case 4: INSTANCE Already exists
        IgniteAuthorizerExtension authorizationNew = AuthorizationFactory.getInstance(applicationContext);
        Assert.assertEquals(authorization, authorizationNew);
        Assert.assertTrue(authorizationNew instanceof Authorizer);
    }

}
