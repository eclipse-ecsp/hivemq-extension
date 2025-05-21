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

package org.eclipse.ecsp.hivemq.callbacks;

import com.codahale.metrics.MetricRegistry;
import com.hivemq.extension.sdk.api.auth.parameter.TopicPermission;
import com.hivemq.extension.sdk.api.client.ClientContext;
import com.hivemq.extension.sdk.api.client.parameter.ClientInformation;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionAttributeStore;
import com.hivemq.extension.sdk.api.client.parameter.ConnectionInformation;
import com.hivemq.extension.sdk.api.client.parameter.Listener;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackInboundInput;
import com.hivemq.extension.sdk.api.interceptor.puback.parameter.PubackInboundOutput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundInput;
import com.hivemq.extension.sdk.api.interceptor.publish.parameter.PublishInboundOutput;
import com.hivemq.extension.sdk.api.packets.auth.ModifiableDefaultPermissions;
import com.hivemq.extension.sdk.api.packets.general.Qos;
import com.hivemq.extension.sdk.api.packets.puback.PubackPacket;
import com.hivemq.extension.sdk.api.packets.publish.ModifiablePublishPacket;
import com.hivemq.extension.sdk.api.packets.publish.PublishPacket;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import com.hivemq.extension.sdk.api.services.builder.TopicPermissionBuilder;
import com.hivemq.extension.sdk.api.services.session.ClientService;
import org.eclipse.ecsp.analytics.stream.base.PropertyNames;
import org.eclipse.ecsp.analytics.stream.base.metrics.reporter.CumulativeLogger;
import org.eclipse.ecsp.entities.IgniteBlobEvent;
import org.eclipse.ecsp.hivemq.auth.authorization.stub.StubManagedExtensionExecutorService;
import org.eclipse.ecsp.hivemq.auth.constants.AuthConstants;
import org.eclipse.ecsp.hivemq.base.TopicMapper;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscription;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCache;
import org.eclipse.ecsp.hivemq.cache.DeviceSubscriptionCacheFactory;
import org.eclipse.ecsp.hivemq.kafka.util.CompressionJack;
import org.eclipse.ecsp.hivemq.routing.TopicMapperFactory;
import org.eclipse.ecsp.hivemq.routing.TopicMapping;
import org.eclipse.ecsp.hivemq.routing.TopicMapping.Route;
import org.eclipse.ecsp.hivemq.sink.HivemqSinkService;
import org.eclipse.ecsp.hivemq.transform.IngestionSerializerFactory;
import org.eclipse.ecsp.hivemq.utils.HealthService;
import org.eclipse.ecsp.hivemq.utils.HivemqServiceProvider;
import org.eclipse.ecsp.hivemq.utils.IgniteTopicFormatter;
import org.eclipse.ecsp.hivemq.utils.KeepAliveHandler;
import org.eclipse.ecsp.hivemq.utils.PropertyLoader;
import org.eclipse.ecsp.key.IgniteStringKey;
import org.eclipse.ecsp.serializer.IngestionSerializer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import static org.eclipse.ecsp.hivemq.auth.constants.AuthConstants.PROFILE_CHECK_DISABLED_TOPICS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Class to test MessageStoreCallback methods.
 *
 * @author Binoy Mandal
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Builders.class, Services.class, HivemqSinkService.class, IngestionSerializerFactory.class,
                  TopicMapperFactory.class })
@PowerMockIgnore({ "com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "javax.management.*" })
public class MessageStoreCallbackTest {

    public static final String CLIENT_ID_DUMMY = "dummy";

    static {
        File path = new File(
                MessageStoreCallbackTest.class.getClassLoader().getResource("hivemq-plugin-base.properties").getFile());
        PropertyLoader.reload(path);
    }

    private MessageStoreCallback msgStoreCallBack;

    @Mock
    PublishInboundInput publishInboundInput;

    @Mock
    PublishInboundOutput publishInboundOutput;

    @Mock
    ClientInformation clientInformation;

    @Mock
    ConnectionInformation connectionInformation;

    @Mock
    ConnectionAttributeStore connectionAttributeStore;

    @Mock
    PublishPacket publishPacket;

    @Mock
    ClientContext clientContext;

    @Mock
    ModifiableDefaultPermissions modifiableDefaultPermissions;

    @Mock
    private TopicPermissionBuilder permissionBuilder;

    @Mock
    TopicPermission permission;

    @Mock
    PubackInboundInput pubackInboundInput;

    @Mock
    PubackInboundOutput pubackInboundOutput;

    @Mock
    PubackPacket pubackPacket;

    @Mock
    private KeepAliveHandler keepAliveHandler;

    @Mock
    private DeviceSubscriptionCache deviceSubscriptionCache;

    @Mock
    private HivemqSinkService hivemqSinkService;

    @Mock
    private DeviceSubscriptionCacheFactory deviceSubscriptionCacheFactory;

    @Mock
    private TopicMapper topicMapper;

    @Mock
    private CompressionJack compressionUtil;

    @Mock
    private IngestionSerializer transformer;

    // @Mock
    // DeviceToVehicleMapper deviceToVehicleMapper;

    private String payload = "{\"message\":\"TEST\"}";

    private TopicMapping tm2Cloud;

    private TopicMapping tm2CloudRlTest;

    private TopicMapping tm2CloudSizeTest;

    private TopicMapping getTm2CloudFrequencyTest;

    private static final String DEVICEID = "device-1";

    private static final String VEHICLEID = "vehicle-1";

    private static final String SERVICEID = "ro";

    private static final String COMM_CHECK = "commcheck";

    private static final String SIZE_TEST_TOPIC = "-sizeTest";

    private static final String FREQUENCY_TEST_TOPIC = "-frequencyTest";

    private static final String REQUEST_LIMTER_TEST = "-rlTest";

    private final String mqtt2CloudTopic = "haa/harman/dev/" + DEVICEID + "/2c/" + SERVICEID;

    private static final String MQTT_2_COMM_CHECK_TOPIC = "haa/harman/dev/" + DEVICEID + "/2c/" + COMM_CHECK;

    private final String mqtt2CloudTopicRlTest = "haa/harman/dev/" + DEVICEID + "/2c/" + SERVICEID
            + REQUEST_LIMTER_TEST;

    private final String mqtt2CloudTopicSizeTest = "haa/harman/dev/" + DEVICEID + "/2c/" + SERVICEID + SIZE_TEST_TOPIC;

    private final String mqtt2CloudTopicFrequencyTest = "haa/harman/dev/" + DEVICEID + "/2c/" + SERVICEID
            + FREQUENCY_TEST_TOPIC;

    private static final String COMM_CHECK_2_DEVICE_TOPIC = "haa/harman/dev/" + DEVICEID + "/2d/" + COMM_CHECK;

    private final String mqqt2DeviceTopic = "haa/harman/dev/" + DEVICEID + "/2d/" + SERVICEID;

    private static final String TOPIC_PREFIX = "haa/harman/dev/";
    private static final int ONE_THOUSAND = 1000;
    private static final int PORT = 1888;

    /**
     * This setup method gets called before each test case and load required properties and class objects.
     * 
     */
    @Before
    public void setup() {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.when(Services.extensionExecutorService()).thenReturn(new StubManagedExtensionExecutorService());
        msgStoreCallBack = new MessageStoreCallback();
        PropertyLoader.getProperties().put(AuthConstants.QOS_LEVEL_ENABLED, "false");
        PropertyLoader.getProperties().put(PropertyNames.LOG_PER_PDID, false);
        IgniteStringKey key = new IgniteStringKey();
        key.setKey(VEHICLEID);

        tm2Cloud = TopicMapping.builder().deviceId(DEVICEID).deviceStatusRequired(true).route(Route.TO_CLOUD)
                .serviceId("service-1").serviceName("ro").streamStatusTopic(mqtt2CloudTopic)
                .streamTopic(mqtt2CloudTopic).build();
        tm2CloudRlTest = TopicMapping.builder().deviceId(DEVICEID).deviceStatusRequired(true).route(Route.TO_CLOUD)
                .serviceId("service-1").serviceName("ro").streamStatusTopic(mqtt2CloudTopicRlTest)
                .streamTopic(mqtt2CloudTopicRlTest).build();

        tm2CloudSizeTest = TopicMapping.builder().deviceId(DEVICEID).deviceStatusRequired(true).route(Route.TO_CLOUD)
                .serviceId("service-1").serviceName("ro").streamStatusTopic(mqtt2CloudTopicSizeTest)
                .streamTopic(mqtt2CloudTopicSizeTest).build();
        getTm2CloudFrequencyTest = TopicMapping.builder().deviceId(DEVICEID).deviceStatusRequired(true)
                .route(Route.TO_CLOUD).serviceId("service-1").serviceName("ro")
                .streamStatusTopic(mqtt2CloudTopicFrequencyTest).streamTopic(mqtt2CloudTopicFrequencyTest).build();

        when(deviceSubscriptionCache.getSubscription(tm2Cloud.getDeviceId()))
                .thenReturn(new DeviceSubscription(VEHICLEID));
        when(topicMapper.getTopicMapping(mqtt2CloudTopic)).thenReturn(tm2Cloud);
        when(topicMapper.getTopicMapping(mqtt2CloudTopicRlTest)).thenReturn(tm2CloudRlTest);
        when(topicMapper.getTopicMapping(mqtt2CloudTopicSizeTest)).thenReturn(tm2CloudSizeTest);
        when(topicMapper.getTopicMapping(mqtt2CloudTopicFrequencyTest)).thenReturn(getTm2CloudFrequencyTest);

        Mockito.when(publishInboundInput.getClientInformation()).thenReturn(clientInformation);
        Mockito.when(publishInboundInput.getConnectionInformation()).thenReturn(connectionInformation);
        Mockito.when(publishInboundInput.getConnectionInformation().getConnectionAttributeStore())
                .thenReturn(connectionAttributeStore);
        Mockito.when(publishInboundInput.getPublishPacket()).thenReturn(publishPacket);

        when(publishInboundInput.getClientInformation().getClientId()).thenReturn(CLIENT_ID_DUMMY);
        when(publishInboundInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of("dummy"));

        when(permission.getTopicFilter()).thenReturn(mqtt2CloudTopic);
        when(permission.getActivity()).thenReturn(TopicPermission.MqttActivity.ALL);
        when(permission.getType()).thenReturn(TopicPermission.PermissionType.ALLOW);
        when(permission.getPublishRetain()).thenReturn(TopicPermission.Retain.ALL);

        List<TopicPermission> permLst = new ArrayList<>();
        permLst.add(permission);
        modifiableDefaultPermissions.add(permission);
        Mockito.when(clientContext.getDefaultPermissions()).thenReturn(modifiableDefaultPermissions);
        Mockito.when(modifiableDefaultPermissions.asList()).thenReturn(permLst);

        msgStoreCallBack.setCompressionUtil(compressionUtil);
        msgStoreCallBack.setTopicFormatter(new IgniteTopicFormatter());

        PowerMockito.mockStatic(Builders.class);
        PowerMockito.when(Builders.topicPermission()).thenReturn(permissionBuilder);
        PowerMockito.when(permissionBuilder.type(Mockito.any())).thenReturn(permissionBuilder);
        PowerMockito.when(permissionBuilder.qos(Mockito.any())).thenReturn(permissionBuilder);
        PowerMockito.when(permissionBuilder.activity(Mockito.any())).thenReturn(permissionBuilder);
        PowerMockito.when(permissionBuilder.build()).thenReturn(permission);

    }

    /*
     * NoWrapping test where actual payload will not serialized.
     */
    @Test
    public void testOnStoreMessageSuccessWithNoWrapping() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        msgStoreCallBack = new MessageStoreCallback();

        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        DeviceSubscription deviceSubscription = new DeviceSubscription(VEHICLEID);
        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("hu");
        deviceSubscription.setDeviceType(deviceType);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription("device-1", deviceSubscription);
        when(compressionUtil.decompress(payload.getBytes())).thenReturn(payload.getBytes());
        msgStoreCallBack.setWrapWithIgniteEventEnabled(false);
        when(publishInboundInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of(""));

        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);

        verify(hivemqSinkService, times(1)).sendMsgToSink(VEHICLEID.getBytes(), payload.getBytes(),
                "haa-harman-dev-ro" /* mqtt2CloudTopic */);
    }

    /**
     * no topics that allows to client id send data without vehicle id are
     * configured in the system, a client which is not mapped to vehicle sends a
     * topic that requires to have vehicle id mapped to it, expect 0 services to be
     * invoked.
     */
    @Test
    public void noVehicleIdWithCommCheckNotConfigured() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        msgStoreCallBack = new MessageStoreCallback();
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        when(deviceSubscriptionCache.getSubscription(tm2Cloud.getDeviceId())).thenReturn(null);
        when(compressionUtil.decompress(payload.getBytes())).thenReturn(payload.getBytes());
        msgStoreCallBack.setWrapWithIgniteEventEnabled(false);
        when(publishInboundInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of(""));
        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(0)).sendMsgToSink(VEHICLEID.getBytes(), payload.getBytes(), mqtt2CloudTopic);
    }

    /**
     * A topic that allows to client id send data without vehicle id is configured
     * in the system, a client which is not mapped to vehicle sends a topic that
     * requires to have vehicle id mapped to it, expect 0 services to be invoked.
     */
    @Test
    public void noVehicleIdSendRoTopicWithCommCheckConfigured() throws IOException {
        PropertyLoader.getProperties().put(PROFILE_CHECK_DISABLED_TOPICS, COMM_CHECK);
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        msgStoreCallBack = new MessageStoreCallback();
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        when(deviceSubscriptionCache.getSubscription(tm2Cloud.getDeviceId())).thenReturn(null);
        when(compressionUtil.decompress(payload.getBytes())).thenReturn(payload.getBytes());
        msgStoreCallBack.setWrapWithIgniteEventEnabled(false);
        when(publishInboundInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of(""));
        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(0)).sendMsgToSink(VEHICLEID.getBytes(), payload.getBytes(), mqtt2CloudTopic);
        PropertyLoader.getProperties().remove(PROFILE_CHECK_DISABLED_TOPICS);
    }


    /**
     * whenever there is commcheck response on subscribed topic, need to wait for
     * ack and publish a generic event back to commcheck.
     * 
     * <p>for suspicious device and swap scenario, commcheck need to initiate a
     * disconnect after this ack
     */
    @Test
    public void deviceReceivedWithCommCheckResponse() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        PowerMockito.mockStatic(TopicMapperFactory.class);
        BDDMockito.given(TopicMapperFactory.getInstance()).willReturn(topicMapper);
        PropertyLoader.getProperties().put(PROFILE_CHECK_DISABLED_TOPICS, COMM_CHECK);
        msgStoreCallBack = new MessageStoreCallback();
        when(compressionUtil.decompress(payload.getBytes())).thenReturn(payload.getBytes());
        msgStoreCallBack.setWrapWithIgniteEventEnabled(false);

        when(permission.getTopicFilter()).thenReturn(mqtt2CloudTopicFrequencyTest);
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(COMM_CHECK_2_DEVICE_TOPIC);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        TopicMapping topicMapping = TopicMapping.builder().deviceId("device-1").deviceStatusRequired(true)
                .route(Route.TO_DEVICE).serviceId("commcheck").serviceName(COMM_CHECK).streamStatusTopic(COMM_CHECK)
                .streamTopic(COMM_CHECK_2_DEVICE_TOPIC).build();
        when(topicMapper.getTopicMapping(COMM_CHECK_2_DEVICE_TOPIC)).thenReturn(topicMapping);
        when(publishInboundInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of("commcheck-service"));
        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(0)).sendMsgToSink(
                publishInboundInput.getClientInformation().getClientId().getBytes(), payload.getBytes(),
                COMM_CHECK_2_DEVICE_TOPIC);

        String userName = "device-1";
        TopicMapping topicMapping1 = TopicMapping.builder().deviceId(DEVICEID).deviceStatusRequired(true)
                .route(Route.TO_CLOUD).serviceId("commcheck").serviceName(COMM_CHECK).streamStatusTopic(COMM_CHECK)
                .streamTopic(MQTT_2_COMM_CHECK_TOPIC).build();
        when(topicMapper.getTopicMapping(MQTT_2_COMM_CHECK_TOPIC)).thenReturn(topicMapping1);
        when(publishInboundInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of(CLIENT_ID_DUMMY));
        when(publishInboundInput.getClientInformation().getClientId()).thenReturn(userName);
        when(pubackInboundInput.getClientInformation()).thenReturn(clientInformation);
        when(pubackInboundInput.getClientInformation().getClientId()).thenReturn(userName);
        when(pubackInboundInput.getPubackPacket()).thenReturn(pubackPacket);
        when(pubackInboundInput.getPubackPacket().getPacketIdentifier()).thenReturn(0);
        new PubackReceived().onInboundPuback(pubackInboundInput, pubackInboundOutput);

        PropertyLoader.getProperties().remove(PROFILE_CHECK_DISABLED_TOPICS);
    }

    /**
     * Test simple flow of publishing data to Kafka with Request Limiter Test with
     * Limited number of requests per minute.
     */
    @Test
    public void testMessagePublishWithLimitedNumberOfRequestsPerMinute() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);

        PowerMockito.mockStatic(TopicMapperFactory.class);
        BDDMockito.given(TopicMapperFactory.getInstance()).willReturn(topicMapper);

        msgStoreCallBack = new MessageStoreCallback();

        getTm2CloudFrequencyTest = TopicMapping.builder().deviceId(DEVICEID).deviceStatusRequired(true)
                .route(Route.TO_CLOUD).serviceId("service-1").serviceName("ro")
                .streamStatusTopic(mqtt2CloudTopicFrequencyTest).streamTopic(mqtt2CloudTopicFrequencyTest).build();
        when(topicMapper.getTopicMapping(mqtt2CloudTopicFrequencyTest)).thenReturn(getTm2CloudFrequencyTest);

        when(permission.getTopicFilter()).thenReturn(mqtt2CloudTopicFrequencyTest);
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopicFrequencyTest);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());

        DeviceSubscription deviceSubscription = new DeviceSubscription(VEHICLEID);
        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("hu");
        deviceSubscription.setDeviceType(deviceType);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription("device-1", deviceSubscription);

        when(compressionUtil.decompress(payload.getBytes())).thenReturn(payload.getBytes());
        msgStoreCallBack.setWrapWithIgniteEventEnabled(false);
        when(publishInboundInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of(""));
        for (int i = 0; i < ONE_THOUSAND; i++) {
            Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                    .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));
            msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
            System.out.println("PublishCounter : " + i + " byteSize : " + payload.getBytes().length);
        }

        verify(hivemqSinkService, times(ONE_THOUSAND)).sendMsgToSink(VEHICLEID.getBytes(), payload.getBytes(),
                mqtt2CloudTopicFrequencyTest);
    }

    /**
     * Test simple flow of publishing data to Kafka with Request Limiter Test with
     * Limited allowed byte size per minute.
     */
    @Test
    public void testMessagePublishWithLimitedBytesPerMinute() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);

        PowerMockito.mockStatic(TopicMapperFactory.class);
        BDDMockito.given(TopicMapperFactory.getInstance()).willReturn(topicMapper);

        msgStoreCallBack = new MessageStoreCallback();

        tm2CloudSizeTest = TopicMapping.builder().deviceId(DEVICEID).deviceStatusRequired(true).route(Route.TO_CLOUD)
                .serviceId("service-1").serviceName("ro").streamStatusTopic(mqtt2CloudTopicSizeTest)
                .streamTopic(mqtt2CloudTopicSizeTest).build();
        when(topicMapper.getTopicMapping(mqtt2CloudTopicSizeTest)).thenReturn(tm2CloudSizeTest);

        when(permission.getTopicFilter()).thenReturn(mqtt2CloudTopicSizeTest);
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopicSizeTest);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());

        DeviceSubscription deviceSubscription = new DeviceSubscription(VEHICLEID);
        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("hu");
        deviceSubscription.setDeviceType(deviceType);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription("device-1", deviceSubscription);

        when(compressionUtil.decompress(payload.getBytes())).thenReturn(payload.getBytes());
        msgStoreCallBack.setWrapWithIgniteEventEnabled(false);
        when(publishInboundInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of(""));
        for (int i = 0; i < ONE_THOUSAND; i++) {
            Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                    .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));
            msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
            System.out.println("PublishCounter : " + i + " byteSize : " + payload.getBytes().length);
        }
        verify(hivemqSinkService, times(ONE_THOUSAND)).sendMsgToSink(VEHICLEID.getBytes(), payload.getBytes(),
                mqtt2CloudTopicSizeTest);
    }

    /**
     * Test simple flow of publishing data to Kafka with Request Limiter Test with
     * Limited allowed byte size and number of requests per minute.
     */
    @Test
    public void testMessagePublishWithLimitedFrequencyAndBytesPerMinute() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);

        PowerMockito.mockStatic(TopicMapperFactory.class);
        BDDMockito.given(TopicMapperFactory.getInstance()).willReturn(topicMapper);

        msgStoreCallBack = new MessageStoreCallback();

        tm2CloudRlTest = TopicMapping.builder().deviceId(DEVICEID).deviceStatusRequired(true).route(Route.TO_CLOUD)
                .serviceId("service-1").serviceName("ro").streamStatusTopic(mqtt2CloudTopicRlTest)
                .streamTopic(mqtt2CloudTopicRlTest).build();
        when(topicMapper.getTopicMapping(mqtt2CloudTopicRlTest)).thenReturn(tm2CloudRlTest);

        when(permission.getTopicFilter()).thenReturn(mqtt2CloudTopicRlTest);

        when(permission.getTopicFilter()).thenReturn(mqtt2CloudTopicRlTest);
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopicRlTest);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());

        DeviceSubscription deviceSubscription = new DeviceSubscription(VEHICLEID);
        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("hu");
        deviceSubscription.setDeviceType(deviceType);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription("device-1", deviceSubscription);

        when(compressionUtil.decompress(payload.getBytes())).thenReturn(payload.getBytes());
        msgStoreCallBack.setWrapWithIgniteEventEnabled(false);
        when(publishInboundInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of(""));
        for (int i = 0; i < ONE_THOUSAND; i++) {
            Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                    .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));
            msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
            System.out.println("PublishCounter : " + i + " byteSize : " + payload.getBytes().length);
        }

        verify(hivemqSinkService, times(ONE_THOUSAND)).sendMsgToSink(VEHICLEID.getBytes(), payload.getBytes(),
                mqtt2CloudTopicRlTest);
    }
    
    /**
     * Test simple flow of publishing data to Kafka with Request Limiter Test with
     * Limited number of requests per minute.
     */
    @Test
    public void testMessagePublishDisabledRequestLimiter() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);

        PowerMockito.mockStatic(TopicMapperFactory.class);
        BDDMockito.given(TopicMapperFactory.getInstance()).willReturn(topicMapper);

        msgStoreCallBack = new MessageStoreCallback();

        getTm2CloudFrequencyTest = TopicMapping.builder().deviceId(DEVICEID).deviceStatusRequired(true)
                .route(Route.TO_CLOUD).serviceId("service-1").serviceName("ro")
                .streamStatusTopic(mqtt2CloudTopicFrequencyTest).streamTopic(mqtt2CloudTopicFrequencyTest).build();
        when(topicMapper.getTopicMapping(mqtt2CloudTopicFrequencyTest)).thenReturn(getTm2CloudFrequencyTest);

        when(permission.getTopicFilter()).thenReturn(mqtt2CloudTopicFrequencyTest);
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopicFrequencyTest);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());

        DeviceSubscription deviceSubscription = new DeviceSubscription(VEHICLEID);
        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("hu");
        deviceSubscription.setDeviceType(deviceType);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription("device-1", deviceSubscription);

        when(compressionUtil.decompress(payload.getBytes())).thenReturn(payload.getBytes());
        msgStoreCallBack.setWrapWithIgniteEventEnabled(false);
        when(publishInboundInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of(""));
        for (int i = 0; i < ONE_THOUSAND; i++) {
            Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                    .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));
            msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
            System.out.println("PublishCounter : " + i + " byteSize : " + payload.getBytes().length);
        }

        verify(hivemqSinkService, times(ONE_THOUSAND)).sendMsgToSink(VEHICLEID.getBytes(), payload.getBytes(),
                mqtt2CloudTopicFrequencyTest);
    }

    /**
     * Topic null test.
     */
    @Test
    public void testOnStoreMessageTopicAsNull() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        msgStoreCallBack = new MessageStoreCallback();
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(null);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        when(compressionUtil.decompress(payload.getBytes())).thenReturn(payload.getBytes());
        msgStoreCallBack.setWrapWithIgniteEventEnabled(false);
        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(0)).sendMsgToSink(VEHICLEID.getBytes(), payload.getBytes(), null);
    }

    /**
     * Wrapping test where actual payload will serialized.
     */
    @Test
    public void testOnStoreMessageSuccessWithWrapping() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);

        PowerMockito.mockStatic(IngestionSerializerFactory.class);
        BDDMockito.given(IngestionSerializerFactory.getInstance()).willReturn(transformer);

        msgStoreCallBack = new MessageStoreCallback();


        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        DeviceSubscription deviceSubscription = new DeviceSubscription(VEHICLEID);
        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("hu");
        deviceSubscription.setDeviceType(deviceType);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription("device-1", deviceSubscription);

        when(transformer.serialize(any(IgniteBlobEvent.class))).thenReturn(payload.getBytes());

        when(compressionUtil.decompress(payload.getBytes())).thenReturn(payload.getBytes());
        msgStoreCallBack.setWrapWithIgniteEventEnabled(true);
        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(1)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
    }

    /**
     * Head unit swap topic test.
     */
    @Test
    public void testOnStoreMessageSuccessForSwap() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);

        PowerMockito.mockStatic(TopicMapperFactory.class);
        BDDMockito.given(TopicMapperFactory.getInstance()).willReturn(topicMapper);
        msgStoreCallBack = new MessageStoreCallback();

        DeviceSubscription deviceSubscription = new DeviceSubscription(VEHICLEID);
        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("hu");
        deviceSubscription.setDeviceType(deviceType);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription("device-1", deviceSubscription);

        PropertyLoader.getProperties().put(AuthConstants.DISCONNECT_TOPIC_NAME, "disconnect");
        String clientId = "hu-swap-test-client";
        
        ClientService cs = Mockito.mock(ClientService.class);
        HivemqServiceProvider.setBlockingClientService(cs);
        CompletableFuture<Boolean> isDisconnected = CompletableFuture.completedFuture(true);
        when(cs.disconnectClient(clientId)).thenReturn(isDisconnected);
        when(publishInboundInput.getClientInformation().getClientId()).thenReturn(clientId);
        String expectedTopic = "haa/harman/dev/" + clientId + "/2d/disconnect";
        when(permission.getTopicFilter()).thenReturn(expectedTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(expectedTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));
        TopicMapping tm = TopicMapping.builder().deviceId("device1").deviceStatusRequired(true).route(Route.TO_DEVICE)
                .serviceId("service-1").serviceName("ro").streamStatusTopic(mqqt2DeviceTopic)
                .streamTopic(mqqt2DeviceTopic).build();
        when(topicMapper.getTopicMapping(expectedTopic)).thenReturn(tm);

        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(0)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
        PropertyLoader.getProperties().remove(AuthConstants.DISCONNECT_TOPIC_NAME);
        HivemqServiceProvider.setBlockingClientService(null);
    }

    /**
     * decompression failed test.
     */
    @Test
    public void testOnStoreMessageSuccessWithDecompressionFailure() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        msgStoreCallBack = new MessageStoreCallback();
        msgStoreCallBack.setCompressionUtil(compressionUtil);
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        when(compressionUtil.decompress(payload.getBytes())).thenThrow(new IOException());
        msgStoreCallBack.setWrapWithIgniteEventEnabled(true);
        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(0)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
    }

    /**
     * decompression disbaled.
     */
    @Test
    public void testOnStoreMessageSuccessWithDecompressionDisabled() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);

        msgStoreCallBack = new MessageStoreCallback();


        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        DeviceSubscription deviceSubscription = new DeviceSubscription(VEHICLEID);
        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("hu");
        deviceSubscription.setDeviceType(deviceType);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription("device-1", deviceSubscription);

        msgStoreCallBack.setWrapWithIgniteEventEnabled(false);
        msgStoreCallBack.setDecompressionEnabled(false);
        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(1)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
    }

    /**
     * Pdid test from a mqtt topic.
     */
    @Test
    public void testOnStoreMessagePdid() {
        String pdid = MessageStoreCallback.getPdid(mqtt2CloudTopic, TOPIC_PREFIX, "/2c/" + SERVICEID);
        assertEquals(DEVICEID, pdid);
    }

    /**
     * In case payload is empty.
     */
    @Test
    public void testOnStoreMessagePayloadEmpty() {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        msgStoreCallBack = new MessageStoreCallback();
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload()).thenReturn(Optional.empty());

        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(0)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
    }

    /**
     * Logging for a topic and increment by one if it used earlier.
     */
    @Test
    public void testOnStoreMessageLoggingPerPdidEnable()
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        msgStoreCallBack = new MessageStoreCallback();
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        msgStoreCallBack.setLogPerPdid(true);
        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        CumulativeLogger logger = CumulativeLogger.getLogger();

        Field stateField = logger.getClass().getDeclaredField("STATE");
        if (stateField == null) {
            return;
        }
        stateField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, AtomicLong> map = (Map<String, AtomicLong>) stateField.get(logger);
        assertTrue(map.get(mqtt2CloudTopic).get() > 0);
    }

    /**
     * Device message should not come through here.
     */
    @Test
    public void testOnStoreMessage2Device() {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        msgStoreCallBack = new MessageStoreCallback();
        when(permission.getTopicFilter()).thenReturn(mqqt2DeviceTopic);

        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqqt2DeviceTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        TopicMapping tm = TopicMapping.builder().deviceId("").deviceStatusRequired(true).route(Route.TO_DEVICE)
                .serviceId("service-1").serviceName("ro").streamStatusTopic(mqqt2DeviceTopic)
                .streamTopic(mqqt2DeviceTopic).build();
        when(topicMapper.getTopicMapping(mqqt2DeviceTopic)).thenReturn(tm);
        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(0)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
    }

    /**
     * If device id is empty test.
     *
     */
    @Test
    public void testOnStoreMessageDeviceIdEmpty() {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);

        PowerMockito.mockStatic(TopicMapperFactory.class);
        BDDMockito.given(TopicMapperFactory.getInstance()).willReturn(topicMapper);
        msgStoreCallBack = new MessageStoreCallback();


        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        TopicMapping tm = TopicMapping.builder().deviceId("").deviceStatusRequired(true).route(Route.TO_CLOUD)
                .serviceId("service-1").serviceName("ro").streamStatusTopic("").streamTopic("").build();
        when(topicMapper.getTopicMapping(mqtt2CloudTopic)).thenReturn(tm);
        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(topicMapper, times(1)).getTopicMapping(mqtt2CloudTopic);
        verify(hivemqSinkService, times(0)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
    }

    /**
     * Device id null test.
     */
    @Test
    public void testOnStoreMessageDeviceIdNull() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);

        PowerMockito.mockStatic(TopicMapperFactory.class);
        BDDMockito.given(TopicMapperFactory.getInstance()).willReturn(topicMapper);

        msgStoreCallBack = new MessageStoreCallback();

        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        TopicMapping tm = TopicMapping.builder().deviceId(null).deviceStatusRequired(true).route(Route.TO_CLOUD)
                .serviceId("service-1").serviceName("ro").streamStatusTopic("").streamTopic("").build();
        when(topicMapper.getTopicMapping(mqtt2CloudTopic)).thenReturn(tm);
        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(topicMapper, times(1)).getTopicMapping(mqtt2CloudTopic);
        verify(hivemqSinkService, times(0)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
    }

    /**
     * Service id null test.
     */
    @Test
    public void testOnStoreMessageServiceIdNull() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        PowerMockito.mockStatic(TopicMapperFactory.class);
        BDDMockito.given(TopicMapperFactory.getInstance()).willReturn(topicMapper);
        msgStoreCallBack = new MessageStoreCallback();
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        TopicMapping tm = TopicMapping.builder().deviceId(DEVICEID).deviceStatusRequired(true).route(Route.TO_CLOUD)
                .serviceId(null).serviceName("ro").streamStatusTopic("").streamTopic("").build();
        when(topicMapper.getTopicMapping(mqtt2CloudTopic)).thenReturn(tm);
        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(0)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
    }

    /**
     * service id empty test.
     */
    @Test
    public void testOnStoreMessageServiceIdEmpty() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        PowerMockito.mockStatic(TopicMapperFactory.class);
        BDDMockito.given(TopicMapperFactory.getInstance()).willReturn(topicMapper);
        msgStoreCallBack = new MessageStoreCallback();
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        TopicMapping tm = TopicMapping.builder().deviceId(DEVICEID).deviceStatusRequired(true).route(Route.TO_CLOUD)
                .serviceId("").serviceName("ro").streamStatusTopic("").streamTopic("").build();
        when(topicMapper.getTopicMapping(anyString())).thenReturn(tm);
        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(0)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
    }

    /**
     * Vehicle id empty test.
     */
    @Test
    public void testOnStoreMessageVehicleEmpty() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        PropertyLoader.getProperties().put(AuthConstants.PROFILE_CHECK_DISABLED_TOPICS, "commchk");
        msgStoreCallBack = new MessageStoreCallback();
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        DeviceSubscription deviceSubscription = new DeviceSubscription("");
        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("");
        deviceSubscription.setDeviceType(deviceType);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription(DEVICEID, deviceSubscription);

        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(0)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
        DeviceSubscriptionCacheFactory.getInstance().removeSubscription(DEVICEID);
        PropertyLoader.getProperties().remove(AuthConstants.PROFILE_CHECK_DISABLED_TOPICS);
    }

    /**
     * Health check user test.
     */
    @Test
    public void testOnStoreMessageForHealthCheckUserFailed() throws IOException {
        msgStoreCallBack = new MessageStoreCallback();
        when(publishInboundInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of("health"));
        Listener listener = Mockito.mock(Listener.class);
        when(listener.getPort()).thenReturn(PORT);
        when(publishInboundInput.getConnectionInformation().getListener()).thenReturn(Optional.of(listener));
        msgStoreCallBack.onReceivedHealthCheckMessage("health", publishInboundOutput);
    }

    /**
     * Health check user test.
     */
    @Test
    public void testOnStoreMessageForHealthCheckUserSuccess() throws IOException {
        msgStoreCallBack = new MessageStoreCallback();
        when(publishInboundInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of("health"));
        Listener listener = Mockito.mock(Listener.class);
        when(listener.getPort()).thenReturn(PORT);
        when(publishInboundInput.getConnectionInformation().getListener()).thenReturn(Optional.of(listener));
        HealthService.getInstance().setHealthy(true);
        msgStoreCallBack.onReceivedHealthCheckMessage("health", publishInboundOutput);
        assertTrue(HealthService.getInstance().isHealthy());
        HealthService.getInstance().setHealthy(false);
    }

    /**
     * Health check user test.
     */
    @Test
    public void testOnStoreMessageForKeepAlive() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        msgStoreCallBack = new MessageStoreCallback();
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        when(keepAliveHandler.doProcessKeepAliveMsg(any(Properties.class), anyString(), anyString(), anyString()))
                .thenReturn(true);
        when(compressionUtil.decompress(payload.getBytes())).thenReturn(payload.getBytes());
        msgStoreCallBack.setWrapWithIgniteEventEnabled(false);
        when(publishInboundInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of(""));
        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
    }

    /**
     * Log per pdid test. it is incremented or not.
     */
    @Test
    public void testOnStoreMessageLoggingperpid() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);

        msgStoreCallBack = new MessageStoreCallback();

        DeviceSubscription deviceSubscription = new DeviceSubscription(VEHICLEID);
        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("hu");
        deviceSubscription.setDeviceType(deviceType);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription("device-1", deviceSubscription);

        msgStoreCallBack.setLogPerPdid(true);
        msgStoreCallBack.setWrapWithIgniteEventEnabled(false);
        when(compressionUtil.decompress(payload.getBytes())).thenReturn(payload.getBytes());
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(1)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());

    }

    /**
     * Test case for verifying the stream status topic mapping.
     * 
     * <p>This test case checks the behavior of the stream status topic mapping in two scenarios:
     * 1. When the key is present in the map.
     * 2. When the key is not present in the map.
     * 
     * <p>For the first scenario, it creates a TopicMapping object with a specific deviceId, serviceId, streamTopic,
     * route, deviceStatusRequired, and serviceName. It then asserts that the stream status topic returned by
     * the getStreamStatusTopic() method matches the expected value.
     * 
     * <p>For the second scenario, it modifies the TopicMapping object by setting a different serviceName and
     * streamStatusTopic. It then asserts that the stream status topic returned by the getStreamStatusTopic()
     * method matches the streamStatusTopic value set in the TopicMapping object.
     */
    @Test
    public void testTopicMappingStreamStatusTopic() {
        // Case 1: Stream Status Topic on key present in map
        TopicMapping mapping = TopicMapping.builder().deviceId("device123").serviceId("service123").streamTopic("")
                .route(TopicMapping.Route.TO_CLOUD).deviceStatusRequired(false).serviceName("ada/ftd").build();
        Assert.assertEquals("device-status-ada-control-sp", mapping.getStreamStatusTopic());

        // Case 2: Stream Status Topic on key not present in map
        mapping = TopicMapping.builder().deviceId("device123").serviceId("service123").streamTopic("")
                .route(TopicMapping.Route.TO_CLOUD).deviceStatusRequired(false).serviceName("dvp")
                .streamStatusTopic(COMM_CHECK).build();
        Assert.assertEquals(COMM_CHECK, mapping.getStreamStatusTopic());
    }

    /**
     * Test case to verify the behavior when MQTT service ID mapping is not found.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testMqttServiceIdMappingNotFound() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        msgStoreCallBack = new MessageStoreCallback();
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn("haa/harman/dev/dummy/topic");
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        TopicMapping tm = TopicMapping.builder().deviceId("").deviceStatusRequired(true).route(Route.TO_CLOUD)
                .serviceId("service-1").serviceName("ro").streamStatusTopic("").streamTopic("").build();
        when(topicMapper.getTopicMapping(mqtt2CloudTopic)).thenReturn(tm);
        MessageStoreCallback messageStoreCallback = new MessageStoreCallback();
        messageStoreCallback.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(topicMapper, times(0)).getTopicMapping(anyString());
        verify(hivemqSinkService, times(0)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
    }

    /**
     * Test case to verify the MQTT topic mapping for a global topic.
     *
     * @throws IOException if an I/O error occurs.
     * @throws NoSuchFieldException if a field is not found.
     * @throws SecurityException if a security violation occurs.
     */
    @Test
    public void testMqttTopicMappingGlobalTopic() throws IOException, NoSuchFieldException, SecurityException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn("haa/harman/dev/global/topic");
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        TopicMapping tm = TopicMapping.builder().deviceId("").deviceStatusRequired(true).route(Route.TO_CLOUD)
                .serviceId("service-1").serviceName("ro").streamStatusTopic("").streamTopic("").build();
        when(topicMapper.getTopicMapping(mqtt2CloudTopic)).thenReturn(tm);
        MessageStoreCallback messageStoreCallback = new MessageStoreCallback();
        List<String> globalTopics = new ArrayList<>();
        globalTopics.add("haa/harman/dev/global/topic");
        messageStoreCallback.setGlobalSubTopics(globalTopics);
        messageStoreCallback.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(topicMapper, times(0)).getTopicMapping(anyString());
        verify(hivemqSinkService, times(0)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
    }

    /**
     * Test case to verify the behavior when the MQTT service ID mapping is not proper.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testMqttServiceIdMappingNotProper() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());

        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn("dummy");
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        TopicMapping tm = TopicMapping.builder().deviceId("").deviceStatusRequired(true).serviceId("service-1")
                .serviceName("ro").streamStatusTopic("").streamTopic("").build();
        when(topicMapper.getTopicMapping(mqtt2CloudTopic)).thenReturn(tm);
        MessageStoreCallback messageStoreCallback = new MessageStoreCallback();
        messageStoreCallback.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(topicMapper, times(0)).getTopicMapping(anyString());
        verify(hivemqSinkService, times(0)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
    }

    /**
     * Test case for the MQTT keep-alive topic.
     *
     * @throws IOException if an I/O error occurs
     */
    @Test
    public void testMqttKeepAliveTopic() throws IOException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PropertyLoader.getProperties().put(AuthConstants.KEEP_ALIVE_TOPIC_NAME, "ro");
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn("haa/harman/dev/dummy/2c/ro");
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        TopicMapping tm = TopicMapping.builder().deviceId("").deviceStatusRequired(true).route(Route.TO_CLOUD)
                .serviceId("service-1").serviceName("ro").streamStatusTopic("").streamTopic("").build();

        when(topicMapper.getTopicMapping(mqtt2CloudTopic)).thenReturn(tm);
        MessageStoreCallback messageStoreCallback = new MessageStoreCallback();
        messageStoreCallback.setTopicFormatter(new IgniteTopicFormatter());
        messageStoreCallback.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(topicMapper, times(0)).getTopicMapping(anyString());
        verify(hivemqSinkService, times(0)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
        PropertyLoader.getProperties().remove(AuthConstants.KEEP_ALIVE_TOPIC_NAME);
    }

    /**
     * Test case to verify the behavior when MQTT topic mapping is null.
     *
     * @throws IOException              if an I/O error occurs.
     * @throws NoSuchFieldException     if a field is not found.
     * @throws SecurityException        if a security violation occurs.
     * @throws IllegalArgumentException if an illegal argument is passed.
     * @throws IllegalAccessException if an illegal access to a field or method occurs.
     */
    @Test
    public void testMqttTopicMappingIsNull() throws IOException, NoSuchFieldException, SecurityException,
            IllegalArgumentException, IllegalAccessException {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);

        PowerMockito.mockStatic(TopicMapperFactory.class);
        BDDMockito.given(TopicMapperFactory.getInstance()).willReturn(topicMapper);
        msgStoreCallBack = new MessageStoreCallback();


        DeviceSubscription deviceSubscription = new DeviceSubscription(VEHICLEID);
        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("hu");
        deviceSubscription.setDeviceType(deviceType);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription("device-1", deviceSubscription);

        PropertyLoader.getProperties().put(AuthConstants.DISCONNECT_TOPIC_NAME, "disconnect");
        String clientId = "hu-swap-test-client";
        
        ClientService cs = Mockito.mock(ClientService.class);
        HivemqServiceProvider.setBlockingClientService(cs);
        CompletableFuture<Boolean> isDisconnected = CompletableFuture.completedFuture(true);
        when(cs.disconnectClient(clientId)).thenReturn(isDisconnected);
        when(publishInboundInput.getClientInformation().getClientId()).thenReturn(clientId);

        TopicMapping tm = TopicMapping.builder().deviceId("").deviceStatusRequired(true).route(Route.TO_DEVICE)
                .serviceId("service-1").serviceName("ro").streamStatusTopic("").streamTopic("").build();
        String expectedTopic = "haa/harman/dev/" + clientId + "/2d/disconnect";
        when(topicMapper.getTopicMapping(mqtt2CloudTopic)).thenReturn(tm);
        when(permission.getTopicFilter()).thenReturn(expectedTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(expectedTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(0)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
        PropertyLoader.getProperties().remove(AuthConstants.DISCONNECT_TOPIC_NAME);
        HivemqServiceProvider.setBlockingClientService(null);
    }

    /**
     * Test case to verify the MQTT topic mapping to route device.
     */
    @Test
    public void testMqttTopicMappingToRouteDevice() {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);


        PowerMockito.mockStatic(TopicMapperFactory.class);
        BDDMockito.given(TopicMapperFactory.getInstance()).willReturn(topicMapper);
        msgStoreCallBack = new MessageStoreCallback();

        DeviceSubscription deviceSubscription = new DeviceSubscription(VEHICLEID);
        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("hu");
        deviceSubscription.setDeviceType(deviceType);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription("device-1", deviceSubscription);

        PropertyLoader.getProperties().put(AuthConstants.DISCONNECT_TOPIC_NAME, "disconnect");
        String clientId = "hu-swap-test-client";
        
        ClientService cs = Mockito.mock(ClientService.class);
        HivemqServiceProvider.setBlockingClientService(cs);
        CompletableFuture<Boolean> isDisconnected = CompletableFuture.completedFuture(true);
        when(cs.disconnectClient(clientId)).thenReturn(isDisconnected);
        when(publishInboundInput.getClientInformation().getClientId()).thenReturn(clientId);

        TopicMapping tm = TopicMapping.builder().deviceId("").deviceStatusRequired(true).route(Route.TO_DEVICE)
                .serviceId("service-1").serviceName("ro").streamStatusTopic("").streamTopic("").build();
        String expectedTopic = "haa/harman/dev/" + clientId + "/2d/disconnect";
        when(topicMapper.getTopicMapping(expectedTopic)).thenReturn(tm);
        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(expectedTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));

        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(0)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());
        PropertyLoader.getProperties().remove(AuthConstants.DISCONNECT_TOPIC_NAME);
        HivemqServiceProvider.setBlockingClientService(null);
    }

    /**
     * This method tests the behavior of the MessageStoreCallback when MQTT QoS level change is enabled.
     * It sets up the necessary mock objects and properties, and then invokes the doPublishReceived method
     * of the MessageStoreCallback class with different input scenarios. Finally, it verifies that the
     * expected methods of the HivemqSinkService class are called.
     */
    @Test
    public void testMqttQosChangeEnabled() {
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);

        PropertyLoader.getProperties().put(AuthConstants.QOS_LEVEL_2C_VALUE, "AT_LEAST_ONCE");
        PropertyLoader.getProperties().put(AuthConstants.QOS_LEVEL_2D_VALUE, "EXACTLY_ONCE");
        PropertyLoader.getProperties().put(AuthConstants.QOS_LEVEL_ENABLED, "true");
        msgStoreCallBack = new MessageStoreCallback();

        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));
        Mockito.when(publishInboundOutput.getPublishPacket()).thenReturn(Mockito.mock(ModifiablePublishPacket.class));

        DeviceSubscription deviceSubscription = new DeviceSubscription(VEHICLEID);
        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("hu");
        deviceSubscription.setDeviceType(deviceType);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription("device-1", deviceSubscription);
        msgStoreCallBack.setWrapWithIgniteEventEnabled(false);

        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(1)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());

        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqqt2DeviceTopic);
        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);
        verify(hivemqSinkService, times(1)).sendMsgToSink(any(byte[].class), any(byte[].class), anyString());

        PropertyLoader.getProperties().remove(AuthConstants.QOS_LEVEL_2C_VALUE);
        PropertyLoader.getProperties().remove(AuthConstants.QOS_LEVEL_2D_VALUE);
        PropertyLoader.getProperties().remove(AuthConstants.QOS_LEVEL_ENABLED);
    }

    /**
     * Test case to validate the topic correctness.
     */
    @Test
    public void testValidateTopicCorrect() {
        assertTrue(
                msgStoreCallBack.validateTopic("haa/harman/dev/haa_api/2c/events", "haa/harman/dev/haa_api/2c/events"));
    }

    /**
     * Test case to validate the behavior of the validateTopic method when the topic is incorrect.
     * It asserts that the validateTopic method returns false when given two different topics.
     */
    @Test
    public void testValidateTopicIncorrect() {
        assertFalse(
                msgStoreCallBack.validateTopic("haa/harman/dev/haa_api/2c/events", "haa/harman/dev/haa_api/2c/alerts"));
    }
    

    /**
     * This method tests the behavior of the system when a suspicious device is encountered.
     * It sets up the necessary mock objects and verifies that the expected actions are performed.
     *
     * @throws Exception if an error occurs during the test
     */
    @Test
    public void testSuspiciousDevice() throws Exception {
        PropertyLoader.getProperties().put(AuthConstants.PROFILE_CHECK_DISABLED_TOPICS, "commchk");
        PowerMockito.mockStatic(Services.class);
        PowerMockito.when(Services.metricRegistry()).thenReturn(new MetricRegistry());
        PowerMockito.mockStatic(HivemqSinkService.class);
        BDDMockito.given(HivemqSinkService.getInstance()).willReturn(hivemqSinkService);
        msgStoreCallBack = new MessageStoreCallback();

        Mockito.when(publishInboundInput.getPublishPacket().getTopic()).thenReturn(mqtt2CloudTopic);
        Mockito.when(publishInboundInput.getPublishPacket().getQos()).thenReturn(Qos.AT_LEAST_ONCE);
        Mockito.when(publishInboundInput.getPublishPacket().getTimestamp()).thenReturn(System.currentTimeMillis());
        Mockito.when(publishInboundInput.getPublishPacket().getPayload())
                .thenReturn(Optional.of(ByteBuffer.wrap(payload.getBytes())));
        when(publishInboundInput.getClientInformation().getClientId()).thenReturn(CLIENT_ID_DUMMY);

        java.util.Optional<String> deviceType = java.util.Optional.ofNullable("hu");
        DeviceSubscription deviceSubscription = new DeviceSubscription(VEHICLEID);
        deviceSubscription.setDeviceType(deviceType);
        deviceSubscription.setSuspicious(true);
        DeviceSubscriptionCacheFactory.getInstance().addSubscription(DEVICEID, deviceSubscription);
        when(compressionUtil.decompress(payload.getBytes())).thenReturn(payload.getBytes());
        msgStoreCallBack.setWrapWithIgniteEventEnabled(false);

        when(publishInboundInput.getConnectionInformation().getConnectionAttributeStore()
                .getAsString(AuthConstants.USERNAME)).thenReturn(Optional.of(""));

        msgStoreCallBack.doPublishReceived(publishInboundInput, publishInboundOutput);

        verify(hivemqSinkService, times(0)).sendMsgToSink(VEHICLEID.getBytes(), payload.getBytes(),
                "haa-harman-dev-ro" /* mqtt2CloudTopic */);
        DeviceSubscriptionCacheFactory.getInstance().removeSubscription(DEVICEID);
        PropertyLoader.getProperties().remove(AuthConstants.PROFILE_CHECK_DISABLED_TOPICS);
    }

    /**
     * This method gets called for cleaning up after test run.
     */
    @After
    public void cleanup() {
        publishInboundInput = null;
        deviceSubscriptionCacheFactory = null;
        hivemqSinkService = null;
        DeviceSubscriptionCacheFactory.getInstance().removeSubscription(DEVICEID);
    }
}