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

import com.google.inject.assistedinject.Assisted;
import io.nats.stan.Connection;
import io.nats.stan.ConnectionFactory;
import org.graylog.plugins.nats.config.NatsStreamingConfig;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public class GelfNatsStreamingOutput extends AbstractGelfNatsOutput {
    private static final Logger LOG = LoggerFactory.getLogger(GelfNatsStreamingOutput.class);

    private static final String CK_CLUSTER_ID = "cluster_id";
    private static final String CK_CLIENT_ID = "client_id";
    private static final String CK_ACK_TIMEOUT = "ack_timeout";
    private static final String CK_DISCOVER_PREFIX = "discover_prefix";
    private static final String CK_MAX_PUB_ACKS_IN_FLIGHT = "max_pub_acks_in_flight";

    private static final String DEFAULT_DISCOVER_PREFIX = "_STAN.discover";
    private static final int DEFAULT_MAX_PUB_ACKS_IN_FLIGHT = 16384;
    private static final int DEFAULT_ACK_TIMEOUT = 30000;

    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final Connection streamingConnection;

    @Inject
    public GelfNatsStreamingOutput(@Assisted Configuration configuration, ServerStatus serverStatus) throws MessageOutputConfigurationException {
        super(
                createNatsConnection(configuration),
                getChannels(configuration),
                serverStatus.getNodeId().toString(),
                serverStatus.getClusterId()
        );
        streamingConnection = createNatsStreamingConnection(configuration, connection);
    }

    private static Connection createNatsStreamingConnection(Configuration configuration, io.nats.client.Connection connection) throws MessageOutputConfigurationException {
        final String clusterId = configuration.getString(CK_CLUSTER_ID);
        final String clientId = configuration.getString(CK_CLIENT_ID);
        final String discoverPrefix = configuration.getString(CK_DISCOVER_PREFIX, DEFAULT_DISCOVER_PREFIX);
        final int ackTimeout = configuration.getInt(CK_ACK_TIMEOUT, DEFAULT_ACK_TIMEOUT);
        final int maxPubAcksInFlight = configuration.getInt(CK_MAX_PUB_ACKS_IN_FLIGHT, DEFAULT_MAX_PUB_ACKS_IN_FLIGHT);

        final ConnectionFactory cf = new ConnectionFactory(clusterId, clientId);
        cf.setNatsConnection(connection);
        cf.setDiscoverPrefix(discoverPrefix);
        cf.setAckTimeout(ackTimeout, TimeUnit.MILLISECONDS);
        cf.setMaxPubAcksInFlight(maxPubAcksInFlight);

        try {
            return cf.createConnection();
        } catch (IOException | TimeoutException e) {
            throw new MessageOutputConfigurationException("Couldn't start NATS Streaming output: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (isRunning() && streamingConnection != null) {
            LOG.debug("Closing NATS Streaming connection");
            try {
                streamingConnection.close();
                isRunning.set(false);
            } catch (Exception e) {
                LOG.error("Error closing NATS Streaming connection", e);
            }
        }

        super.stop();
    }

    @Override
    public void write(Message message) throws Exception {
        for (String channel : channels) {
            streamingConnection.publish(channel, toGELFMessage(message));
        }
    }

    @FactoryClass
    public interface Factory extends MessageOutput.Factory<GelfNatsStreamingOutput> {
        @Override
        GelfNatsStreamingOutput create(Stream stream, Configuration configuration);

        @Override
        GelfNatsStreamingOutput.Config getConfig();

        @Override
        GelfNatsStreamingOutput.Descriptor getDescriptor();
    }

    @ConfigClass
    public static class Config extends AbstractGelfNatsOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();
            NatsStreamingConfig.addFields(r);
            return r;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("GELF NATS Streaming Output", false, "", "An output sending messages to a NATS Streaming server.");
        }
    }
}
