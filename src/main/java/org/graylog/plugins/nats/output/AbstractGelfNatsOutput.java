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
import io.nats.client.Connection;
import io.nats.client.ConnectionFactory;
import org.graylog.plugins.nats.config.NatsConfig;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

abstract class AbstractGelfNatsOutput implements MessageOutput {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractGelfNatsOutput.class);

    private static final String CK_SERVER_URIS = "server_uris";
    private static final String CK_CHANNELS = "channels";
    private static final String CK_CONNECTION_NAME = "connection_name";
    private static final String CK_CONNECTION_TIMEOUT = "connection_timeout";
    private static final String CK_NO_RANDOMIZE = "no_randomize";
    private static final String CK_VERBOSE = "verbose";
    private static final String CK_PING_INTERVAL = "require_protocol_acks";
    private static final String CK_MAX_RECONNECT = "max_reconnect";
    private static final String CK_MAX_OUTSTANDING_PINGS = "max_pings_out";
    private static final String CK_PEDANTIC = "pedantic";

    private static final String DEFAULT_CONNECTION_NAME = "graylog";
    private static final String DEFAULT_CHANNELS = "graylog";

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final String nodeId;
    private final String clusterId;

    final Connection connection;
    final String[] channels;


    static Connection createNatsConnection(Configuration configuration) throws MessageOutputConfigurationException {
        final String[] servers = getServers(configuration);
        final String connectionName = configuration.getString(CK_CONNECTION_NAME, DEFAULT_CONNECTION_NAME);
        final int connectionTimeout = configuration.getInt(CK_CONNECTION_TIMEOUT, ConnectionFactory.DEFAULT_TIMEOUT);
        final int maxReconnect = configuration.getInt(CK_MAX_RECONNECT, ConnectionFactory.DEFAULT_MAX_RECONNECT);
        final int maxOutstandingPings = configuration.getInt(CK_MAX_OUTSTANDING_PINGS, ConnectionFactory.DEFAULT_MAX_PINGS_OUT);
        final int pingInterval = configuration.getInt(CK_PING_INTERVAL, ConnectionFactory.DEFAULT_PING_INTERVAL);
        final boolean noRandomize = configuration.getBoolean(CK_NO_RANDOMIZE, false);
        final boolean pedantic = configuration.getBoolean(CK_PEDANTIC, false);
        final boolean verbose = configuration.getBoolean(CK_VERBOSE, false);

        final ConnectionFactory cf = new ConnectionFactory(servers);
        cf.setConnectionName(connectionName);
        cf.setConnectionTimeout(connectionTimeout);
        cf.setMaxReconnect(maxReconnect);
        cf.setMaxPingsOut(maxOutstandingPings);
        cf.setPingInterval(pingInterval);
        cf.setNoRandomize(noRandomize);
        cf.setPedantic(pedantic);
        cf.setVerbose(verbose);

        try {
            return cf.createConnection();
        } catch (Exception e) {
            throw new MessageOutputConfigurationException("Couldn't connect to NATS servers: " + e.getMessage());
        }
    }

    private static String[] getServers(Configuration configuration) {
        final String serversConfig = configuration.getString(CK_SERVER_URIS, ConnectionFactory.DEFAULT_URL);
        return splitByNewline(serversConfig);
    }

    static String[] getChannels(Configuration configuration) {
        final String channelsConfig = configuration.getString(CK_CHANNELS, DEFAULT_CHANNELS);
        return splitByNewline(channelsConfig);
    }

    private static String[] splitByNewline(String text) {
        return StreamSupport.stream(Arrays.spliterator(text.split("\n")), false)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    @VisibleForTesting
    AbstractGelfNatsOutput(Connection connection, String[] channels, String nodeId, String clusterId) {
        this.connection = requireNonNull(connection);
        this.channels = requireNonNull(channels);
        this.nodeId = requireNonNull(nodeId);
        this.clusterId = requireNonNull(clusterId);
        isRunning.set(true);
    }

    @Override
    public void stop() {
        if (isRunning() && connection != null) {
            LOG.debug("Closing NATS connection");
            try {
                connection.close();
                isRunning.set(false);
            } catch (Exception e) {
                LOG.error("Error closing NATS connection", e);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning.get();
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

    static class Config extends MessageOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();
            NatsConfig.addFields(r);
            return r;
        }
    }
}
