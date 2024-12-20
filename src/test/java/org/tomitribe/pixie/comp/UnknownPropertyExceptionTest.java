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

public class UnknownPropertyExceptionTest extends Assert {

    private UnknownPropertyException e;

    @Before
    public void setup() {
        try {
            final Properties properties = new Properties();
            properties.put("thing", "new://" + NoSuchOption.class.getName());
            properties.put("thing.shape", "@red");
            new System(properties);
            throw new AssertionError("ConstructionFailedException should have been thrown");
        } catch (ConstructionFailedException e) {
            assertNotNull(e.getCause());
            assertEquals(e.getComponent(), NoSuchOption.class);

            assertTrue(e.getCause() instanceof UnknownPropertyException);
            this.e = (UnknownPropertyException) e.getCause();
        }
    }

    @Test
    public void testGetMessage() throws Exception {
        assertEquals("Unknown property `shape = @red`", e.getMessage());
    }

    @Test
    public void testGetKey() throws Exception {
        assertEquals("shape", e.getKey());
    }

    @Test
    public void testGetValue() throws Exception {
        assertEquals("@red", e.getValue());
    }

    @Test
    public void testGetComponent() throws Exception {
        assertEquals(NoSuchOption.class, e.getComponent());
    }

    public static class NoSuchOption {

        public NoSuchOption(final @Name String name) {
        }
    }
}