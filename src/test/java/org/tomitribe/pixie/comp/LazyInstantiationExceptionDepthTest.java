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
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * An object four levels deep has encountered an issue and
 * cannot be built.  This cascades upward.  We verify our
 * exception handling tells this story.
 */
public class LazyInstantiationExceptionDepthTest extends Assert {

    ConstructionFailedException e;

    @Before
    public void setup() throws Exception {
        try {
            final Properties properties = new Properties();
            final System system = new System(properties);
            system.get(Universe.class);
            throw new AssertionError("ConstructionFailedException should have been thrown");
        } catch (ConstructionFailedException e) {
            this.e = e;
        }
    }


    @Test
    public void testGetCauseChain() {
        final List<Throwable> causes = new ArrayList<>();
        Throwable t = e;
        do {
            causes.add(t);
            t = t.getCause();
        } while (t != null);

        assertEquals(Universe.class, ((ConstructionFailedException) causes.get(0)).getComponent());
        assertEquals(Galaxy.class, ((ConstructionFailedException) causes.get(1)).getComponent());
        assertEquals(SolarSystem.class, ((ConstructionFailedException) causes.get(2)).getComponent());
        assertEquals(World.class, ((ConstructionFailedException) causes.get(3)).getComponent());
        assertEquals(Peace.class, ((ConstructionFailedException) causes.get(4)).getComponent());
        assertEquals(Peace.class, ((NoValidConstructorException) causes.get(5)).getComponent());
    }

    @Test
    public void testGetMessage() {
        final String testName = this.getClass().getName();
        assertEquals("" +
                "Unable to construct component: class " + testName + "$Universe - " +
                "Unable to construct component: class " + testName + "$Galaxy - " +
                "Unable to construct component: class " + testName + "$SolarSystem - " +
                "Unable to construct component: class " + testName + "$World - " +
                "Unable to construct component: class " + testName + "$Peace - " +
                "No valid constructor found: " + testName + "$Peace", e.getMessage());
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
        private Peace() {
        }
    }
}
