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

import java.lang.reflect.Constructor;
import java.net.URI;
import java.util.Properties;

public class AmbiguousConstructorExceptionTest extends Assert {

    private AmbiguousConstructorException e;

    @Before
    public void setup() {
        try {

            final System system = new System(new Properties());
            system.get(AmbiguousDefinition.class);

            throw new AssertionError("ConstructionFailedException should have been thrown");
        } catch (ConstructionFailedException e) {
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof AmbiguousConstructorException);
            this.e = (AmbiguousConstructorException) e.getCause();
        }
    }

    @Test
    public void testGetMessage() throws Exception {
        assertEquals("Only one eligible constructor allowed: " +
                "org.tomitribe.pixie.comp.AmbiguousConstructorExceptionTest$AmbiguousDefinition;" +
                " Found 2", e.getMessage());
    }

    @Test
    public void testGetComponent() throws Exception {
        assertEquals(AmbiguousDefinition.class, e.getComponent());
    }

    @Test
    public void testGetFound() throws Exception {
        assertEquals(2, e.getFound().size());

        for (final Constructor<?> constructor : e.getFound()) {
            assertEquals(AmbiguousDefinition.class, constructor.getDeclaringClass());
        }
    }

    public static class AmbiguousDefinition {

        public AmbiguousDefinition(final @Name String name) {
        }

        public AmbiguousDefinition(final @Name String name, final @Option("location") URI location) {
        }
    }
}