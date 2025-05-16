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
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission.MqttActivity;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission.Qos;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.Listener;
import com.hivemq.extension.sdk.api.packets.connect.ConnectPacket;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.eclipse.ecsp.hivemq.auth.authorization.Authorizer;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.callbacks.SubscriptionStatusHandler;
import org.eclipse.ecsp.hivemq.kafka.ApplicationConstants;
import org.eclipse.ecsp.utils.logger.IgniteLogger;
import org.eclipse.ecsp.utils.logger.IgniteLoggerFactory;
import org.json.simple.JSONObject;
import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import static java.util.Arrays.asList;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.PROFILE_CHECK_DISABLED_TOPICS;
import static org.eclipse.ecsp.hivemq.utils.CommonUtil.getField;

/**
 * This is a utility class.
 */
public class HivemqUtils {
    private HivemqUtils() {
    }

    private static final String LOG_INVALID_VALUE_FOR_KEY_IN_PROPERTY_FILE = "Invalid value {} for key {} "
            + "in property file {}";
    private static final String LOG_PROPERTY_KEY_VALUE = "Property key::{} value:: {}";
    private static final IgniteLogger LOGGER = IgniteLoggerFactory.getLogger(HivemqUtils.class);
    private static final SubscriptionStatusHandler STATUS_HANDLER = new SubscriptionStatusHandler();
    private static final String REGEX_COMMA_SPLIT_TRIM = "\\s*,\\s*";
    private static final String PERF_USER = "perf_user";

    private static final String VP_PAYLOAD_ATTR_TELEMATICS_NAME = "telematics";
    public static final String VP_PAYLOAD_ATTR_CLIENTID_NAME = "clientId";
    private static final int PORT = 1883;

    /**
     * This method get configure global topics from properties.
     *
     * @return list of global topics.
     */
    public static List<String> getGlobalSubTopics() {
        List<String> globalSubTopics = new ArrayList<>();
        try {
            String mqttTopicPrefix = PropertyLoader.getValue(AuthConstants.MQTT_TOPIC_PREFIX);
            globalSubTopics = PropertyLoader.getPropertiesMap()
                    .getOrDefault(AuthConstants.GLOBAL_SUB_TOPICS, new ArrayList<>()).stream()
                    .map(topic -> mqttTopicPrefix + topic).toList();
            LOGGER.debug("Global subscribe topic  list: {} and mqttTopicPrefix: {}", globalSubTopics, mqttTopicPrefix);
        } catch (Exception e) {
            LOGGER.error("globalSubTopics is not contructed", e);
        }
        return globalSubTopics;
    }

    /**
     * This method creates connect/disconnect event to be sent to kafka.
     */
    @SuppressWarnings("unchecked")
    public static JSONObject createEvent(String pdid, String connectionStatus) {
        JSONObject eventObj = new JSONObject();
        long ts = System.currentTimeMillis();
        eventObj.put(EventMetadataConstants.EVENT_UPLOAD_TIMESTAMP, ts);
        eventObj.put(EventMetadataConstants.PDID, pdid);

        JSONObject dataObj = new JSONObject();
        dataObj.put(EventMetadataConstants.EVENT_ID, EventMetadataConstants.CONNECTION_STATUS_EVENT_ID);
        dataObj.put(EventMetadataConstants.EVENT_VERSION, EventMetadataConstants.EVENT_VERSION_VALUE);
        dataObj.put(EventMetadataConstants.EVENT_TIMESTAMP, ts);
        dataObj.put(EventMetadataConstants.EVENT_TIMEZONE, EventMetadataConstants.UTC_TIMEZONE_DIFF);
        dataObj.put(EventMetadataConstants.PDID, pdid);

        JSONObject eventData = new JSONObject();
        eventData.put(EventMetadataConstants.EVENT_VALUE, connectionStatus);
        dataObj.put(EventMetadataConstants.EVENT_DATA, eventData);

        eventObj.put(EventMetadataConstants.EVENT_PAYLOAD, dataObj);
        return eventObj;
    }

    /**
     * This method creates commcheck ack response event.
     *
     * @param pdid - client id
     * @return message json object
     */
    @SuppressWarnings("unchecked")
    public static JSONObject createCommcheckAckResponseEvent(String pdid) {
        JSONObject eventObj = new JSONObject();
        long ts = System.currentTimeMillis();
        eventObj.put(EventMetadataConstants.EVENT_UPLOAD_TIMESTAMP, ts);
        eventObj.put(EventMetadataConstants.PDID, pdid);

        JSONObject dataObj = new JSONObject();
        dataObj.put(EventMetadataConstants.EVENT_ID, EventMetadataConstants.COMMCHECK_RESPONSE_ACK_EVENT);
        dataObj.put(EventMetadataConstants.EVENT_VERSION, EventMetadataConstants.EVENT_VERSION_VALUE);
        dataObj.put(EventMetadataConstants.EVENT_TIMESTAMP, ts);
        dataObj.put(EventMetadataConstants.EVENT_TIMEZONE, EventMetadataConstants.UTC_TIMEZONE_DIFF);
        dataObj.put(EventMetadataConstants.PDID, pdid);

        eventObj.put(EventMetadataConstants.EVENT_PAYLOAD, dataObj);
        return eventObj;
    }

    /**
     * This method read/load public key from given path.
     *
     * @param filepath - path on which public key is available
     * @return Public key
     * @throws IOException File not available on given path
     * @throws NoSuchAlgorithmException If wrong algorithm is passed in KeyFactory.getInstance
     * @throws InvalidKeySpecException If invalid key is provided to generatePublic method
     */
    public static PublicKey readPublickey(String filepath)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        PublicKey publicKey = null;
        LOGGER.debug("readPublickey->filepath:" + filepath);
        String pkeyStr = "";
        pkeyStr = new String(Files.readAllBytes(Paths.get(filepath)));
        byte[] publicKeyBytes = Base64.decodeBase64(pkeyStr);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        publicKey = keyFactory.generatePublic(keySpec);
        return publicKey;
    }

    /**
     * Extract common name(CN) from given certificate string.
     *
     * @param cert - certificate string
     * @return common name
     * @throws CertificateException exception when extracting common name from
     *                              certificate
     */
    public static String getCnFromCert(String cert) throws CertificateException {
        try {
            String cnName = getCnFromCert(getCertificateFromString(cert));
            LOGGER.debug("Extracted common name is:{}", cnName);
            return cnName;
        } catch (CertificateException e) {
            LOGGER.error("Unable to find common name from certificate:{}", cert, e);
            throw e;
        }
    }

    /**
     * Extract common name from given X509Certificate.
     *
     * @param cert - X509Certificate certificate
     * @return common name
     */
    public static String getCnFromCert(X509Certificate cert) {
        X500Principal principal;
        principal = cert.getSubjectX500Principal();
        X500Name x500name = new X500Name(principal.getName());
        RDN cn = x500name.getRDNs(BCStyle.CN)[0];
        String cnName = IETFUtils.valueToString(cn.getFirst().getValue());
        LOGGER.debug("Extracted common name is:{}", cnName);
        return cnName;
    }

    /**
     * Creates a X509Certificate certificate from given certificate string.
     *
     * @param cert - certificate string
     * @return - X509Certificate certificate
     * @throws CertificateException - if any exception while creating certificate.
     */
    public static X509Certificate getCertificateFromString(String cert) throws CertificateException {
        final String emptyString = "";
        String trimmedCert = cert.replace(AuthConstants.CERT_BEGIN, emptyString).replace(AuthConstants.END_CERT,
                emptyString);
        byte[] encoded = Base64.decodeBase64(trimmedCert);
        LOGGER.debug("Removed begin and end certificate comments from cert :{} ", trimmedCert);
        CertificateFactory fact = CertificateFactory.getInstance(AuthConstants.CERTIFICATE_TYPE);
        return (X509Certificate) fact.generateCertificate(new ByteArrayInputStream(encoded));
    }

    /**
     * This method check if connecting client is a internal service.
     *
     * @param userName                 - username
     * @param commonName               - common name from certificate
     * @param serviceCertificatePrefix - configured service certificate prefix
     * @return - true if common name starts with configured serviceCertificatePrefix
     */
    public static boolean isServiceRequest(String userName, String commonName, String serviceCertificatePrefix) {
        if (PERF_USER.equals(userName)) {
            return false;
        }
        LOGGER.debug("isAServiceRequest - The provided certificate cn {} is compared against {}", commonName,
                serviceCertificatePrefix);
        return (commonName != null && commonName.toUpperCase().startsWith(serviceCertificatePrefix)) ? Boolean.TRUE
                : Boolean.FALSE;
    }

    /**
     * This method checks if connection request is from an internal service by
     * checking request port.
     *
     * @param properties - loaded properties on application startup
     * @param clientData - mqtt client connection information
     * @return - true if its a service request
     */
    public static boolean isInternalService(Properties properties, ConnectionInformation clientData) {
        String oneWayPort = properties.getProperty(AuthConstants.INTERNAL_SERVICE_PORT);
        List<String> portList = Arrays.asList(oneWayPort.split(AuthConstants.DELIMITER));
        Integer listenerPort = 0;
        Optional<Listener> listener = clientData.getListener();
        if (listener.isPresent()) {
            listenerPort = listener.get().getPort();
        }
        final Integer listenerPortCopy = listenerPort;
        LOGGER.debug("Internal service port configured:{} and listenerPort:{}", portList, listenerPort);
        return portList.stream().anyMatch(port -> port.trim().equalsIgnoreCase(listenerPortCopy.toString()));
    }

    /**
     * This method checks if connection request is from an internal service by
     * checking request port.
     *
     * @param userName   - username
     * @param clientData - mqtt client connection information
     * @return - true if its a service request
     */
    public static boolean isInternalServiceClient(String userName, ConnectionInformation clientData) {
        Integer listenerPort = 0;
        Optional<Listener> listener = clientData.getListener();
        if (listener.isPresent()) {
            listenerPort = listener.get().getPort();
        }
        return (("haa_api".equalsIgnoreCase(userName)) && (PORT == listenerPort));
    }

    /**
     * Healthcheck user identification.
     *
     * @param properties - properties
     * @param clientData - client connection information
     * @return true/false
     */
    public static boolean isHealthCheckUser(Properties properties, ConnectionInformation clientData) {
        boolean isHealthCheckUser = false;
        try {
            String configHealthUser = properties.getProperty(AuthConstants.HEALTH_CHECK_USER);
            int configHealthPort = Integer.parseInt(properties.getProperty(AuthConstants.HEALTH_CHECK_PORT));
            Integer listenerPort = 0;
            Optional<Listener> listener = clientData.getListener();
            if (listener.isPresent()) {
                listenerPort = listener.get().getPort();
            }
            final Optional<String> usernameOptional = clientData.getConnectionAttributeStore()
                    .getAsString(AuthConstants.USERNAME);
            String userName = usernameOptional.isPresent() ? usernameOptional.get() : StringUtils.EMPTY;
            LOGGER.debug(
                    "Health check has been configurerd for the config username:{} with service port:{} "
                            + "and with incoming user:{} and port:{}",
                    configHealthUser, configHealthPort, userName, listenerPort);
            isHealthCheckUser = Objects.nonNull(configHealthUser) && configHealthUser.equals(userName)
                    && configHealthPort == listenerPort;
        } catch (Exception ex) {
            LOGGER.error("Below exception occured while checking health check user for client data: {} ", clientData,
                    ex);
        }
        return isHealthCheckUser;

    }

    /**
     * This method remove any prefix, if there, from username based on configured
     * list of username prefixes.
     *
     * @param mqttUserName - username
     * @return - username without prefix
     */
    public static String getUserWithoutPrefix(String mqttUserName) {
        List<String> mqttUserPrefixList = Arrays
                .asList(HivemqUtils.getPropertyArrayValue(AuthConstants.MQTT_USER_PREFIX));
        String userName = null;
        for (String userPrefix : mqttUserPrefixList) {
            if (mqttUserName.startsWith(userPrefix)) {
                userName = mqttUserName.replace(userPrefix, ApplicationConstants.BLANK);
                LOGGER.debug("userName without prefix: {}", userName);
                return userName;
            }
        }
        LOGGER.debug("userName with prefix: {}", mqttUserName);
        return mqttUserName;
    }

    /**
     * This method checks if given user is a whitelisted user by checking list of
     * configured whitelisted users.
     *
     * @param user - username
     * @return true if its a whitelisted user
     */
    public static boolean isWhiteListedUser(String user) {
        boolean isWhiteListedUser = false;
        if (StringUtils.isEmpty(user)) {
            return isWhiteListedUser;
        }
        if (PERF_USER.equals(user)) {
            return isWhiteListedUser;
        }

        try {
            isWhiteListedUser = PropertyLoader.getPropertiesMap().get(AuthConstants.WHITELISTED_USERS)
                    .contains(getUserWithoutPrefix(user));
        } catch (Exception e) {
            LOGGER.error("Error while checking white list user.", e);
        }
        return isWhiteListedUser;
    }

    /**
     * This method gets property value of given key from property map.
     *
     * @param key - property key
     * @return property value in integer
     */
    public static int getPropertyIntValue(final String key) {
        String value = "0";
        int intValue = 0;
        try {
            value = PropertyLoader.getValue(key);
            LOGGER.debug(LOG_PROPERTY_KEY_VALUE, key, value);
            if (value == null) {
                return intValue;
            }
            intValue = Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.error(LOG_INVALID_VALUE_FOR_KEY_IN_PROPERTY_FILE, value, key, e);
        }

        return intValue;
    }

    /**
     * This method gets property value of given key from property map.
     *
     * @param key - property key
     * @return property value in double
     */
    public static Double getPropertyDoubleValue(final String key) {
        String value = "0";
        Double doubleVal = 0.0;
        try {
            value = PropertyLoader.getValue(key);
            LOGGER.debug(LOG_PROPERTY_KEY_VALUE, key, value);
            if (value == null) {
                return doubleVal;
            }
            doubleVal = Double.valueOf(value.trim());
        } catch (NumberFormatException e) {
            LOGGER.error(LOG_INVALID_VALUE_FOR_KEY_IN_PROPERTY_FILE, value, key, e);
        }

        return doubleVal;
    }

    /**
     * This method gets property value of given key from property map.
     *
     * @param key - property key
     * @return property value in boolean
     */
    public static Boolean getPropertyBooleanValue(final String key) {
        String strValue = "false";
        Boolean value = Boolean.FALSE;
        try {
            strValue = PropertyLoader.getValue(key);
            LOGGER.debug(LOG_PROPERTY_KEY_VALUE, key, strValue);
            value = Boolean.parseBoolean(strValue);
        } catch (Exception e) {
            LOGGER.error(LOG_INVALID_VALUE_FOR_KEY_IN_PROPERTY_FILE, strValue, key, e);
        }

        return value;
    }

    /**
     * Provide permission for a topic.
     *
     * @param topicName topic for which permission is required.
     * @param activity  subscribe/publish.
     * @return mqtt topic permission
     */
    public static TopicPermission getPermission(String topicName, MqttActivity activity) {
        Qos qos = Qos.valueOf(PropertyLoader.getValue(AuthConstants.MQTT_QOS_LEVEL, "ALL"));
        return getPermission(topicName, activity, qos);
    }

    /**
     * Provides permission on given topic.
     *
     * @param topicName - topic for which permission is required.
     * @param activity  - subscribe/publish.
     * @param qos       - quality of service
     * @return Topic Permission
     */
    public static TopicPermission getPermission(String topicName, MqttActivity activity, Qos qos) {
        LOGGER.trace("topic name {} QOS is {}", topicName, qos);
        return Builders.topicPermission().topicFilter(topicName).type(TopicPermission.PermissionType.ALLOW).qos(qos)
                .activity(activity).build();
    }

    /*
     * On disconnect - both subscription cache and permissionMap clean need to be
     * done 1. when no other connection in progress 2. For ecu swap or suspicious
     * device when disconnect happens .. this need to be called explicitly before
     * disconnect as immediate reconnect may not clear the cache.
     */
    public static void clearCache(final String clientId, boolean removeFromRedis) {
        clearPermissionMap(clientId);
        removeSubscription(clientId, removeFromRedis);
    }

    public static void clearPermissionMap(final String clientId) {
        LOGGER.info("Clearing local cache for {}", clientId);
        Authorizer.removeFromPermissionMap(clientId);
    }

    public static void removeSubscription(final String clientId, boolean removeFromRedis) {
        STATUS_HANDLER.removeSubscription(clientId, removeFromRedis);
    }

    /**
     * This method gets configured acceptable topic list from property map.
     *
     * @return List of acceptable topics.
     */
    public static List<String> getAcceptableTopics() {
        String acceptableTopicsProperty = Optional.ofNullable(PropertyLoader.getValue(PROFILE_CHECK_DISABLED_TOPICS))
                .orElse(StringUtils.EMPTY);
        return acceptableTopicsProperty.isEmpty() ? new ArrayList<>() : asList(acceptableTopicsProperty.split(","));
    }

    /**
     * This method reads property value for given key and split that with comma
     * returns array of those values.
     *
     * @param key - key for which need to read value
     * @return string array of property value
     */
    public static String[] getPropertyArrayValue(final String key) {
        String strValue = null;
        String[] valueArr = {};
        try {
            strValue = PropertyLoader.getValue(key);
            LOGGER.debug(LOG_PROPERTY_KEY_VALUE, key, strValue);
            if (StringUtils.isNotEmpty(strValue)) {
                valueArr = strValue.trim().split(REGEX_COMMA_SPLIT_TRIM);
            }

            LOGGER.debug("Property key::{} valueArr:: {}", key, valueArr);
        } catch (Exception e) {
            LOGGER.error(LOG_INVALID_VALUE_FOR_KEY_IN_PROPERTY_FILE, strValue, key, e);
        }

        return valueArr;
    }

    /**
     * Retrieves the username from the given ConnectPacket.
     *
     * @param connectPacket The ConnectPacket from which to retrieve the username.
     * @return The username if present, or an empty string if not.
     */
    public static String getUserName(final ConnectPacket connectPacket) {
        return connectPacket.getUserName().orElse(StringUtils.EMPTY);
    }

    /**
     * Method returns password from mqtt client connect packet.
     *
     * @param connectPacket - mqtt client connect packet
     * @return - password string
     */
    public static String getPassword(final ConnectPacket connectPacket) {
        String password = null;
        Optional<ByteBuffer> passwordBuffer = connectPacket.getPassword();
        if (passwordBuffer.isPresent()) {
            password = StandardCharsets.ISO_8859_1.decode(passwordBuffer.get()).toString();
        }
        return password;
    }

    /**
     * This method checks if a client is connect to hivemq broker or not.
     *
     * @param clientId client id
     * @return - true if client is connected
     * @throws InterruptedException - InterruptedException
     * @throws ExecutionException   - ExecutionException
     */
    public static boolean isConnected(String clientId) throws InterruptedException, ExecutionException {
        boolean isConnected = HivemqServiceProvider.getBlockingClientService().isClientConnected(clientId).isDone()
                ? HivemqServiceProvider.getBlockingClientService().isClientConnected(clientId).get()
                : Boolean.FALSE;
        LOGGER.debug("For client:{} connection status:{}.", clientId, isConnected);
        return isConnected;
    }

    /**
     * Method returns telematics client id from give vehicle profile data.
     *
     * @param profileNode - vehicle profile data
     * @param clientId    - client id
     * @return telematics client id
     */
    public static String getTelematicsClientId(final JsonNode profileNode, String clientId) {

        String telematicsClientId = null;
        try {
            LOGGER.debug("Get telematicsClientId from VehcileProfile for clientId: {}", clientId);
            telematicsClientId = getField(profileNode, VP_PAYLOAD_ATTR_TELEMATICS_NAME, VP_PAYLOAD_ATTR_CLIENTID_NAME);
            LOGGER.debug("Telematics clientId {} for clientId {}", telematicsClientId, clientId);
        } catch (Exception e) {
            LOGGER.error("Exception occured while getting telematics ClientId for client:{} ", clientId, e);
        }
        return telematicsClientId;
    }

}