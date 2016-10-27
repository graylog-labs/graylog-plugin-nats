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

import org.graylog.plugins.nats.transport.NatsTransport;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.inputs.transports.NettyTransport;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
@Ignore
public class NatsTransportConfigTest {
    @Test
    public void getRequestedConfigurationOverridesDefaultPort() throws Exception {
        final NatsTransport.Config config = new NatsTransport.Config();
        final ConfigurationRequest requestedConfiguration = config.getRequestedConfiguration();

        assertThat(requestedConfiguration.containsField(NettyTransport.CK_PORT)).isTrue();
        assertThat(requestedConfiguration.getField(NettyTransport.CK_PORT).getDefaultValue()).isEqualTo(5044);
    }
}
