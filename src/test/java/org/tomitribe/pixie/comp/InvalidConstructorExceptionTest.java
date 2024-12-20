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
import java.util.concurrent.TimeUnit;

public class InvalidConstructorExceptionTest extends Assert {

    private InvalidConstructorException e;

    @Before
    public void setup() {
        try {
            final System system = new System(new Properties());
            system.get(InvalidConstructorParameter.class);
            throw new AssertionError("ConstructionFailedException should have been thrown");
        } catch (ConstructionFailedException e) {
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof InvalidConstructorException);
            this.e = (InvalidConstructorException) e.getCause();
        }
    }

    @Test
    public void testGetMessage() throws Exception {
        assertEquals("Constructor is missing 2 parameter annotations\n" +
                "  org.tomitribe.pixie.comp.InvalidConstructorExceptionTest$InvalidConstructorParameter\n" +
                "  InvalidConstructorParameter(\n" +
                "    @Name String\n" +
                "    @Option(\"red\") String\n" +
                "    /* missing */ boolean\n" +
                "    @Component(\"green\") URI\n" +
                "    @Option(\"blue\") @Default(\"navy\") String\n" +
                "    @Component(\"unit\") @Default(\"MINUTES\") TimeUnit\n" +
                "    /* missing */ int\n" +
                "  )\n" +
                "  Add @Component, @Option or @Name", e.getMessage());
    }

    @Test
    public void testGetComponent() throws Exception {
        assertEquals(InvalidConstructorParameter.class, e.getComponent());
    }

    public static class InvalidConstructorParameter {

        public InvalidConstructorParameter(
                @Name String name,
                @Option("red") String red,
                boolean fun,
                @Component("green") URI green,
                @Option("blue") @Default("navy") String blue,
                @Component("unit") @Default("MINUTES") TimeUnit unit,
                int notAnnotated) {
        }
    }
}