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

import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import io.nats.stan.Connection;
import io.nats.stan.ConnectionFactory;
import org.graylog.plugins.nats.BaseNatsStreamingTest;
import org.graylog.plugins.nats.config.NatsConfig;
import org.graylog.plugins.nats.config.NatsStreamingConfig;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NatsStreamingTransportIT extends BaseNatsStreamingTest {
    private static final String CHANNELS = "NatsStreamingTransportIT";

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
                        NatsConfig.CK_SERVER_URIS, URL,
                        NatsConfig.CK_CHANNELS, CHANNELS,
                        NatsConfig.CK_CONNECTION_NAME, "NatsStreamingTransportIT-consumer",
                        NatsStreamingConfig.CK_CLUSTER_ID, CLUSTER_ID,
                        NatsStreamingConfig.CK_CLIENT_ID, "NatsStreamingTransportIT-consumer"
                )
        );

        final MessageInput messageInput = mock(MessageInput.class);
        final ConnectionFactory cf = new ConnectionFactory(CLUSTER_ID, "NatsStreamingTransportIT-publisher");
        cf.setNatsUrl(URL);

        try (final NatsStreamingTransport natsTransport = new NatsStreamingTransport(configuration, eventBus, localMetricRegistry);
             final Connection nc = cf.createConnection()) {
            natsTransport.launch(messageInput);
            await().until(natsTransport::isConnected);

            nc.publish(CHANNELS, "TEST".getBytes(StandardCharsets.UTF_8));

            await()
                    .catchUncaughtExceptions()
                    .until(() -> verify(messageInput, times(1)).processRawMessage(any(RawMessage.class)));
        }
    }
}
