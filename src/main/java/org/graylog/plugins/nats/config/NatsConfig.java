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
package org.graylog.plugins.nats.config;

import io.nats.client.ConnectionFactory;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;

public class NatsConfig  {
    public static final String CK_SERVER_URIS = "server_uris";
    public static final String CK_CHANNELS = "channels";
    public static final String CK_CONNECTION_NAME = "connection_name";
    public static final String CK_CONNECTION_TIMEOUT = "connection_timeout";
    public static final String CK_NO_RANDOMIZE = "no_randomize";
    public static final String CK_VERBOSE = "verbose";
    public static final String CK_PING_INTERVAL = "require_protocol_acks";
    public static final String CK_MAX_RECONNECT = "max_reconnect";
    public static final String CK_MAX_OUTSTANDING_PINGS = "max_pings_out";
    public static final String CK_PEDANTIC = "pedantic";

    public static final String DEFAULT_CONNECTION_NAME = "graylog";

    public static void addFields(ConfigurationRequest r) {
        r.addField(new TextField(
                CK_SERVER_URIS,
                "NATS servers",
                "nats://localhost:4222/",
                "List of URIs (one per line) of the NATS servers: nats://[password@]host[:port][/databaseNumber]",
                ConfigurationField.Optional.NOT_OPTIONAL,
                TextField.Attribute.TEXTAREA));
        r.addField(new BooleanField(
                CK_NO_RANDOMIZE,
                "No randomization of servers",
                false,
                "Disable server list randomization"));
        r.addField(new TextField(
                CK_CHANNELS,
                "Channels",
                "",
                "List of channels (one per line)",
                ConfigurationField.Optional.NOT_OPTIONAL,
                TextField.Attribute.TEXTAREA));
        r.addField(new TextField(
                CK_CONNECTION_NAME,
                "Connection Name",
                DEFAULT_CONNECTION_NAME,
                "Name associated with this NATS connection",
                ConfigurationField.Optional.OPTIONAL));
        r.addField(new NumberField(
                CK_CONNECTION_TIMEOUT,
                "Connection Timeout (ms)",
                ConnectionFactory.DEFAULT_TIMEOUT,
                "The maximum amount of time to wait for a connection to a NATS server to complete successfully",
                NumberField.Attribute.ONLY_POSITIVE));
        r.addField(new NumberField(
                CK_MAX_RECONNECT,
                "Max. Reconnects",
                ConnectionFactory.DEFAULT_MAX_RECONNECT,
                "The maximum number of reconnection attempts for this connection",
                NumberField.Attribute.ONLY_POSITIVE));
        r.addField(new NumberField(
                CK_MAX_OUTSTANDING_PINGS,
                "Max. Outstanding Pings",
                ConnectionFactory.DEFAULT_MAX_PINGS_OUT,
                "Once this limit is exceeded, the connection is marked as stale and closed",
                NumberField.Attribute.ONLY_POSITIVE));
        r.addField(new NumberField(
                CK_PING_INTERVAL,
                "Ping Interval (ms)",
                ConnectionFactory.DEFAULT_PING_INTERVAL,
                "Send a PING to the server at this interval to ensure the server is still alive",
                NumberField.Attribute.ONLY_POSITIVE));
        r.addField(new BooleanField(
                CK_VERBOSE,
                "Require server ACKs",
                false,
                "Whether or not this connection should require protocol ACKs from the server"));
        r.addField(new BooleanField(
                CK_PEDANTIC,
                "Strict protocol checking",
                false,
                "Whether or not this connection should require strict server-side protocol checking"));
    }
}
