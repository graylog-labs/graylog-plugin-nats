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
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class GelfNatsOutput extends AbstractGelfNatsOutput {
    private static final Logger LOG = LoggerFactory.getLogger(GelfNatsOutput.class);

    @Inject
    public GelfNatsOutput(@Assisted Configuration configuration, ServerStatus serverStatus) throws MessageOutputConfigurationException {
        super(
                createNatsConnection(configuration),
                getChannels(configuration),
                serverStatus.getNodeId().toString(),
                serverStatus.getClusterId()
        );
    }

    @Override
    public void write(Message message) throws Exception {
        for (String channel : channels) {
            connection.publish(channel, toGELFMessage(message));
        }
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
    public static class Config extends AbstractGelfNatsOutput.Config {
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("GELF NATS Output", false, "", "An output sending messages to a NATS server.");
        }
    }
}
