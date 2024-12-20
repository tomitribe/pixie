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

import org.tomitribe.pixie.System;
import org.junit.Assert;
import org.junit.Test;

import java.util.Properties;

public class ComponentOverridesAreCaseInsensitiveTest extends Assert {

    @Test
    public void test() throws Exception {
        final Properties properties = new Properties();

        properties.put("laniakea", "new://" + Universe.class.getName());
        properties.put("laniakea.gAlAxY", "@milkyway");

        properties.put("milkyway", "new://" + Galaxy.class.getName());
        properties.put("milkyway.soLaRSyStEm", "@helios");

        properties.put("helios", "new://" + SolarSystem.class.getName());
        properties.put("helios.wOrLD", "@earth");

        properties.put("earth", "new://" + World.class.getName());
        properties.put("earth.pEAce", "@someday");

        properties.put("someday", "new://" + Peace.class.getName());

        final System system = new System(properties);
        final Universe universe = system.get(Universe.class);
        assertNotNull(universe);
    }

    public static class Universe {
        public Universe(
                final @Name String name,
                final @Component("galaxy") Galaxy galaxy
        ) {
        }
    }

    public static class Galaxy {
        public Galaxy(
                final @Name String name,
                final @Component("solarSystem") SolarSystem SolarSystem
        ) {
        }
    }

    public static class SolarSystem {
        public SolarSystem(
                final @Name String name,
                final @Component("world") World world
        ) {
        }
    }

    public static class World {
        public World(
                final @Name String name,
                final @Component("peace") Peace peace
        ) {
        }
    }

    public static class Peace {
        // No valid constructor
        public Peace() {
        }
    }

}
