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

public class ConstructionFailedExceptionTest extends Assert {

    private ConstructionFailedException e;

    @Before
    public void setup() {
        try {

            final System system = new System(new Properties());
            system.get(ConstructionFailure.class);

            throw new AssertionError("ConstructionFailedException should have been thrown");
        } catch (ConstructionFailedException e) {
            this.e = e;
        }
    }

    @Test
    public void testGetMessage() throws Exception {
        assertEquals("Unable to construct component: class " +
                "org.tomitribe.pixie.comp.ConstructionFailedExceptionTest$ConstructionFailure" +
                " - It's bad", e.getMessage());
    }

    @Test
    public void testGetComponent() throws Exception {
        assertEquals(ConstructionFailure.class, e.getComponent());
    }

    @Test
    public void testGetCause() throws Exception {
        assertTrue(e.getCause() instanceof SomethingIsNotRightException);
    }

    public static class ConstructionFailure {

        public ConstructionFailure(final @Name String name) {
            throw new SomethingIsNotRightException("It's bad");
        }
    }

    public static class SomethingIsNotRightException extends RuntimeException {
        public SomethingIsNotRightException(final String message) {
            super(message);
        }
    }
}