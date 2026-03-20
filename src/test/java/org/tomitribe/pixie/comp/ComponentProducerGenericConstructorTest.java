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
import org.tomitribe.pixie.Name;
import org.tomitribe.pixie.Param;
import org.tomitribe.pixie.System;

import java.util.Properties;

/**
 * Tests that constructor-based declarations correctly expose their
 * generic type information for matching. Covers bounded type parameters
 * on superclasses and multi-level type variable forwarding.
 */
public class ComponentProducerGenericConstructorTest extends Assert {

    // -----------------------------------------------------------------------
    // Basic generic match (non-bounded)
    // -----------------------------------------------------------------------

    @Test
    public void basicMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + HandlerConsumer.class.getName());
        properties.put("handler", "new://" + StringIntHandler.class.getName());

        final System system = new System(properties);
        final HandlerConsumer consumer = system.get(HandlerConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof StringIntHandler);
    }

    @Test(expected = ConstructionFailedException.class)
    public void basicNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + HandlerConsumer.class.getName());
        properties.put("handler", "new://" + LongBoolHandler.class.getName());

        new System(properties);
    }

    // -----------------------------------------------------------------------
    // Subtype with inherited generics
    // -----------------------------------------------------------------------

    @Test
    public void subtypeWithInheritedGenerics() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + HandlerConsumer.class.getName());
        // ImprovedStringIntHandler implements ImprovedHandler<String, Integer>
        // which extends Handler<String, Integer>
        properties.put("handler", "new://" + ImprovedStringIntHandler.class.getName());

        final System system = new System(properties);
        final HandlerConsumer consumer = system.get(HandlerConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof ImprovedStringIntHandler);
    }

    @Test(expected = ConstructionFailedException.class)
    public void subtypeWithWrongInheritedGenerics() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + HandlerConsumer.class.getName());
        properties.put("handler", "new://" + ImprovedLongBoolHandler.class.getName());

        new System(properties);
    }

    // -----------------------------------------------------------------------
    // Bounded type parameter
    // -----------------------------------------------------------------------

    @Test
    public void boundedTypeParameterMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ComparableConsumer.class.getName());
        properties.put("sorter", "new://" + StringSorter.class.getName());

        final System system = new System(properties);
        final ComparableConsumer consumer = system.get(ComparableConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getSorter() instanceof StringSorter);
    }

    @Test(expected = ConstructionFailedException.class)
    public void boundedTypeParameterNoMatch() throws Exception {
        final Properties properties = new Properties();
        // Consumer wants Sorter<String>, IntSorter is Sorter<Integer>
        properties.put("consumer", "new://" + ComparableConsumer.class.getName());
        properties.put("sorter", "new://" + IntSorter.class.getName());

        new System(properties);
    }

    @Test
    public void multiLevelBoundedTypeVariable() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ComparableConsumer.class.getName());
        // SpecialStringSorter extends StringSorter extends Sorter<String>
        properties.put("sorter", "new://" + SpecialStringSorter.class.getName());

        final System system = new System(properties);
        final ComparableConsumer consumer = system.get(ComparableConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getSorter() instanceof SpecialStringSorter);
    }

    @Test
    public void deferredBoundedTypeVariable() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ComparableConsumer.class.getName());
        // DeferredStringSorter extends AbstractSorter<String>
        // where AbstractSorter<T extends Comparable<T>> extends Sorter<T>
        properties.put("sorter", "new://" + DeferredStringSorter.class.getName());

        final System system = new System(properties);
        final ComparableConsumer consumer = system.get(ComparableConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getSorter() instanceof DeferredStringSorter);
    }

    // -----------------------------------------------------------------------
    // Mixed generics: some from subclass, some from class hierarchy
    // -----------------------------------------------------------------------

    @Test
    public void mixedGenericsMatch() throws Exception {
        final Properties properties = new Properties();
        // Consumer wants Handler<String, Boolean>
        // StringBooleanHandler extends BooleanHandler<String>
        //   where BooleanHandler<I> implements Handler<I, Boolean>
        //   String from subclass, Boolean from BooleanHandler hierarchy
        properties.put("consumer", "new://" + MixedConsumer.class.getName());
        properties.put("handler", "new://" + StringBooleanHandler.class.getName());

        final System system = new System(properties);
        final MixedConsumer consumer = system.get(MixedConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof StringBooleanHandler);
    }

    @Test(expected = ConstructionFailedException.class)
    public void mixedGenericsNoMatch() throws Exception {
        final Properties properties = new Properties();
        // Consumer wants Handler<String, Boolean>
        // IntBooleanHandler extends BooleanHandler<Integer> — I=Integer doesn't match String
        properties.put("consumer", "new://" + MixedConsumer.class.getName());
        properties.put("handler", "new://" + IntBooleanHandler.class.getName());

        new System(properties);
    }

    @Test(expected = ConstructionFailedException.class)
    public void mixedGenericsWrongFixedType() throws Exception {
        final Properties properties = new Properties();
        // Consumer wants Handler<String, Integer>
        // StringBooleanHandler has Handler<String, Boolean> — O=Boolean doesn't match Integer
        properties.put("consumer", "new://" + HandlerIntConsumer.class.getName());
        properties.put("handler", "new://" + StringBooleanHandler.class.getName());

        new System(properties);
    }

    // =======================================================================
    // Fixtures — Sorter with bounded type parameter
    // =======================================================================

    public static abstract class Sorter<T extends Comparable<T>> {
        public Sorter(final @Name String name) {
        }

        public abstract T sort();
    }

    public static abstract class AbstractSorter<T extends Comparable<T>> extends Sorter<T> {
        public AbstractSorter(final @Name String name) {
            super(name);
        }
    }

    public static class StringSorter extends Sorter<String> {
        public StringSorter(final @Name String name) {
            super(name);
        }

        @Override
        public String sort() {
            return "sorted";
        }
    }

    public static class IntSorter extends Sorter<Integer> {
        public IntSorter(final @Name String name) {
            super(name);
        }

        @Override
        public Integer sort() {
            return 0;
        }
    }

    public static class SpecialStringSorter extends StringSorter {
        public SpecialStringSorter(final @Name String name) {
            super(name);
        }
    }

    public static class DeferredStringSorter extends AbstractSorter<String> {
        public DeferredStringSorter(final @Name String name) {
            super(name);
        }

        @Override
        public String sort() {
            return "deferred-sorted";
        }
    }

    // =======================================================================
    // Fixtures — Mixed generics (partial binding through class hierarchy)
    // =======================================================================

    public interface Handler<I, O> {
        O handle(I input);
    }

    public interface ImprovedHandler<I, O> extends Handler<I, O> {
    }

    public static class StringIntHandler implements Handler<String, Integer> {
        public StringIntHandler(final @Name String name) {
        }

        @Override
        public Integer handle(final String input) {
            return input.length();
        }
    }

    public static class LongBoolHandler implements Handler<Long, Boolean> {
        public LongBoolHandler(final @Name String name) {
        }

        @Override
        public Boolean handle(final Long input) {
            return input > 0;
        }
    }

    public static class ImprovedStringIntHandler implements ImprovedHandler<String, Integer> {
        public ImprovedStringIntHandler(final @Name String name) {
        }

        @Override
        public Integer handle(final String input) {
            return input.length();
        }
    }

    public static class ImprovedLongBoolHandler implements ImprovedHandler<Long, Boolean> {
        public ImprovedLongBoolHandler(final @Name String name) {
        }

        @Override
        public Boolean handle(final Long input) {
            return input > 0;
        }
    }

    public static abstract class BooleanHandler<I> implements Handler<I, Boolean> {
        public BooleanHandler(final @Name String name) {
        }
    }

    public static class StringBooleanHandler extends BooleanHandler<String> {
        public StringBooleanHandler(final @Name String name) {
            super(name);
        }

        @Override
        public Boolean handle(final String input) {
            return input != null;
        }
    }

    public static class IntBooleanHandler extends BooleanHandler<Integer> {
        public IntBooleanHandler(final @Name String name) {
            super(name);
        }

        @Override
        public Boolean handle(final Integer input) {
            return input > 0;
        }
    }

    // =======================================================================
    // Fixtures — Consumer
    // =======================================================================

    public static class ComparableConsumer {
        private final Sorter<String> sorter;

        public ComparableConsumer(
                final @Name String name,
                final @Param("sorter") @Component Sorter<String> sorter
        ) {
            this.sorter = sorter;
        }

        public Sorter<String> getSorter() {
            return sorter;
        }
    }

    public static class HandlerConsumer {
        private final Handler<String, Integer> handler;

        public HandlerConsumer(
                final @Name String name,
                final @Param("handler") @Component Handler<String, Integer> handler
        ) {
            this.handler = handler;
        }

        public Handler<String, Integer> getHandler() {
            return handler;
        }
    }

    public static class MixedConsumer {
        private final Handler<String, Boolean> handler;

        public MixedConsumer(
                final @Name String name,
                final @Param("handler") @Component Handler<String, Boolean> handler
        ) {
            this.handler = handler;
        }

        public Handler<String, Boolean> getHandler() {
            return handler;
        }
    }

    public static class HandlerIntConsumer {
        private final Handler<String, Integer> handler;

        public HandlerIntConsumer(
                final @Name String name,
                final @Param("handler") @Component Handler<String, Integer> handler
        ) {
            this.handler = handler;
        }

        public Handler<String, Integer> getHandler() {
            return handler;
        }
    }
}
