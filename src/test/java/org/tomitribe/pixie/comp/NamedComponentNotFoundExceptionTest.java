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

/**
 * NamedComponentNotFoundException is like ClassNotFoundException, it is useless
 * without knowing who was needing the component/class.  For this reason, it is
 * intentionally nested inside a ConstructionFailedException.
 *
 * In short:
 * NamedComponentNotFoundException -> what component could not be found
 * ConstructionFailedException -> what component failed to build as a result
 *
 * This test case ensures we tell that complete story
 */
public class NamedComponentNotFoundExceptionTest extends Assert {

    private NamedComponentNotFoundException namedComponentNotFoundException;
    private ConstructionFailedException constructionFailedException;

    @Before
    public void setup() {
        try {
            final Properties properties = new Properties();
            properties.put("thing", "new://" + NamedComponentNotFound.class.getName());
            properties.put("thing.dependent", "@joe");
            new System(properties);
            throw new AssertionError("ConstructionFailedException should have been thrown");
        } catch (ConstructionFailedException e) {
            assertNotNull(e.getCause());
            this.constructionFailedException = e;
            assertTrue(e.getCause() instanceof NamedComponentNotFoundException);
            this.namedComponentNotFoundException = (NamedComponentNotFoundException) e.getCause();
        }
    }

    @Test
    public void testGetMessage() throws Exception {
        assertEquals("Unable to find component with name 'joe'" +
                " and type org.tomitribe.pixie.comp.NamedComponentNotFoundExceptionTest$Dependent",
                namedComponentNotFoundException.getMessage());
    }

    @Test
    public void testGetOptions() throws Exception {
//        assertEquals(Join.join("\n", "color", "shape"), Join.join("\n", e.getOptions()));
    }

    @Test
    public void testConstructionFailedGetComponent() throws Exception {
        assertEquals(NamedComponentNotFound.class, constructionFailedException.getComponent());
    }

    @Test
    public void testGetComponent() throws Exception {
        assertEquals(Dependent.class, namedComponentNotFoundException.getComponent());
    }

    public static class NamedComponentNotFound {

        public NamedComponentNotFound(
                final @Name String name,
                final @Component("dependent") Dependent dependent
        ) {
        }
    }

    public static class Dependent {

    }
}