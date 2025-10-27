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
import org.tomitribe.pixie.Default;
import org.tomitribe.pixie.Name;
import org.tomitribe.pixie.Param;
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
 * <p>
 * We should be able to make this work.
 */
public class AnnotatedNamedReferenceSortingTest extends Assert {

    private static final List<String> order = new ArrayList<>();
    private static final String LANIAKEA = "laniakea";
    private static final String MILKYWAY = "milkyway";
    private static final String HELIOS = "helios";
    private static final String EARTH = "earth";
    private static final String SOMEDAY = "someday";
    private static final String LUNA = "luna";

    @Test
    public void setup() throws Exception {
        order.clear();
        final Properties properties = new Properties();

        properties.put(LANIAKEA, "new://" + Universe.class.getName());
        properties.put(SOMEDAY, "new://" + Peace.class.getName());
        properties.put(EARTH, "new://" + World.class.getName());
        properties.put(MILKYWAY, "new://" + Galaxy.class.getName());
        properties.put(HELIOS, "new://" + SolarSystem.class.getName());
        properties.put(LUNA, "new://" + Luna.class.getName());

        final System system = new System(properties);

        assertEquals("Peace{someday}\n" +
                "World{earth}\n" +
                "SolarSystem{helios}\n" +
                "Galaxy{milkyway}\n" +
                "Universe{laniakea}\n" +
                "Luna{luna}", Join.join("\n", order));
    }

    public static class Universe {
        public Universe(
                final @Name String name,
                final @Param("galaxy") @Component @Default(MILKYWAY) Galaxy galaxy
        ) {
            order.add("Universe{" + name + "}");
        }
    }

    public static class Galaxy {
        public Galaxy(
                final @Name String name,
                final @Param("solarSystem") @Component @Default(HELIOS) SolarSystem SolarSystem
        ) {
            order.add("Galaxy{" + name + "}");
        }
    }

    public static class SolarSystem {
        public SolarSystem(
                final @Name String name,
                final @Param("world") @Component @Default(EARTH) World world
        ) {
            order.add("SolarSystem{" + name + "}");
        }
    }

    public static class World {
        public World(
                final @Name String name,
                final @Param("peace") @Component @Default(SOMEDAY) Peace peace
        ) {
            order.add("World{" + name + "}");
        }
    }

    public static class Peace {
        public Peace(final @Name String name) {
            order.add("Peace{" + name + "}");
        }
    }

    public static class Luna {
        public Luna(
                @Name final String name,
                @Param("peace") @Component final Peace peace,
                @Param("system")@Component final System system) {
            order.add("Luna{" + name + "}");
        }
    }

}
