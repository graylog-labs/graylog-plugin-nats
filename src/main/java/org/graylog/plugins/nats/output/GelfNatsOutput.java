/**
 * Graylog NATS Plugin - NATS plugin for Graylog
 * Copyright © 2016 Graylog, Inc. (hello@graylog.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.nats.output;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.assistedinject.Assisted;
import io.nats.client.Connection;
import io.nats.client.ConnectionFactory;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.requireNonNull;

public class GelfNatsOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(GelfNatsOutput.class);

    private static final String CK_NATS_URI = "nats_uri";
    private static final String CK_CHANNEL = "channel";

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Connection connection;
    private final String channel;

    private final String nodeId;
    private final String clusterId;

    @Inject
    public GelfNatsOutput(@Assisted Configuration configuration, ServerStatus serverStatus) throws MessageOutputConfigurationException {
        this(
                buildNatsConnection(configuration),
                configuration.getString(CK_CHANNEL, "graylog"),
                serverStatus.getNodeId().toString(),
                serverStatus.getClusterId()
        );
    }

    private static Connection buildNatsConnection(Configuration configuration) throws MessageOutputConfigurationException {
        final String natsURI = configuration.getString(CK_NATS_URI, "nats://127.0.0.1:4222/");
        final ConnectionFactory cf = new ConnectionFactory(natsURI);

        try {
            return cf.createConnection();
        } catch (IOException | TimeoutException e) {
            throw new MessageOutputConfigurationException("Couldn't start NATS output: " + e.getMessage());
        }
    }

    @VisibleForTesting
    GelfNatsOutput(Connection connection, String channel, String nodeId, String clusterId) {
        this.connection = requireNonNull(connection);
        this.channel = requireNonNull(channel);
        this.nodeId = requireNonNull(nodeId);
        this.clusterId = requireNonNull(clusterId);
        isRunning.set(true);
    }

    @Override
    public void stop() {
        if (isRunning() && connection != null) {
            LOG.debug("Stopping NATS output");
            try {
                connection.close();
                isRunning.set(false);
            } catch (Exception e) {
                LOG.error("Error stopping NATS output", e);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
    }

    @Override
    public void write(Message message) throws Exception {
        connection.publish(channel, toGELFMessage(message));
    }

    @Override
    public void write(List<Message> messages) throws Exception {
        for (Message message : messages) {
            write(message);
        }
    }

    @Nullable
    private Integer extractLevel(Object rawLevel) {
        Integer level;
        if (rawLevel instanceof Number) {
            level = ((Number) rawLevel).intValue();
        } else if (rawLevel instanceof String) {
            try {
                level = Integer.parseInt((String) rawLevel);
            } catch (NumberFormatException e) {
                LOG.debug("Invalid message level " + rawLevel, e);
                level = null;
            }
        } else {
            LOG.debug("Invalid message level {}", rawLevel);
            level = null;
        }

        return level;
    }

    protected byte[] toGELFMessage(final Message message) throws IOException {
        final HashMap<String, Object> fields = new HashMap<>(message.getFields());

        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(512);
        final JsonFactory jsonFactory = new JsonFactory();
        final JsonGenerator generator = jsonFactory.createGenerator(outputStream);
        generator.writeStartObject();
        generator.writeStringField("version", "1.1");
        generator.writeStringField("host", (String) fields.remove(Message.FIELD_SOURCE));
        generator.writeStringField("short_message", (String) fields.remove(Message.FIELD_MESSAGE));

        final String fullMessage = (String) fields.remove(Message.FIELD_FULL_MESSAGE);
        if (fullMessage != null) {
            generator.writeStringField("full_message", fullMessage);
        }

        final Object fieldTimeStamp = fields.remove(Message.FIELD_TIMESTAMP);
        final DateTime timestamp;
        if (fieldTimeStamp instanceof DateTime) {
            timestamp = (DateTime) fieldTimeStamp;
        } else {
            timestamp = Tools.nowUTC();
        }
        generator.writeNumberField("timestamp", timestamp.getMillis() / 1000d);

        final Object fieldLevel = fields.remove(Message.FIELD_TIMESTAMP);
        final Integer level = extractLevel(fieldLevel);
        if (level != null) {
            generator.writeNumberField("level", level);
        }

        for (Map.Entry<String, Object> field : fields.entrySet()) {
            final String key = field.getKey();
            final Object value = field.getValue();

            if (value instanceof String) {
                generator.writeStringField(key, (String) value);
            } else if (value instanceof Boolean) {
                generator.writeBooleanField(key, (Boolean) value);
            } else if (value instanceof Integer) {
                generator.writeNumberField(key, (Integer) value);
            } else if (value instanceof Long) {
                generator.writeNumberField(key, (Long) value);
            } else if (value instanceof Float) {
                generator.writeNumberField(key, (Float) value);
            } else if (value instanceof Double) {
                generator.writeNumberField(key, (Double) value);
            } else if (value instanceof BigDecimal) {
                generator.writeNumberField(key, (BigDecimal) value);
            } else if (value == null) {
                generator.writeNullField(key);
            }
        }

        generator.writeStringField("_forwarder_cluster_id", clusterId);
        generator.writeStringField("_forwarder_node_id", nodeId);

        generator.writeEndObject();

        generator.flush();

        return outputStream.toByteArray();
    }

    @FactoryClass
    public interface Factory extends MessageOutput.Factory<GelfNatsOutput> {
        @Override
        GelfNatsOutput create(Stream stream, Configuration configuration);

        @Override
        GelfNatsOutput.Config getConfig();

        @Override
        GelfNatsOutput.Descriptor getDescriptor();
    }

    @ConfigClass
    public static class Config extends MessageOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();

            r.addField(new TextField(
                    CK_NATS_URI,
                    "NATS URI",
                    "nats://localhost",
                    "URI of the NATS server: nats://[password@]host[:port][/databaseNumber]",
                    ConfigurationField.Optional.NOT_OPTIONAL));
            r.addField(new TextField(
                    CK_CHANNEL,
                    "Channel",
                    "",
                    "Name of the channel to publish messages to",
                    ConfigurationField.Optional.NOT_OPTIONAL));

            return r;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("GELF NATS Output", false, "", "An output sending messages to NATS.");
        }
    }
}
