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

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import io.nats.client.Connection;
import io.nats.client.ConnectionFactory;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.nio.charset.StandardCharsets;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NatsTransportIT {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private static final String NATS_HOST = System.getProperty("nats.host", ConnectionFactory.DEFAULT_HOST);
    private static final int NATS_PORT = Integer.getInteger("nats.port", ConnectionFactory.DEFAULT_PORT);
    private static final String NATS_URL = "nats://" + NATS_HOST + ":" + NATS_PORT;

    private EventBus eventBus;
    private LocalMetricRegistry localMetricRegistry;

    @Before
    public void setUp() throws MessageOutputConfigurationException {
        eventBus = new EventBus();
        localMetricRegistry = new LocalMetricRegistry();
    }

    @Test
    public void subscribeChannel() throws Exception {
        final Configuration configuration = new Configuration(
                ImmutableMap.of(
                        "nats_uri", NATS_URL,
                        "channels", "graylog"
                )
        );

        final NatsTransport redisTransport = new NatsTransport(configuration, eventBus, localMetricRegistry);
        final MessageInput messageInput = mock(MessageInput.class);
        redisTransport.launch(messageInput);

        final ConnectionFactory cf = new ConnectionFactory(NATS_URL);
        try (Connection nc = cf.createConnection()) {
            nc.publish("graylog", "TEST".getBytes(StandardCharsets.UTF_8));
        }

        Thread.sleep(100L);

        verify(messageInput, times(1)).processRawMessage(any(RawMessage.class));

        redisTransport.stop();
    }
}
