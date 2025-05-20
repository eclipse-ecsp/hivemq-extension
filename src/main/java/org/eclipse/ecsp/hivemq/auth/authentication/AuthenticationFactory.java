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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.ecsp.hivemq.base.IgniteAuthenticationCallback;
import org.eclipse.ecsp.hivemq.exceptions.ClassLoaderNotFoundException;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * This is a factory class which creates object of authenticator, configured in
 * properties.
 */
public class AuthenticationFactory {
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(AuthenticationFactory.class);

    private static final Object LOCK = new Object();
    private static IgniteAuthenticationCallback instance;

    private AuthenticationFactory() {

    }

    
    /**
     * Returns a singleton instance of {@link IgniteAuthenticationCallback} by loading the implementation class
     * specified in the application properties. The implementation class is retrieved from the application context
     * using its bean name (converted to lower case).
     *
     * <p>This method is thread-safe and ensures that only one instance of the authentication callback is created.
     * If the instance already exists, it is returned immediately. Otherwise, the method attempts to load the class
     * specified by the {@code HIVEMQ_PLUGIN_AUTHENTICATION_IMPL_CLASS} property, retrieves its bean from the
     * provided {@link AnnotationConfigApplicationContext}, and assigns it as the singleton instance.</p>
     *
     * @param applicationContext the Spring application context used to retrieve the authentication callback bean
     * @return the singleton instance of {@link IgniteAuthenticationCallback}
     * @throws ClassNotFoundException if the implementation class name is not specified or cannot 
     *      be found on the classpath
     * @throws ClassLoaderNotFoundException if the class loader is null
     */
    public static synchronized IgniteAuthenticationCallback getInstance(
            AnnotationConfigApplicationContext applicationContext)
            throws ClassNotFoundException, ClassLoaderNotFoundException {
        if (instance != null) {
            return instance;
        }
        synchronized (LOCK) {
            String className = PropertyLoader.getValue(ApplicationConstants.HIVEMQ_PLUGIN_AUTHENTICATION_IMPL_CLASS);
            LOGGER.info("Loading authentication impl class {}", className);
            if (StringUtils.isEmpty(className)) {
                throw new ClassNotFoundException(ApplicationConstants.HIVEMQ_PLUGIN_AUTHENTICATION_IMPL_CLASS
                        + " refers to a class [" + className + "] that is not available on the classpath");
            }
            ClassLoader classLoader = applicationContext.getClassLoader();
            if (classLoader != null) {
                instance = (IgniteAuthenticationCallback) applicationContext
                        .getBean(classLoader.loadClass(className).getSimpleName().toLowerCase());
            } else {
                LOGGER.error("ClassLoader is null in AuthenticationFactory");
                throw new ClassLoaderNotFoundException("ClassLoader is null");
            }
        }
        return instance;
    }
}
