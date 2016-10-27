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
import io.nats.client.MessageHandler;
import io.nats.client.Subscription;
import org.graylog.plugins.nats.config.NatsConfig;
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
import java.util.HashSet;
import java.util.Set;

public class NatsTransport extends AbstractNatsTransport {
    private final Set<Subscription> subscriptions = new HashSet<>();

    @Inject
    public NatsTransport(@Assisted Configuration configuration,
                         EventBus eventBus,
                         LocalMetricRegistry metricRegistry) {
        super(configuration, eventBus, metricRegistry);
    }

    @Override
    protected void doLaunch(MessageInput input) throws MisfireException {
        super.doLaunch(input);

        final MessageHandler messageHandler = m -> input.processRawMessage(new RawMessage(m.getData()));
        final Set<String> channels = getChannels();

        for (String channel : channels) {
            final Subscription subscription = connection.subscribe(channel, messageHandler);
            subscriptions.add(subscription);
        }
    }

    @Override
    protected void doStop() {
        subscriptions.forEach(Subscription::close);

        super.doStop();
    }

    @FactoryClass
    public interface Factory extends Transport.Factory<NatsTransport> {
        @Override
        NatsTransport create(Configuration configuration);

        @Override
        Config getConfig();
    }

    @ConfigClass
    public static class Config extends AbstractNatsTransport.Config {
    }
}
