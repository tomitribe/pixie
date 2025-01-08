/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tomitribe.pixie.comp;

import org.tomitribe.pixie.Default;
import org.tomitribe.pixie.Option;
import org.tomitribe.pixie.System;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.Properties;

public class DefaultsTest extends Assert {

    @Test
    public void testDefaults() {
        final Properties properties = new Properties();
        properties.put("defaults", "new://" + Defaults.class.getName());
        final System system = new System(properties);
        final Defaults defaults = system.get(Defaults.class);

        assertNotNull(defaults);
        assertEquals("green", defaults.getColor());
        assertEquals(URI.create("red://location"), defaults.getLocation());
    }

    @Test
    public void testOverride() {
        final Properties properties = new Properties();
        properties.put("configured", "new://" + Defaults.class.getName());
        properties.put("configured.color", "blue");
        properties.put("configured.location", "orange://localhost");
        final System system = new System(properties);
        final Defaults defaults = system.get(Defaults.class);

        assertNotNull(defaults);
        assertEquals("blue", defaults.getColor());
        assertEquals(URI.create("orange://localhost"), defaults.getLocation());
    }

    public static class Defaults {

        private final URI location;
        private final String color;

        public Defaults(
                final @Option("location") @Default("red://location") URI location,
                final @Option("color") @Default("green") String color
        ) {
            this.location = location;
            this.color = color;
        }

        public URI getLocation() {
            return location;
        }

        public String getColor() {
            return color;
        }
    }


}