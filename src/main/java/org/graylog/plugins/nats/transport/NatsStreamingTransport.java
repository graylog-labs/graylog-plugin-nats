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
package org.graylog.plugins.nats.transport;

import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import io.nats.stan.Connection;
import io.nats.stan.ConnectionFactory;
import io.nats.stan.MessageHandler;
import io.nats.stan.Subscription;
import org.graylog.plugins.nats.config.NatsStreamingConfig;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.journal.RawMessage;

import javax.inject.Inject;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NatsStreamingTransport extends AbstractNatsTransport {
    private final Set<Subscription> subscriptions = new HashSet<>();
    private Connection streamingConnection;

    @Inject
    public NatsStreamingTransport(@Assisted Configuration configuration,
                                  EventBus eventBus,
                                  LocalMetricRegistry metricRegistry) {
        super(configuration, eventBus, metricRegistry);
    }

    @Override
    protected void doLaunch(MessageInput input) throws MisfireException {
        super.doLaunch(input);

        streamingConnection = createNatsStreamingConnection();

        final MessageHandler messageHandler = m -> input.processRawMessage(new RawMessage(m.getData()));
        final Set<String> channels = getChannels();
        for (String channel : channels) {
            try {
                final Subscription subscription = streamingConnection.subscribe(channel, messageHandler);
                subscriptions.add(subscription);
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    private Connection createNatsStreamingConnection() throws MisfireException {
        final String clusterId = configuration.getString(NatsStreamingConfig.CK_CLUSTER_ID);
        final String clientId = configuration.getString(NatsStreamingConfig.CK_CLIENT_ID);
        final String discoverPrefix = configuration.getString(NatsStreamingConfig.CK_DISCOVER_PREFIX, NatsStreamingConfig.DEFAULT_DISCOVER_PREFIX);
        final int ackTimeout = configuration.getInt(NatsStreamingConfig.CK_ACK_TIMEOUT, NatsStreamingConfig.DEFAULT_ACK_TIMEOUT);
        final int maxPubAcksInFlight = configuration.getInt(NatsStreamingConfig.CK_MAX_PUB_ACKS_IN_FLIGHT, NatsStreamingConfig.DEFAULT_MAX_PUB_ACKS_IN_FLIGHT);

        final ConnectionFactory cf = new ConnectionFactory(clusterId, clientId);
        cf.setNatsConnection(connection);
        cf.setDiscoverPrefix(discoverPrefix);
        cf.setAckTimeout(ackTimeout, TimeUnit.MILLISECONDS);
        cf.setMaxPubAcksInFlight(maxPubAcksInFlight);

        try {
            return cf.createConnection();
        } catch (IOException | TimeoutException e) {
            throw new MisfireException("Couldn't connect to NATS Streaming server", e);
        }
    }

    @Override
    protected void doStop() {
        subscriptions.forEach(Subscription::close);
        if (streamingConnection != null) {
            try {
                streamingConnection.close();
            } catch (Exception e) {
                // Ignore
            }
        }

        super.doStop();
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<NatsStreamingTransport> {
        @Override
        NatsStreamingTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractNatsTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();
            NatsStreamingConfig.addFields(r);
            return r;
        }
    }
}
