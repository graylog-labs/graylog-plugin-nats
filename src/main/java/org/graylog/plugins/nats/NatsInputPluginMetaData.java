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

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

public class NatsInputPluginMetaData implements PluginMetaData {
    @Override
    public String getUniqueId() {
        return NatsInputPlugin.class.getCanonicalName();
    }

    @Override
    public String getName() {
        return "NATS plugin";
    }

    @Override
    public String getAuthor() {
        return "Graylog, Inc.";
    }

    @Override
    public URI getURL() {
        return URI.create("https://www.graylog.org/");
    }

    @Override
    public Version getVersion() {
        return Version.from(1, 0, 0);
    }

    @Override
    public String getDescription() {
        return "Input plugin for NATS.";
    }

    @Override
    public Version getRequiredVersion() {
        return Version.from(2, 0, 0);
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}