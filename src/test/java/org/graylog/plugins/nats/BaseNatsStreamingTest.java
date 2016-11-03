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

import io.nats.client.ConnectionFactory;
import org.awaitility.Awaitility;
import org.junit.BeforeClass;

import java.util.concurrent.TimeUnit;

import static org.junit.Assume.assumeFalse;

public abstract class BaseNatsStreamingTest {
    private static boolean SKIP = Boolean.getBoolean("nats-streaming.tests.skip");

    private static final String HOST = System.getProperty("nats-streaming.host", ConnectionFactory.DEFAULT_HOST);
    private static final int PORT = Integer.getInteger("nats-streaming.port", ConnectionFactory.DEFAULT_PORT);
    protected static final String URL = "nats://" + HOST + ":" + PORT;
    protected static final String CLUSTER_ID = "test-cluster";

    static {
        Awaitility.setDefaultTimeout(5L, TimeUnit.SECONDS);
    }

    @BeforeClass
    public static void skipTests() {
        assumeFalse(SKIP);
    }
}
