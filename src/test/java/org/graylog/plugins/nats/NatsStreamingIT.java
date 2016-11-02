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
package org.graylog.plugins.nats;

import io.nats.stan.Connection;
import io.nats.stan.ConnectionFactory;
import io.nats.stan.Message;
import io.nats.stan.Subscription;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

public class NatsStreamingIT {
    private static final String CLIENT_ID = "NatsStreamingIT-client";
    private static final String CHANNEL = "NatsStreamingIT";

    @Test
    public void basicSubscriptionIsWorking() throws Exception {
        final CountDownLatch messageReceived = new CountDownLatch(1);
        final ConnectionFactory cf = new ConnectionFactory(NatsConstants.CLUSTER_ID, CLIENT_ID);
        cf.setNatsUrl(NatsConstants.URL);
        final AtomicReference<Message> messageReference = new AtomicReference<>();
        final byte[] messagePayload = "Hello World".getBytes(StandardCharsets.UTF_8);
        try (
                final Connection sc = cf.createConnection();
                final Subscription sub = sc.subscribe(CHANNEL, m -> {
                    messageReference.set(m);
                    messageReceived.countDown();
                })
        ) {
            sc.publish(CHANNEL, messagePayload);

            messageReceived.await(1L, TimeUnit.SECONDS);
        }

        final Message message = messageReference.get();
        assertThat(message).isNotNull();
        assertThat(message.getData()).isEqualTo(messagePayload);
    }
}
