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

import com.google.common.collect.ImmutableMap;
import io.nats.stan.Connection;
import io.nats.stan.ConnectionFactory;
import org.graylog.plugins.nats.BaseNatsStreamingTest;
import org.graylog.plugins.nats.NatsConstants;
import org.graylog.plugins.nats.config.NatsConfig;
import org.graylog.plugins.nats.config.NatsStreamingConfig;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.when;

public class GelfNatsStreamingOutputIT extends BaseNatsStreamingTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    private static final String CHANNELS = "GelfNatsStreamingOutputIT";

    @Mock
    private NodeId nodeId;
    @Mock
    private ServerStatus serverStatus;

    private GelfNatsStreamingOutput output;

    @Before
    public void setUp() throws MessageOutputConfigurationException {
        final Configuration configuration = new Configuration(
                ImmutableMap.of(
                        NatsConfig.CK_SERVER_URIS, NatsConstants.URL,
                        NatsConfig.CK_CHANNELS, CHANNELS,
                        NatsConfig.CK_CONNECTION_NAME, "GelfNatsStreamingOutputIT-publisher",
                        NatsStreamingConfig.CK_CLUSTER_ID, NatsConstants.CLUSTER_ID,
                        NatsStreamingConfig.CK_CLIENT_ID, "GelfNatsStreamingOutputIT-publisher"
                )

        );
        when(serverStatus.getClusterId()).thenReturn("GRAYLOG-CLUSTER-ID");
        when(nodeId.toString()).thenReturn("GRAYLOG-NODE-ID");
        when(serverStatus.getNodeId()).thenReturn(nodeId);

        output = new GelfNatsStreamingOutput(configuration, serverStatus);

        assumeTrue(output.isRunning());
    }

    @After
    public void tearDown() {
        output.stop();
    }

    @Test
    public void publishMessage() throws Exception {
        final List<byte[]> receivedMessages = new CopyOnWriteArrayList<>();
        final ConnectionFactory cf = new ConnectionFactory(NatsConstants.CLUSTER_ID, "GelfNatsStreamingOutputIT-consumer");
        cf.setNatsUrl(NatsConstants.URL);
        cf.setConnectTimeout(Duration.ofSeconds(5L));

        try (Connection nc = cf.createConnection()) {
            nc.subscribe(CHANNELS, msg -> receivedMessages.add(msg.getData()));

            final DateTime timestamp = new DateTime(2016, 9, 5, 11, 0, DateTimeZone.UTC);
            final Map<String, Object> messageFields = ImmutableMap.<String, Object>builder()
                    .put(Message.FIELD_ID, "061b5ed0-734a-11e6-8e18-6c4008b8fc28")
                    .put(Message.FIELD_MESSAGE, "TEST")
                    .put(Message.FIELD_SOURCE, "integration.test")
                    .put(Message.FIELD_TIMESTAMP, timestamp)
                    .put(Message.FIELD_LEVEL, 5)
                    .put("facility", "IntegrationTest")
                    .put("string", "foobar")
                    .put("bool", true)
                    .put("int", 42)
                    .put("long", 4242424242L)
                    .put("float", 23.42f)
                    .put("double", 23.42d)
                    .put("big_decimal", new BigDecimal("42424242424242424242"))
                    .build();
            final Message message = new Message(messageFields);

            output.write(message);

            await().atMost(10L, TimeUnit.SECONDS).until(() -> !receivedMessages.isEmpty());
        }

        final byte[] expectedMessage = ("{" +
                "\"version\":\"1.1\"," +
                "\"host\":\"integration.test\"" +
                ",\"short_message\":\"TEST\"," +
                "\"timestamp\":1.4730732E9," +
                "\"big_decimal\":42424242424242424242," +
                "\"string\":\"foobar\"," +
                "\"bool\":true," +
                "\"level\":5," +
                "\"double\":23.42," +
                "\"float\":23.42," +
                "\"int\":42," +
                "\"long\":4242424242," +
                "\"_id\":\"061b5ed0-734a-11e6-8e18-6c4008b8fc28\"," +
                "\"facility\":\"IntegrationTest\"," +
                "\"_forwarder_cluster_id\":\"GRAYLOG-CLUSTER-ID\"," +
                "\"_forwarder_node_id\":\"GRAYLOG-NODE-ID\"}").getBytes(StandardCharsets.UTF_8);
        assertThat(receivedMessages)
                .isNotEmpty()
                .containsOnly(expectedMessage);
    }
}
