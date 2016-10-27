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

import com.codahale.metrics.MetricSet;
import com.google.common.eventbus.EventBus;
import io.nats.client.Connection;
import io.nats.client.ConnectionFactory;
import org.graylog.plugins.nats.config.NatsConfig;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

abstract class AbstractNatsTransport extends ThrottleableTransport {
    private final LocalMetricRegistry metricRegistry;
    protected final Configuration configuration;
    protected Connection connection;

    AbstractNatsTransport(Configuration configuration,
                          EventBus eventBus,
                          LocalMetricRegistry metricRegistry) {
        super(eventBus, configuration);
        this.configuration = configuration;
        this.metricRegistry = requireNonNull(metricRegistry);
    }

    @Override
    protected void doLaunch(MessageInput input) throws MisfireException {
        connection = createNatsConnection();
    }

    private Connection createNatsConnection() throws MisfireException {
        final String[] servers = getServers();
        final String connectionName = configuration.getString(NatsConfig.CK_CONNECTION_NAME, NatsConfig.DEFAULT_CONNECTION_NAME);
        final int connectionTimeout = configuration.getInt(NatsConfig.CK_CONNECTION_TIMEOUT, ConnectionFactory.DEFAULT_TIMEOUT);
        final int maxReconnect = configuration.getInt(NatsConfig.CK_MAX_RECONNECT, ConnectionFactory.DEFAULT_MAX_RECONNECT);
        final int maxOutstandingPings = configuration.getInt(NatsConfig.CK_MAX_OUTSTANDING_PINGS, ConnectionFactory.DEFAULT_MAX_PINGS_OUT);
        final int pingInterval = configuration.getInt(NatsConfig.CK_PING_INTERVAL, ConnectionFactory.DEFAULT_PING_INTERVAL);
        final boolean noRandomize = configuration.getBoolean(NatsConfig.CK_NO_RANDOMIZE, false);
        final boolean pedantic = configuration.getBoolean(NatsConfig.CK_PEDANTIC, false);
        final boolean verbose = configuration.getBoolean(NatsConfig.CK_VERBOSE, false);

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
            throw new MisfireException("Couldn't connect to NATS servers", e);
        }
    }

    private String[] getServers() {
        final String serversConfig = configuration.getString(NatsConfig.CK_SERVER_URIS, ConnectionFactory.DEFAULT_URL);
        return StreamSupport.stream(Arrays.spliterator(serversConfig.split("\n")), false)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
    }

    Set<String> getChannels() {
        final String channelsConfig = configuration.getString(NatsConfig.CK_CHANNELS, "");
        return StreamSupport.stream(Arrays.spliterator(channelsConfig.split("\n")), false)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    protected void doStop() {
        if (connection != null) {
            connection.close();
        }
    }

    @Override
    public void setMessageAggregator(CodecAggregator aggregator) {
    }

    @Override
    public MetricSet getMetricSet() {
        return metricRegistry;
    }

    static class Config extends ThrottleableTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();
            NatsConfig.addFields(r);
            return r;
        }
    }
}
