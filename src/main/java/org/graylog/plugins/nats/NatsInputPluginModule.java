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
package org.graylog.plugins.nats;

import org.graylog.plugins.nats.input.GelfNatsInput;
import org.graylog.plugins.nats.input.RawNatsInput;
import org.graylog.plugins.nats.input.SyslogNatsInput;
import org.graylog.plugins.nats.output.GelfNatsOutput;
import org.graylog.plugins.nats.transport.NatsTransport;
import org.graylog2.plugin.PluginModule;

public class NatsInputPluginModule extends PluginModule {
    @Override
    protected void configure() {
        addTransport("nats", NatsTransport.class);
        addMessageInput(GelfNatsInput.class);
        addMessageInput(RawNatsInput.class);
        addMessageInput(SyslogNatsInput.class);
        addMessageOutput(GelfNatsOutput.class);
    }
}
