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

import java.util.List;
import java.util.Properties;

/**
 * Tests that generic type arguments inside collection element types
 * are used to narrow which components are collected for injection.
 *
 * For example, List<RequestHandler<String, Integer>> should only
 * collect RequestHandler implementations with matching type arguments.
 */
public class ComponentGenericsCollectionTest extends Assert {

    // -----------------------------------------------------------------------
    // Collection — exact generic element type
    // -----------------------------------------------------------------------

    @Test
    public void collectsOnlyMatchingGenericType() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExactCollectionConsumer.class.getName());
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());
        properties.put("intString", "new://" + IntStringHandler.class.getName());

        final System system = new System(properties);
        final ExactCollectionConsumer consumer = system.get(ExactCollectionConsumer.class);

        assertEquals(1, consumer.getHandlers().size());
        assertTrue(consumer.getHandlers().get(0) instanceof StringIntHandler);
    }

    @Test
    public void collectsNoneWhenNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExactCollectionConsumer.class.getName());
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final ExactCollectionConsumer consumer = system.get(ExactCollectionConsumer.class);

        assertEquals(0, consumer.getHandlers().size());
    }

    // -----------------------------------------------------------------------
    // Collection — extends wildcard element type
    // -----------------------------------------------------------------------

    @Test
    public void extendsWildcardCollectsMatching() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExtendsCollectionConsumer.class.getName());
        // IntStringHandler has <Integer, String> — Integer extends Number ✓
        properties.put("intString", "new://" + IntStringHandler.class.getName());
        // StringIntHandler has <String, Integer> — String does NOT extend Number ✗
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());
        // LongBoolHandler has <Long, Boolean> — Long extends Number ✓
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final ExtendsCollectionConsumer consumer = system.get(ExtendsCollectionConsumer.class);

        assertEquals(2, consumer.getHandlers().size());
        assertTrue(consumer.getHandlers().stream().anyMatch(h -> h instanceof IntStringHandler));
        assertTrue(consumer.getHandlers().stream().anyMatch(h -> h instanceof LongBoolHandler));
    }

    // -----------------------------------------------------------------------
    // Collection — super wildcard element type
    // -----------------------------------------------------------------------

    @Test
    public void superWildcardCollectsMatching() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperCollectionConsumer.class.getName());
        // NumberBoolHandler has <Number, Boolean> — Number is supertype of Integer ✓
        properties.put("numBool", "new://" + NumberBoolHandler.class.getName());
        // StringIntHandler has <String, Integer> — String is NOT supertype of Integer ✗
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());

        final System system = new System(properties);
        final SuperCollectionConsumer consumer = system.get(SuperCollectionConsumer.class);

        assertEquals(1, consumer.getHandlers().size());
        assertTrue(consumer.getHandlers().get(0) instanceof NumberBoolHandler);
    }

    // -----------------------------------------------------------------------
    // Collection — unbounded wildcard element type
    // -----------------------------------------------------------------------

    @Test
    public void unboundedWildcardCollectsAll() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + UnboundedCollectionConsumer.class.getName());
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());
        properties.put("intString", "new://" + IntStringHandler.class.getName());

        final System system = new System(properties);
        final UnboundedCollectionConsumer consumer = system.get(UnboundedCollectionConsumer.class);

        assertEquals(3, consumer.getHandlers().size());
    }

    // -----------------------------------------------------------------------
    // Collection — raw element type (backwards compatibility)
    // -----------------------------------------------------------------------

    @Test
    public void rawCollectionCollectsAll() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + RawCollectionConsumer.class.getName());
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final RawCollectionConsumer consumer = system.get(RawCollectionConsumer.class);

        assertEquals(2, consumer.getHandlers().size());
    }

    // -----------------------------------------------------------------------
    // Collection — multi-level interface inheritance
    // -----------------------------------------------------------------------

    @Test
    public void collectionMatchesThroughInterfaceHierarchy() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExactCollectionConsumer.class.getName());
        // ImprovedStringIntHandler implements ImprovedHandler<String, Integer>
        // which extends RequestHandler<String, Integer>
        properties.put("improved", "new://" + ImprovedStringIntHandler.class.getName());
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final ExactCollectionConsumer consumer = system.get(ExactCollectionConsumer.class);

        assertEquals(1, consumer.getHandlers().size());
        assertTrue(consumer.getHandlers().get(0) instanceof ImprovedStringIntHandler);
    }

    // =======================================================================
    // Fixtures — RequestHandler hierarchy
    // =======================================================================

    public interface RequestHandler<I, O> {
        O handle(I input);
    }

    public interface ImprovedHandler<I, O> extends RequestHandler<I, O> {
    }

    public static class StringIntHandler implements RequestHandler<String, Integer> {
        public StringIntHandler(final @Name String name) {
        }

        @Override
        public Integer handle(final String input) {
            return input.length();
        }
    }

    public static class LongBoolHandler implements RequestHandler<Long, Boolean> {
        public LongBoolHandler(final @Name String name) {
        }

        @Override
        public Boolean handle(final Long input) {
            return input > 0;
        }
    }

    public static class IntStringHandler implements RequestHandler<Integer, String> {
        public IntStringHandler(final @Name String name) {
        }

        @Override
        public String handle(final Integer input) {
            return input.toString();
        }
    }

    public static class NumberBoolHandler implements RequestHandler<Number, Boolean> {
        public NumberBoolHandler(final @Name String name) {
        }

        @Override
        public Boolean handle(final Number input) {
            return input.intValue() > 0;
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

    // =======================================================================
    // Fixtures — Consumer classes
    // =======================================================================

    public static class ExactCollectionConsumer {
        private final List<RequestHandler<String, Integer>> handlers;

        public ExactCollectionConsumer(
                final @Name String name,
                final @Param("handlers") @Component List<RequestHandler<String, Integer>> handlers
        ) {
            this.handlers = handlers;
        }

        public List<RequestHandler<String, Integer>> getHandlers() {
            return handlers;
        }
    }

    public static class ExtendsCollectionConsumer {
        private final List<RequestHandler<? extends Number, ?>> handlers;

        public ExtendsCollectionConsumer(
                final @Name String name,
                final @Param("handlers") @Component List<RequestHandler<? extends Number, ?>> handlers
        ) {
            this.handlers = handlers;
        }

        public List<RequestHandler<? extends Number, ?>> getHandlers() {
            return handlers;
        }
    }

    public static class SuperCollectionConsumer {
        private final List<RequestHandler<? super Integer, ?>> handlers;

        public SuperCollectionConsumer(
                final @Name String name,
                final @Param("handlers") @Component List<RequestHandler<? super Integer, ?>> handlers
        ) {
            this.handlers = handlers;
        }

        public List<RequestHandler<? super Integer, ?>> getHandlers() {
            return handlers;
        }
    }

    public static class UnboundedCollectionConsumer {
        private final List<RequestHandler<?, ?>> handlers;

        public UnboundedCollectionConsumer(
                final @Name String name,
                final @Param("handlers") @Component List<RequestHandler<?, ?>> handlers
        ) {
            this.handlers = handlers;
        }

        public List<RequestHandler<?, ?>> getHandlers() {
            return handlers;
        }
    }

    @SuppressWarnings("rawtypes")
    public static class RawCollectionConsumer {
        private final List<RequestHandler> handlers;

        public RawCollectionConsumer(
                final @Name String name,
                final @Param("handlers") @Component List<RequestHandler> handlers
        ) {
            this.handlers = handlers;
        }

        public List<RequestHandler> getHandlers() {
            return handlers;
        }
    }
}
