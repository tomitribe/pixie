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
import org.tomitribe.util.SuperProperties;

import java.util.List;
import java.util.Properties;

public class ComponentListByTypeTest extends Assert {

    @Test
    public void test() throws Exception {
        final Properties properties = new SuperProperties();

        properties.put("helios", "new://" + SolarSystem.class.getName());

        properties.put("mercury", "new://" + World.class.getName());
        properties.put("venus", "new://" + World.class.getName());
        properties.put("earth", "new://" + World.class.getName());
        properties.put("mars", "new://" + World.class.getName());
        properties.put("jupiter", "new://" + World.class.getName());
        properties.put("saturn", "new://" + World.class.getName());
        properties.put("uranus", "new://" + World.class.getName());
        properties.put("neptune", "new://" + World.class.getName());


        final System system = new System(properties);
        final SolarSystem solarSystem = system.get(SolarSystem.class);
        assertNotNull(solarSystem);
        assertEquals(8, solarSystem.getWorlds().size());
        assertEquals("earth\n" +
                        "jupiter\n" +
                        "mars\n" +
                        "mercury\n" +
                        "neptune\n" +
                        "saturn\n" +
                        "uranus\n" +
                        "venus",
                solarSystem.getWorlds().stream()
                        .map(World::getName)
                        .sorted()
                        .reduce((s, s2) -> s + "\n" + s2)
                        .get());
    }


    public static class SolarSystem {
        private final List<World> worlds;

        public SolarSystem(
                final @Name String name,
                final @Param("worlds") @Component List<World> worlds
        ) {
            this.worlds = worlds;
        }

        public List<World> getWorlds() {
            return worlds;
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
