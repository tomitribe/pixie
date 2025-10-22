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
import org.junit.Before;
import org.junit.Test;
import org.tomitribe.pixie.Component;
import org.tomitribe.pixie.Default;
import org.tomitribe.pixie.Name;
import org.tomitribe.pixie.Param;
import org.tomitribe.pixie.System;

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
                "    @Param(\"red\") String\n" +
                "    /* missing */ boolean\n" +
                "    @Component @Param(\"green\") URI\n" +
                "    @Param(\"blue\") @Default(\"navy\") String\n" +
                "    @Component @Param(\"unit\") @Default(\"MINUTES\") TimeUnit\n" +
                "    /* missing */ int\n" +
                "  )\n" +
                "  Add @Component, @Param or @Name", e.getMessage());
    }

    /**
     * @Since 3.0
     * This test verifies that when a constructor parameter is annotated with @Component
     * but missing the required @Param annotation, a ConstructionFailedException is thrown.
     * The @Param annotation is required to provide a name for the component reference.
     */
    @Test
    public void testWrongComponentDeclaration() throws Exception {
        final System system = new System(new Properties());
        ConstructionFailedException e = assertThrows(
                ConstructionFailedException.class,
                () -> system.get(InvalidComponentConstructor.class)
        );
        e.printStackTrace();
    }

    @Test
    public void testGetComponent() throws Exception {
        assertEquals(InvalidConstructorParameter.class, e.getComponent());
    }

    public static class InvalidConstructorParameter {

        public InvalidConstructorParameter(
                @Name String name,
                @Param("red") String red,
                boolean fun,
                @Param("green") @Component URI green,
                @Param("blue") @Default("navy") String blue,
                @Param("unit") @Component @Default("MINUTES") TimeUnit unit,
                int notAnnotated) {
        }
    }


    public static class InvalidComponentConstructor {
        public  InvalidComponentConstructor(
                @Name String name,
                @Param("red") String red,
                @Component() URI green,
                @Param("blue") @Default("navy") String blue){
        }
    }
}