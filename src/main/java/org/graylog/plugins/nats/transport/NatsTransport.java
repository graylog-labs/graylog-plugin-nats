/**
 * This file is part of Graylog NATS Plugin.
 *
 * Graylog NATS Plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog NATS Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog NATS Plugin.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.nats.transport;

import com.codahale.metrics.MetricSet;
import com.google.common.eventbus.EventBus;
import com.google.inject.assistedinject.Assisted;
import io.nats.client.Connection;
import io.nats.client.ConnectionFactory;
import io.nats.client.MessageHandler;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.MisfireException;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.transports.ThrottleableTransport;
import org.graylog2.plugin.inputs.transports.Transport;
import org.graylog2.plugin.journal.RawMessage;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import java.util.stream.StreamSupport;

import static java.util.Objects.requireNonNull;

public class NatsTransport extends ThrottleableTransport {
    private static final String CK_NATS_URI = "nats_uri";
    private static final String CK_CHANNELS = "channels";

    private final Configuration configuration;
    private final LocalMetricRegistry metricRegistry;
    private Connection connection;

    @Inject
    public NatsTransport(@Assisted Configuration configuration,
                         EventBus eventBus,
                         LocalMetricRegistry metricRegistry) {
        super(eventBus, configuration);
        this.configuration = configuration;
        this.metricRegistry = requireNonNull(metricRegistry);
    }

    @Override
    protected void doLaunch(MessageInput input) throws MisfireException {
        final String natsURI = configuration.getString(CK_NATS_URI);
        final ConnectionFactory cf = new ConnectionFactory(natsURI);

        try {
            connection = cf.createConnection();
        } catch (IOException | TimeoutException e) {
            throw new MisfireException("Couldn't connect NATS server at " + natsURI, e);
        }

        final MessageHandler messageHandler = m -> input.processRawMessage(new RawMessage(m.getData()));
        final String channels = configuration.getString(CK_CHANNELS, "");
        StreamSupport.stream(Arrays.spliterator(channels.split(",")), false)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(channel -> connection.subscribe(channel, messageHandler));
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

    @FactoryClass
    public interface Factory extends Transport.Factory<NatsTransport> {
        @Override
        NatsTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends ThrottleableTransport.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();

            r.addField(new TextField(
                    CK_NATS_URI,
                    "NATS URI",
                    "nats://localhost",
                    "URI of the NATS server: redis://[password@]host[:port][/databaseNumber]",
                    ConfigurationField.Optional.NOT_OPTIONAL));
            r.addField(new TextField(
                    CK_CHANNELS,
                    "Channels",
                    "",
                    "Comma-separated list of channels to subscribe to",
                    ConfigurationField.Optional.OPTIONAL));
            return r;
        }
    }
}
