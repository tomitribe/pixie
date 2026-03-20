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
import org.junit.Test;
import org.tomitribe.pixie.Component;
import org.tomitribe.pixie.Factory;
import org.tomitribe.pixie.Name;
import org.tomitribe.pixie.Param;
import org.tomitribe.pixie.System;

import java.util.Properties;

/**
 * Tests that factory method declarations correctly expose their
 * generic return type for matching. Covers basic generic returns,
 * bounded type parameters, and multi-level inheritance in the
 * factory-produced type.
 */
public class ComponentProducerGenericFactoryTest extends Assert {

    // -----------------------------------------------------------------------
    // Basic generic return type
    // -----------------------------------------------------------------------

    @Test
    public void factoryGenericReturnMatchesConsumer() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + HandlerConsumer.class.getName());
        properties.put("handler", "new://" + StringIntFactory.class.getName());

        final System system = new System(properties);
        final HandlerConsumer consumer = system.get(HandlerConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.getHandler());
        assertEquals(42, (int) consumer.getHandler().handle("hello"));
    }

    @Test(expected = ConstructionFailedException.class)
    public void factoryGenericReturnRejectsWrongConsumer() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + HandlerConsumer.class.getName());
        properties.put("handler", "new://" + LongBoolFactory.class.getName());

        new System(properties);
    }

    // -----------------------------------------------------------------------
    // Bounded type parameter
    // -----------------------------------------------------------------------

    @Test
    public void factoryWithBoundedTypeParameter() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SorterConsumer.class.getName());
        properties.put("sorter", "new://" + StringSorterFactory.class.getName());

        final System system = new System(properties);
        final SorterConsumer consumer = system.get(SorterConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.getSorter());
    }

    @Test(expected = ConstructionFailedException.class)
    public void factoryWithBoundedTypeParameterNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SorterConsumer.class.getName());
        properties.put("sorter", "new://" + IntSorterFactory.class.getName());

        new System(properties);
    }

    // -----------------------------------------------------------------------
    // Factory returning a subtype that inherits generics
    // -----------------------------------------------------------------------

    @Test
    public void factoryReturningSubtypeWithInheritedGenerics() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + HandlerConsumer.class.getName());
        // Factory returns ImprovedHandler<String, Integer> which extends RequestHandler<String, Integer>
        properties.put("handler", "new://" + ImprovedHandlerFactory.class.getName());

        final System system = new System(properties);
        final HandlerConsumer consumer = system.get(HandlerConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.getHandler());
    }

    @Test(expected = ConstructionFailedException.class)
    public void factoryReturningSubtypeWithWrongGenerics() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + HandlerConsumer.class.getName());
        // Factory returns ImprovedHandler<Long, Boolean> — wrong generics
        properties.put("handler", "new://" + WrongImprovedHandlerFactory.class.getName());

        new System(properties);
    }

    // -----------------------------------------------------------------------
    // Bounded type parameter — multi-level (factory returns a deeper subtype)
    // -----------------------------------------------------------------------

    @Test
    public void factoryReturningSubtypeOfBoundedSuperclass() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SorterConsumer.class.getName());
        // Factory returns SpecialSorter<String> which extends Sorter<String>
        properties.put("sorter", "new://" + SpecialStringSorterFactory.class.getName());

        final System system = new System(properties);
        final SorterConsumer consumer = system.get(SorterConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.getSorter());
    }

    @Test(expected = ConstructionFailedException.class)
    public void factoryReturningSubtypeOfBoundedSuperclassNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SorterConsumer.class.getName());
        properties.put("sorter", "new://" + SpecialIntSorterFactory.class.getName());

        new System(properties);
    }

    // -----------------------------------------------------------------------
    // Mixed generics: some from method declaration, some from class hierarchy
    // -----------------------------------------------------------------------

    @Test
    public void factoryMixedGenericsMatch() throws Exception {
        final Properties properties = new Properties();
        // Consumer wants Handler<String, Boolean>
        // Factory returns BooleanHandler<String> where:
        //   String comes from factory return type
        //   Boolean comes from BooleanHandler's class hierarchy (implements Handler<I, Boolean>)
        properties.put("consumer", "new://" + MixedConsumer.class.getName());
        properties.put("handler", "new://" + StringBooleanHandlerFactory.class.getName());

        final System system = new System(properties);
        final MixedConsumer consumer = system.get(MixedConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.getHandler());
    }

    @Test(expected = ConstructionFailedException.class)
    public void factoryMixedGenericsNoMatch() throws Exception {
        final Properties properties = new Properties();
        // Consumer wants Handler<String, Boolean>
        // Factory returns BooleanHandler<Integer> — I=Integer doesn't match String
        properties.put("consumer", "new://" + MixedConsumer.class.getName());
        properties.put("handler", "new://" + IntBooleanHandlerFactory.class.getName());

        new System(properties);
    }

    @Test(expected = ConstructionFailedException.class)
    public void factoryMixedGenericsWrongFixedType() throws Exception {
        final Properties properties = new Properties();
        // Consumer wants Handler<String, Integer>
        // Factory returns BooleanHandler<String> — O=Boolean (from hierarchy) doesn't match Integer
        properties.put("consumer", "new://" + HandlerConsumer.class.getName());
        properties.put("handler", "new://" + StringBooleanHandlerFactory.class.getName());

        new System(properties);
    }

    // =======================================================================
    // Fixtures — RequestHandler
    // =======================================================================

    public interface RequestHandler<I, O> {
        O handle(I input);
    }

    public interface ImprovedHandler<I, O> extends RequestHandler<I, O> {
    }

    public static class StringIntFactory {
        private StringIntFactory() {
        }

        @Factory
        public static RequestHandler<String, Integer> create(final @Name String name) {
            return input -> 42;
        }
    }

    public static class LongBoolFactory {
        private LongBoolFactory() {
        }

        @Factory
        public static RequestHandler<Long, Boolean> create(final @Name String name) {
            return input -> true;
        }
    }

    public static class ImprovedHandlerFactory {
        private ImprovedHandlerFactory() {
        }

        @Factory
        public static ImprovedHandler<String, Integer> create(final @Name String name) {
            return input -> input.length();
        }
    }

    public static class WrongImprovedHandlerFactory {
        private WrongImprovedHandlerFactory() {
        }

        @Factory
        public static ImprovedHandler<Long, Boolean> create(final @Name String name) {
            return input -> true;
        }
    }

    // =======================================================================
    // Fixtures — Sorter with bounded type parameter
    // =======================================================================

    public static abstract class Sorter<T extends Comparable<T>> {
        public abstract T sort();
    }

    public static class StringSorterFactory {
        private StringSorterFactory() {
        }

        @Factory
        public static Sorter<String> create(final @Name String name) {
            return new Sorter<String>() {
                @Override
                public String sort() {
                    return "sorted";
                }
            };
        }
    }

    public static class IntSorterFactory {
        private IntSorterFactory() {
        }

        @Factory
        public static Sorter<Integer> create(final @Name String name) {
            return new Sorter<Integer>() {
                @Override
                public Integer sort() {
                    return 0;
                }
            };
        }
    }

    public static abstract class SpecialSorter<T extends Comparable<T>> extends Sorter<T> {
    }

    public static class SpecialStringSorterFactory {
        private SpecialStringSorterFactory() {
        }

        @Factory
        public static SpecialSorter<String> create(final @Name String name) {
            return new SpecialSorter<String>() {
                @Override
                public String sort() {
                    return "special-sorted";
                }
            };
        }
    }

    public static class SpecialIntSorterFactory {
        private SpecialIntSorterFactory() {
        }

        @Factory
        public static SpecialSorter<Integer> create(final @Name String name) {
            return new SpecialSorter<Integer>() {
                @Override
                public Integer sort() {
                    return 0;
                }
            };
        }
    }

    // =======================================================================
    // Fixtures — Mixed generics (partial binding through class hierarchy)
    // =======================================================================

    /**
     * BooleanHandler fixes O=Boolean but leaves I open.
     * The factory method's return type binds I.
     */
    public static abstract class BooleanHandler<I> implements RequestHandler<I, Boolean> {
    }

    public static class StringBooleanHandlerFactory {
        private StringBooleanHandlerFactory() {
        }

        @Factory
        public static BooleanHandler<String> create(final @Name String name) {
            return new BooleanHandler<String>() {
                @Override
                public Boolean handle(final String input) {
                    return input != null;
                }
            };
        }
    }

    public static class IntBooleanHandlerFactory {
        private IntBooleanHandlerFactory() {
        }

        @Factory
        public static BooleanHandler<Integer> create(final @Name String name) {
            return new BooleanHandler<Integer>() {
                @Override
                public Boolean handle(final Integer input) {
                    return input > 0;
                }
            };
        }
    }

    // =======================================================================
    // Fixtures — Consumers
    // =======================================================================

    public static class HandlerConsumer {
        private final RequestHandler<String, Integer> handler;

        public HandlerConsumer(
                final @Name String name,
                final @Param("handler") @Component RequestHandler<String, Integer> handler
        ) {
            this.handler = handler;
        }

        public RequestHandler<String, Integer> getHandler() {
            return handler;
        }
    }

    public static class SorterConsumer {
        private final Sorter<String> sorter;

        public SorterConsumer(
                final @Name String name,
                final @Param("sorter") @Component Sorter<String> sorter
        ) {
            this.sorter = sorter;
        }

        public Sorter<String> getSorter() {
            return sorter;
        }
    }

    public static class MixedConsumer {
        private final RequestHandler<String, Boolean> handler;

        public MixedConsumer(
                final @Name String name,
                final @Param("handler") @Component RequestHandler<String, Boolean> handler
        ) {
            this.handler = handler;
        }

        public RequestHandler<String, Boolean> getHandler() {
            return handler;
        }
    }
}
