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
import org.graylog.plugins.nats.config.NatsConfig;
import org.graylog.plugins.nats.config.NatsStreamingConfig;
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
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class NatsStreamingTransportIT {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private static final String NATS_HOST = System.getProperty("nats.host", "localhost");
    private static final int NATS_PORT = Integer.getInteger("nats.port", 4223);
    private static final String NATS_URL = "nats://" + NATS_HOST + ":" + NATS_PORT;
    private static final String NATS_CLUSTER_ID = "test-cluster";
    private static final String CHANNELS = "graylog";

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
                        NatsConfig.CK_SERVER_URIS, NATS_URL,
                        NatsConfig.CK_CHANNELS, CHANNELS,
                        NatsStreamingConfig.CK_CLUSTER_ID, NATS_CLUSTER_ID,
                        NatsStreamingConfig.CK_CLIENT_ID, "NatsStreamingTransport-consumer"
                )
        );

        final NatsStreamingTransport natsTransport = new NatsStreamingTransport(configuration, eventBus, localMetricRegistry);
        final MessageInput messageInput = mock(MessageInput.class);
        natsTransport.launch(messageInput);

        final ConnectionFactory cf = new ConnectionFactory(NATS_CLUSTER_ID, "NatsStreamingTransport-publisher");
        cf.setNatsUrl(NATS_URL);
        try (Connection nc = cf.createConnection()) {
            nc.publish(CHANNELS, "TEST".getBytes(StandardCharsets.UTF_8));
        }

        await()
                .atMost(5L, TimeUnit.SECONDS)
                .catchUncaughtExceptions()
                .until(() -> verify(messageInput, times(1)).processRawMessage(any(RawMessage.class)));

        natsTransport.stop();
    }
}
