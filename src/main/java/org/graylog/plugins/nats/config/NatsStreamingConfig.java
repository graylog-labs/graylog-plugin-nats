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

import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;

public class NatsStreamingConfig {
    public static final String CK_CLUSTER_ID = "cluster_id";
    public static final String CK_CLIENT_ID = "client_id";
    public static final String CK_ACK_TIMEOUT = "ack_timeout";
    public static final String CK_DISCOVER_PREFIX = "discover_prefix";
    public static final String CK_MAX_PUB_ACKS_IN_FLIGHT = "max_pub_acks_in_flight";

    public static final String DEFAULT_DISCOVER_PREFIX = "_STAN.discover";
    public static final int DEFAULT_MAX_PUB_ACKS_IN_FLIGHT = 16384;
    public static final int DEFAULT_ACK_TIMEOUT = 30000;

    public static void addFields(ConfigurationRequest r) {
        r.addField(new TextField(
                CK_CLIENT_ID,
                "NATS Client ID",
                "graylog",
                "Unique NATS Client ID",
                ConfigurationField.Optional.NOT_OPTIONAL));
        r.addField(new TextField(
                CK_CLUSTER_ID,
                "NATS Cluster ID",
                "graylog",
                "NATS Cluster ID",
                ConfigurationField.Optional.NOT_OPTIONAL));
        r.addField(new TextField(
                CK_DISCOVER_PREFIX,
                "Discover Prefix",
                DEFAULT_DISCOVER_PREFIX,
                "The discover prefix string that is used to establish a STAN session",
                ConfigurationField.Optional.OPTIONAL));
        r.addField(new NumberField(
                CK_ACK_TIMEOUT,
                "ACK timeout (ms)",
                DEFAULT_ACK_TIMEOUT,
                "The ACK timeout duration in milliseconds",
                NumberField.Attribute.ONLY_POSITIVE));
        r.addField(new NumberField(
                CK_MAX_PUB_ACKS_IN_FLIGHT,
                "Max. ACKs in flight",
                DEFAULT_MAX_PUB_ACKS_IN_FLIGHT,
                "The maximum number of publish ACKs that may be in flight at any point in time",
                NumberField.Attribute.ONLY_POSITIVE));
    }
}
