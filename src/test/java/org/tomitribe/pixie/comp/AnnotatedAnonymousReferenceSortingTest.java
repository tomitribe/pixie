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

import org.tomitribe.pixie.Component;
import org.tomitribe.pixie.Name;
import org.tomitribe.pixie.System;
import org.junit.Assert;
import org.junit.Test;
import org.tomitribe.util.Join;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The link for each component reference is not specified in the configuration file.
 * It is however mentioned via a @Default annotation on the component reference and
 * those names just happen to line up perfectly with what is configured.
 *
 * We should be able to make this work.
 */
public class AnnotatedAnonymousReferenceSortingTest extends Assert {

    private static final List<String> order = new ArrayList<>();

    @Test
    public void setup() throws Exception {
        order.clear();
        final Properties properties = new Properties();

        properties.put("laniakea", "new://" + Universe.class.getName());
        properties.put("someday", "new://" + Peace.class.getName());
        properties.put("earth", "new://" + World.class.getName());
        properties.put("milkyway", "new://" + Galaxy.class.getName());
        properties.put("helios", "new://" + SolarSystem.class.getName());

        final System system = new System(properties);

        assertEquals("Peace{someday}\n" +
                "World{earth}\n" +
                "SolarSystem{helios}\n" +
                "Galaxy{milkyway}\n" +
                "Universe{laniakea}", Join.join("\n", order));
    }

    public static class Universe {
        public Universe(
                final @Name String name,
                final @Component("galaxy") Galaxy galaxy
        ) {
            order.add("Universe{" + name + "}");
        }
    }

    public static class Galaxy {
        public Galaxy(
                final @Name String name,
                final @Component("solarSystem") SolarSystem SolarSystem
        ) {
            order.add("Galaxy{" + name + "}");
        }
    }

    public static class SolarSystem {
        public SolarSystem(
                final @Name String name,
                final @Component("world") World world
        ) {
            order.add("SolarSystem{" + name + "}");
        }
    }

    public static class World {
        public World(
                final @Name String name,
                final @Component("peace") Peace peace
        ) {
            order.add("World{" + name + "}");
        }
    }

    public static class Peace {
        public Peace(final @Name String name) {
            order.add("Peace{" + name + "}");
        }
    }

}
