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

import java.net.URI;
import java.util.Properties;

public class MissingRequiredOptionExceptionTest extends Assert {

    private MissingRequiredOptionException e;

    @Before
    public void setup() {
        try {
            final System system = new System(new Properties());
            system.get(NoSuchOption.class);
            throw new AssertionError("ConstructionFailedException should have been thrown");
        } catch (ConstructionFailedException e) {
            assertNotNull(e.getCause());
            assertEquals(e.getComponent(), NoSuchOption.class);

            assertTrue(e.getCause() instanceof MissingRequiredOptionException);
            this.e = (MissingRequiredOptionException) e.getCause();
        }
    }

    @Test
    public void testGetMessage() throws Exception {
        assertEquals("Missing required option 'location' for component " +
                "org.tomitribe.pixie.comp." +
                "MissingRequiredOptionExceptionTest$NoSuchOption", e.getMessage());
    }

    @Test
    public void testGetOptionName() throws Exception {
        assertEquals("location", e.getOptionName());
    }

    @Test
    public void testGetComponent() throws Exception {
        assertEquals(NoSuchOption.class, e.getComponent());
    }

    @Test
    public void testGetMessage1() throws Exception {

    }

    @Test
    public void testGetOptionName1() throws Exception {

    }

    public static class NoSuchOption {

        public NoSuchOption(final @Name String name, final @Option("location") URI location) {
        }
    }
}