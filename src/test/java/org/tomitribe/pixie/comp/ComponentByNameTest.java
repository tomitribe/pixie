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

import org.junit.Assert;
import org.junit.Test;
import org.tomitribe.pixie.Component;
import org.tomitribe.pixie.Name;
import org.tomitribe.pixie.Param;
import org.tomitribe.pixie.System;

import java.util.Properties;

public class ComponentByNameTest extends Assert {

    @Test
    public void test() throws Exception {
        final Properties properties = new Properties();

        properties.put("helios", "new://" + SolarSystem.class.getName());
        properties.put("helios.wOrLD", "@earth");

        properties.put("venus", "new://" + World.class.getName());
        properties.put("earth", "new://" + World.class.getName());
        properties.put("mars", "new://" + World.class.getName());


        final System system = new System(properties);
        final SolarSystem solarSystem = system.get(SolarSystem.class);
        assertNotNull(solarSystem);
        assertEquals("earth", solarSystem.getWorld().getName());
    }


    public static class SolarSystem {
        private final World world;

        public SolarSystem(
                final @Name String name,
                final @Param("world") @Component World world
        ) {
            this.world = world;
        }

        public World getWorld() {
            return world;
        }
    }

    public static class World {
        private final String name;

        public World(final @Name String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "World{" +
                    "name='" + name + '\'' +
                    '}';
        }
    }
}
