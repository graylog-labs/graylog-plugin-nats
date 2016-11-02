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
import io.nats.client.Connection;
import io.nats.client.ConnectionFactory;
import org.graylog.plugins.nats.BaseNatsTest;
import org.graylog.plugins.nats.NatsConstants;
import org.graylog.plugins.nats.config.NatsConfig;
import org.graylog2.plugin.LocalMetricRegistry;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NatsTransportIT extends BaseNatsTest {
    private static final String CHANNELS = "NatsTransportIT";

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
                        NatsConfig.CK_SERVER_URIS, NatsConstants.URL,
                        NatsConfig.CK_CHANNELS, CHANNELS,
                        NatsConfig.CK_CONNECTION_NAME, "NatsTransportIT-consumer"
                )
        );

        final MessageInput messageInput = mock(MessageInput.class);
        final ConnectionFactory cf = new ConnectionFactory(NatsConstants.URL);
        cf.setConnectionName("NatsTransportIT-publisher");

        try (final NatsTransport natsTransport = new NatsTransport(configuration, eventBus, localMetricRegistry);
             final Connection nc = cf.createConnection()
        ) {
            natsTransport.launch(messageInput);
            nc.publish(CHANNELS, "TEST".getBytes(StandardCharsets.UTF_8));

            await()
                    .atMost(10L, TimeUnit.SECONDS)
                    .catchUncaughtExceptions()
                    .until(() -> verify(messageInput, times(1)).processRawMessage(any(RawMessage.class)));
        }
    }
}
