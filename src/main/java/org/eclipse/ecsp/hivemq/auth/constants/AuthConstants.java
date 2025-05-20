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

package org.eclipse.ecsp.hivemq.auth.constants;

/**
 * This final class contains all constants used in application.
 */
public final class AuthConstants {
    
    private AuthConstants() {
    }
    
    public static final String TENANT = "";
    public static final String ENV = "";

    public static final String IMEI = "IMEI";
    public static final String REGISTER_SUFFIX = "Register";
    public static final String WHITELISTED_USERS = "whitelisted.users";
    public static final String MQTT_TOPIC_PREFIX = "mqtt.topic.prefix";
    public static final String MQTT_USER_PREFIX = "mqtt.user.prefix";
    public static final String MQTT_UNAME_PREFIX = "mqtt.uname.prefix";
    public static final String MQTT_QOS_LEVEL = "mqtt.qos.level";

    /**
     * Key for a propery which its value is a list of allowd topics that are
     * send without a vehicle id in the headers.
     */
    public static final String GLOBAL_SUB_TOPICS = "device.mqtt.global.subscribe.topics";
    public static final String PROFILE_CHECK_DISABLED_TOPICS = "profile.check.disabled.topics";
    public static final String DEVICE_MQTT_PUBLISH_TOPICS = "device.mqtt.publish.topics";
    public static final String ALL_DEVICE_PERMISSION_ENABLED = "all.device.permission.enabled";
    public static final String DEVICE_MQTT_SUBSCRIBE_TOPICS = "device.mqtt.subscribe.topics";
    public static final String USER_MQTT_SUBSCRIBE_TOPICS = "user.mqtt.subscribe.topics";
    public static final String DELIMITER = ",";
    public static final String MQTT_DELIMITER = "/";
    public static final String HASH = "#";
    public static final String PLUS = "+";
    public static final String HAA = "haa";
    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int TWO = 2;
    public static final int THREE = 3;
    public static final int FOUR = 4;
    public static final int SIX = 6;
    public static final int HTTP_OK = 200;
    public static final String ACTIVE = "active";

    public static final String WEBTOKEN_KEY_FORMAT_MESSAGE = "Web token key format is incorrect. Expected length:";
    public static final String JWT_PUBLIC_KEY_PATH = "jwt.publickey.path";

    public static final String SERVICE_CERTIFICATE_COMMON_NAMES = "service.certificate.common.names";
    public static final String SERVICE_CERTIFICATE_COMMON_NAME_PREFIX = "service.certificate.common.name.prefix";

    // TLS enabled, allow only DeviceMessaging to connect without certificate
    // (authenticate via username and password), property used when TLS is
    // enabled
    public static final String ALLOW_ONLY_WHITELIST_USER_WITHOUT_CERT = "allow.only.whitelist.user.without.cert";

    // Commcheck
    public static final String COMMCHECK_URL = "commcheck.deviceStatusUrl";
    public static final String COMMCHECK_ENABLE_ECU_SUSCIPIOUSCHECK = "commcheck.enableSuscipiousEcuCheck";
    public static final String DISCONNECT_TOPIC_NAME = "swap.response.topic.name";

    // Vehicle Profile service
    public static final String VEHICLE_PROFILE_URL = "d2v.http.url";
    public static final String DEVICE_ID_PARAM = "d2v.http.request.device.id.param";
    public static final String DEVICE_TYPE_PARAM = "d2v.http.request.device.type.param";
    public static final String VEHICLE_ID_KEY = "d2v.http.response.vehicle.id.key";
    public static final String MESSAGE_NODE_KEY = "message";
    public static final String MESSAGE_SUCCESS = "SUCCESS";
    public static final String DATA_NODE_KEY = "data";
    public static final String JSON_ECUS_PATH_NAME = "d2v.http.response.devices.parent.key";
    public static final String IS_ALLOWED_ALL_TOPIC = "device.specific.permissions.enabled";
    public static final String CONNECTED_PLATFORM = "connectedPlatform";
    // Property for Authorized party - the party to which the ID Token was
    // issued
    public static final String CLAIM_SET_AZP = "azp";
    // Property for "sub" (Subject) The subject value MUST either be scoped to
    // be locally unique in the context of the issuer or be globally unique.
    public static final String CLAIM_SET_SUB = "sub";

    // For Validating the scopes in JWT
    public static final String JWT_VALIDATION_ENABLED = "jwt.validation.enabled";
    public static final String VALID_SCOPES = "jwt.valid.scopes";

    public static final String SEMICOLON = ";";
    public static final String COLON = ":";
    public static final String CONF_SERVICEID_SERVICE_MAPPING = "mqtt.topic.service.mapping";
    public static final String STATIC_CRED_ENABLED = "static.password.enabled";
    public static final String STATIC_CRED = "mqtt.user.password";
    public static final String CERT_BEGIN = "-----BEGIN CERTIFICATE-----\n";
    public static final String END_CERT = "-----END CERTIFICATE-----";
    public static final String CERTIFICATE_TYPE = "X.509";
    public static final String INTERNAL_SERVICE_PORT = "internal.service.port";
    public static final String HEALTH_CHECK_USER = "health.check.user";
    public static final String HEALTH_CHECK_PORT = "health.check.port";
    public static final String HEALTH_CHECK_TOPIC = "health.check.topic";
    public static final String KEEP_ALIVE_TOPIC_NAME = "keep.alive.topic";
    public static final int SINGLE_CONNECTION = 1;
    public static final String CLEAN_SESSION = "user.cleansession.enabled";
    public static final String QOS_LEVEL_ENABLED = "user.qos.level.change.enabled";
    public static final String QOS_LEVEL_2C_VALUE = "user.qos.level.value.for.2c";
    public static final String QOS_LEVEL_2D_VALUE = "user.qos.level.value.for.2d";

    //constants for crash alert notifications
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_CORRELATIONID = "correlationid";
    public static final String USER_MANAGEMENT_BASE_URL = "user.management.base.url";
    public static final String USER_MANAGEMENT_API_URL = "user.management.api.url";
    public static final String PREFIX_DMPORTAL = "dmportal_";
    
    public static final String USERNAME = "username";
    public static final String PROPERTY_FILE_PATH = "/opt/hivemq/conf/";
}
