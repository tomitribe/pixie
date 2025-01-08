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

import org.tomitribe.pixie.Event;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class EventReferencesTest extends Assert {

    @Test
    public void happyPath() throws Exception {

        final Class clazz = get(ValidReference.class);

        assertEquals(Date.class, clazz);
    }

    @Test
    public void missingGeneric() throws Exception {

        try {
            get(MissingGeneric.class);
            fail();
        } catch (MissingEventTypeException e) {
            assertEquals("Reference '@Event Consumer arg0' requires a generic type", e.getMessage());
            assertNotNull(e.getParameter());
        }
    }

    @Test
    public void usesWildcard() throws Exception {

        try {
            get(UsesWildcard.class);
            fail();
        } catch (WildcardEventTypeException e) {
            assertEquals("Reference '@Event Consumer<?> arg0' requires a generic type: wildcard found", e.getMessage());
            assertNotNull(e.getParameter());
        }
    }

    @Test
    public void usesGeneric() throws Exception {

        try {
            get(UsesGeneric.class);
            fail();
        } catch (NonClassEventTypeException e) {
            assertEquals("Reference '@Event Consumer<G> arg0' requires a generic type:" +
                    " non-class type 'sun.reflect.generics.reflectiveObjects.TypeVariableImpl' found",
                    e.getMessage());
            assertNotNull(e.getParameter());
        }
    }

    @Test
    public void wrongType() throws Exception {

        try {
            get(WrongArgumentType.class);
            fail();
        } catch (InvalidEventParameterTypeException e) {
            assertEquals("Reference '@Event Supplier arg0' is invalid.  " +
                            "Use '@Event java.util.function.Consumer<Foo> arg0'",
                    e.getMessage());
            assertNotNull(e.getParameter());
        }
    }


    private static Class get(final Class<?> type) {
        final Constructor<?> constructor = type.getConstructors()[0];
        final Parameter parameter = constructor.getParameters()[0];
        return EventReferences.eventType(parameter);
    }

    public static class ValidReference {
        public ValidReference(@Event Consumer<Date> consumer) {
        }
    }

    public static class MissingGeneric {
        public MissingGeneric(@Event Consumer consumer) {
        }
    }

    public static class UsesWildcard {
        public UsesWildcard(@Event Consumer<?> consumer) {
        }
    }

    public static class UsesGeneric<G> {
        public UsesGeneric(@Event Consumer<G> consumer) {
        }
    }

    public static class WrongArgumentType {
        public WrongArgumentType(@Event Supplier<Date> supplier) {
        }
    }
}