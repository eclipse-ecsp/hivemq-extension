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

package org.eclipse.ecsp.hivemq.auth.authentication;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.services.Services;
import org.eclipse.ecsp.hivemq.base.IgniteAuthenticationCallback;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import java.util.Properties;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * This class is to test AuthenticationFactory class.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Services.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class AuthenticationFactoryTest {
    private Properties props;
    @Mock
    private MetricRegistry registry;

    /**
     * This setup method gets called before each test case and load required properties.
     */
    @Before
    public void setup() {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(registry);
        initMocks(this);
        try {
            props = PropertyLoader.getProperties("src/test/resources/hivemq-plugin-base.properties");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Test case for the {@link AuthenticationFactory#getInstance(AnnotationConfigApplicationContext)} method.
     * This method tests the behavior of the method under different scenarios.
     * Scenarios:
     * 1. Case 1: ClassNotFoundException - When the authentication implementation class is not available on the 
     *      classpath.
     * 2. Case 2: ClassLoader is Null - When the class loader is null.
     * 3. Case 3: ClassLoader and properties are present - When the class loader and properties are present.
     * 4. Case 4: INSTANCE Already exists - When the authentication instance already exists.
     *
     * @throws Exception if an exception occurs during the test.
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testGetAuthenticationInstance() throws Exception {
        // Case 1: ClassNotFoundException
        AnnotationConfigApplicationContext applicationContext = Mockito.mock(AnnotationConfigApplicationContext.class);
        props.remove(ApplicationConstants.HIVEMQ_PLUGIN_AUTHENTICATION_IMPL_CLASS);
        Exception exception = Assert.assertThrows(ClassNotFoundException.class, () -> {
            AuthenticationFactory.getInstance(applicationContext);
        });

        Assert.assertTrue(
                exception.getMessage().equalsIgnoreCase(ApplicationConstants.HIVEMQ_PLUGIN_AUTHENTICATION_IMPL_CLASS
                        + " refers to a class [" + null + "] that is not available on the classpath"));

        props.put(ApplicationConstants.HIVEMQ_PLUGIN_AUTHENTICATION_IMPL_CLASS,
                "org.eclipse.ecsp.hivemq.auth.authentication.JWTAuthentication");

        // Case 2: ClassLoader is Null
        exception = Assert.assertThrows(RuntimeException.class, () -> {
            AuthenticationFactory.getInstance(applicationContext);
        });

        Assert.assertTrue(exception.getMessage().equalsIgnoreCase("ClassLoader is null"));

        // Case 3: ClassLoader and properties are present
        ClassLoader classLoader = Mockito.mock(ClassLoader.class);
        Mockito.when(applicationContext.getClassLoader()).thenReturn(classLoader);
        Mockito.when(classLoader.loadClass(Mockito.anyString())).thenReturn((Class) JwtAuthentication.class);

        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(registry);
        JwtAuthentication jwt = new JwtAuthentication();
        Mockito.when(applicationContext.getBean(Mockito.anyString())).thenReturn(jwt);

        IgniteAuthenticationCallback authentication = AuthenticationFactory.getInstance(applicationContext);
        Assert.assertTrue(authentication instanceof JwtAuthentication);

        // Case 4: INSTANCE Already exists
        IgniteAuthenticationCallback authenticationNew = AuthenticationFactory.getInstance(applicationContext);
        Assert.assertEquals(authentication, authenticationNew);
        Assert.assertTrue(authenticationNew instanceof JwtAuthentication);
    }

}
