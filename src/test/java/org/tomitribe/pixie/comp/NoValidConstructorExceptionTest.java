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

import java.util.Properties;

public class NoValidConstructorExceptionTest extends Assert {

    private NoValidConstructorException e;

    @Before
    public void setup() {
        try {
            final System system = new System(new Properties());
            system.get(NoValidConstructor.class);
            throw new AssertionError("ConstructionFailedException should have been thrown");
        } catch (ConstructionFailedException e) {
            assertNotNull(e.getCause());
            assertEquals(e.getComponent(), NoValidConstructor.class);

            assertTrue(e.getCause() instanceof NoValidConstructorException);
            this.e = (NoValidConstructorException) e.getCause();
        }
    }

    @Test
    public void testGetMessage() throws Exception {
        assertEquals("No valid constructor found: " +
                "org.tomitribe.pixie.comp.NoValidConstructorExceptionTest$" +
                "NoValidConstructor", e.getMessage());
    }

    @Test
    public void testGetComponent() throws Exception {
        assertEquals(NoValidConstructor.class, e.getComponent());
    }

    public static class NoValidConstructor {

        private NoValidConstructor() {
        }
    }
}