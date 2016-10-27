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
package org.graylog.plugins.nats.input;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.nats.transport.NatsTransport;
import org.graylog2.inputs.codecs.RawCodec;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;

import javax.inject.Inject;

public class RawNatsInput extends MessageInput {
    private static final String NAME = "Raw/Plaintext NATS";

    @Inject
    public RawNatsInput(@Assisted Configuration configuration,
                        NatsTransport.Factory transportFactory,
                        RawCodec.Factory codecFactory,
                        Config config,
                        Descriptor descriptor,
                        MetricRegistry metricRegistry,
                        LocalMetricRegistry localRegistry,
                        ServerStatus serverStatus) {
        super(metricRegistry, configuration, transportFactory.create(configuration),
                localRegistry, codecFactory.create(configuration), config, descriptor, serverStatus);
    }

    @FactoryClass
    public interface Factory extends MessageInput.Factory<RawNatsInput> {
        @Override
        RawNatsInput create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Descriptor extends MessageInput.Descriptor {
        public Descriptor() {
            super(NAME, false, "");
        }
    }

    @ConfigClass
    public static class Config extends MessageInput.Config {
        @Inject
        public Config(NatsTransport.Factory transport, RawCodec.Factory codec) {
            super(transport.getConfig(), codec.getConfig());
        }
    }
}
