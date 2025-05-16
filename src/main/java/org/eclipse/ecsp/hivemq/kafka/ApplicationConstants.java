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

package org.eclipse.ecsp.hivemq.kafka;

/**
 * This final class contains all constants used in application.
 */
public final class ApplicationConstants {
    
    private ApplicationConstants() {
    }

    public static final String TOPIC_FORMATTER_CLASS = "mqtt.topicFormatter";
    public static final String MQTT_TOPIC_PREFIX = "mqtt.topic.prefix";
    public static final String MQTT_TOPIC_INFIX_2C = "mqtt.topic.infix.2c";
    public static final String MQTT_TOPIC_INFIX_2D = "mqtt.topic.infix.2d";
    public static final String MQTT_TOPIC_EVENTS_SUFFIX = "mqtt.topic.generalevents.suffix";
    public static final String MQTT_TOPIC_ALERTS_SUFFIX = "mqtt.topic.alerts.suffix";

    public static final String KAFKA_BROKER_URL = "kafka.broker.url";
    public static final String KAFKA_AKS = "kafka.acks";
    public static final String KAFKA_LINGER_MS = "kafka.linger.ms";
    public static final String KAFKA_NUM_OF_RETRIES = "kafka.num.put.retries";
    public static final String KAFKA_KEY_SERIALIZER = "kafka.key.serializer";
    public static final String KAFKA_VALUE_SERIALIZER = "kafka.value.serializer";
    public static final String KAFKA_PARTITIONER = "kafka.partitioner";
    public static final String KAFKA_REPLACE_CLASSLOADER = "kafka.replace.classloader";
    public static final String KAFKA_NO_PUTS = "kafka.no.puts";
    public static final String KAFKA_REGISTER_ASYNC_PUTS = "kafka.register.sync.puts";
    public static final String KAFKA_LOG_FREQUENCY = "kafka.log.frequency";
    public static final String KAFKA_RECORD_KEY_START_POSITION = "kafka.record.key.start.pos";
    public static final String KAFKA_RECORD_KEY_END_POSITION = "kafka.record.key.end.pos";
    public static final String KAFKA_REQUEST_TIME_OUT_MS = "kafka.request.timeout.ms";
    public static final String KAFKA_COMPRESSION_TYPE = "kafka.compression.type";
    public static final String DECOMPRESS_ENABLED = "mqtt.decompress.enabled";
    public static final String WRAP_WITH_IGNITE_EVENT_ENABLED = "wrap.with.ignite.event.enabled";
    public static final String KAFKA_BATCH_SIZE = "kafka.batch.size";
    public static final String KAFKA_MAX_BLOCK_MS = "kafka.max.block.ms";
    public static final String KAFKA_MAX_INFLIGHT_REQUEST_PER_CONN = "kafka.max.in.flight.requests.per.connection";

    public static final String KAFKA_KEY_ACKS = "acks";
    public static final String KAFKA_KEY_LINGER_MS = "linger.ms";
    public static final String KAFKA_KEY_RETRIES = "retries";
    public static final String KAFKA_KEY_KEY_SERIALIZER = "key.serializer";
    public static final String KAFKA_KEY_VALUE_SERIALIZER = "value.serializer";

    public static final String KAFKA_KEY_REQUEST_TIME_OUT_MS = "request.timeout.ms";
    public static final String KAFKA_KEY_COMPRESSION_TYPE = "compression.type";
    public static final String KAFKA_KEY_BATCH_SIZE = "batch.size";
    public static final String KAFKA_KEY_MAX_BLOCK_MS = "max.block.ms";
    public static final String KAFKA_KEY_MAX_INFLIGHT_REQUEST_PER_CONN = "max.in.flight.requests.per.connection";

    public static final String BLANK = "";

    // Error codes
    public static final Integer SUCCESS = 200;
    public static final Integer FAILURE = 503;

    public static final String KAFKA_SINK_TOPIC_EVENTS = "kafka.sink.topic.events";
    public static final String KAFKA_SINK_TOPIC_ALERTS = "kafka.sink.topic.alerts";

    public static final String KINESIS_SINK_TOPIC_EVENTS = "kinesis.sink.topic.events";
    public static final String KINESIS_SINK_TOPIC_ALERTS = "kinesis.sink.topic.alerts";

    // Plugin configuration
    public static final String HIVEMQ_PLUGIN_CLIENT_LIFECYCLE_IMPL_CLASS = "client.lifecycle.impl.class";
    public static final String HIVEMQ_PLUGIN_AUTHENTICATION_IMPL_CLASS = "authentication.impl.class";
    public static final String HIVEMQ_PLUGIN_AUTHORIZATION_IMPL_CLASS = "authorization.impl.class";
    public static final String HIVEMQ_PLUGIN_TOPIC_MAPPER_IMPL_CLASS = "topic.mapper.impl.class";
    public static final String HIVEMQ_SINK_NODE_IMPL_CLASS = "hivemq.sink.impl.class";
    public static final String SUBSCRIPTION_CACHE_IMPL_CLASS = "subscription.cache.impl.class";
    public static final String VEHICLE_PROFILE_DATA_EXTRACTER_CLASS = "vehicle.profile.data.extracter.impl.class";

    public static final String IGNITE_EVENT_SERIALIZER_CLASS = "ingestion.serializer.impl";
    public static final String IGNITE_DEVICE_TO_VEHICLE_MAPPER_CLASS = "device.to.vehicle.mapper.impl";

    // SDP Internal Topic
    public static final String KAFKA_SINK_TOPIC_CONNECT = "kafka.sink.topic.connect";
    public static final String KAFKA_SINK_TOPIC_DISCONNECT = "kafka.sink.topic.disconnect";

    public static final String KINESIS_SINK_TOPIC_CONNECT = "kinesis.sink.topic.connect";
    public static final String KINESIS_SINK_TOPIC_DISCONNECT = "kinesis.sink.topic.disconnect";

    public static final String ALLOWED_BLOB_SOURCES = "allowed.blob.sources";
    public static final String ALLOWED_BLOB_ENCODINGS = "allowed.blob.encodings";

    public static final String DELIMITER = ",";

    public static final String EVENT_ID_BLOB = "Blob";
    public static final String MQTT_DELIMITER = "/";
    public static final String PREFIX = "transform";
    public static final String DOT = ".";
    public static final String STATUS_PREFIX = "device-status-";

    // for expiry cache
    public static final String EXPIRY_CACHE_IMPL_CLASS = "expiry.cache.impl.class";
    public static final String EXPIRY_CACHE_ENABLED = "expiry.cache.enabled";

    public static final String PREVENT_LWT_MESSAGE = "expiry.token.prevent.lwt.message";
    
    public static final String DISCONNECT_ONPING_IF_NOT_CONNECTED_ENABLED 
        = "disconnect.on.ping.if.not.connected.enabled";

    public static final String REDIS_ENABLED = "redis.enabled";

    // DMPortal constants
    public static final String DMPORTAL_TOPIC_PERMISSION_CLASS = "dmportal.subs.topic.permissions.impl";

    public static final String DEVICEAWARE_ENABLED = "device.aware.enabled";
    public static final String DEVICEAWARE_ENABLED_SERVICES = "device.aware.enabled.services";

    // for publish/subscribe timeout
    public static final String PUBLISH_INBOUND_TIMEOUT = "publish.inbound.timeout";
    public static final String SUBSCRIBE_INBOUND_TIMEOUT = "subscribe.inbound.timeout";
    public static final String HIVEMQ_PLUGIN_CONFIG_CLASS = "plugin.config.class";
    public static final String HIVEMQ_SERVICE_CLIENT_OVERLOADPROTECTION_DISABLED 
        = "service.client.overloadprotection.disabled";

    public static final String SUBSCRIBE_JMX_METRICS = "org.eclipse.ecsp.hivemq.extension.subscriber.timer";
    public static final String PUBLISH_SUBSCRIBE_ASYNC_TIMEOUT = "publish.subscribe.async.timeout";
    public static final String PUBLISH_DROPPED_TIMEOUT_COUNT_V2C_JMX_METRICS 
        = "org.eclipse.ecsp.hivemq.extension.publish.dropped.timeout.v2c.count";
    public static final String PUBLISH_DROPPED_TIMEOUT_COUNT_C2V_JMX_METRICS 
        = "org.eclipse.ecsp.hivemq.extension.publish.dropped.timeout.c2v.count";

    public static final String DISCONNECT_THREAD = "disconnect.thread";
    public static final String THREAD_DELAY = "disconnect.thread.delay";
    public static final String REPOPULATE_DEFAULT_PERMISSION = "repopulate.default.permission";

    public static final String SSDP_SIMULATOR = "ssdp.simulator";
    public static final String KAFKA_SINK_TOPIC_PRESENCE_MANAGER = "kafka.sink.topic.presence.manager";
    public static final String DEVICE_PLATFORM = "device.platform";
    
    public static final String MQTT_START_TIMER_JMX 
        = "com.hivemq.extensions.harman.ignite.clientlifecycleevents.onmqttconnectionstart.timer";
    public static final String DISCONNECT_TIMER_JMX 
        = "com.hivemq.extensions.harman.ignite.clientlifecycleevents.ondisconnect.timer";
    public static final String ONCONNECT_TIMER_JMX 
        = "com.hivemq.extensions.harman.ignite.abstractauthentication.onconnect.timer";
    public static final String PUBLISH_TIMER_JMX 
        = "com.hivemq.extensions.harman.ignite.abstractonpublishreceivedcallback.oninboundpublish.async.timer";
    public static final String DOPUBLISH_RECEIVED_TIMER_JMX 
        = "com.hivemq.extensions.harman.ignite.abstractonpublishreceivedcallback.oninboundpublish"
                + ".dopublishreceived.timer";
    public static final String KAFKA_PUT_TIMER_JMX 
        = "com.hivemq.extensions.harman.ignite.kafkasinknode.put.method.timer";
    public static final String KAFKA_PUT_COUNTER_JMX = "com.hivemq.extensions.harman.ignite.kafkasinknode.put.count";
    public static final String KAFKA_PUT_CALLBACK_TIMER_JMX 
        = "com.hivemq.extensions.harman.ignite.kafkasinknode.put.callback.timer";
    public static final String AUTHORIZATION_TIMER_JMX 
        = "com.hivemq.extensions.harman.ignite.abstractauthorization.initialize.timer";
    public static final String AUTHORIZATION_DEFAULT_PERMISSION_TIMER_JMX 
        = "com.hivemq.extensions.harman.ignite.abstractauthorization.initialize.authorize.timer";
    public static final String HIVEMQ_PATCH_VERSION_JMX 
        = "com.hivemq.extensions.harman.ignite.pluginmainclass.extensionstart.patchversion.current";
    public static final String GET_VEHICLEID_COUNTER_JMX = "com.hivemq.extensions.harman.ignite.getvehicleid.count";
    public static final String GET_VEHICLEID_TIMER_JMX = "com.hivemq.extensions.harman.ignite.getvehicleid.timer";
    public static final String GET_VEHICLEDATA_COUNTER_JMX = "com.hivemq.extensions.harman.ignite.getvehicledata.count";
    public static final String GET_VEHICLEDATA_TIMER_JMX = "com.hivemq.extensions.harman.ignite.getvehicledata.timer";
    public static final String GET_VEHICLEDATA_EXCEPTION_COUNT_JMX 
        = "com.hivemq.extensions.harman.ignite.getvehicledata.exception.count";
    
    public static final String PUBLISH_INTERCEPTOR = "publish.interceptor.class";
}
