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
import org.tomitribe.pixie.Builder;
import org.tomitribe.pixie.Component;
import org.tomitribe.pixie.Factory;
import org.tomitribe.pixie.Name;
import org.tomitribe.pixie.Nullable;
import org.tomitribe.pixie.Param;
import org.tomitribe.pixie.System;

import java.util.List;
import java.util.Properties;

/**
 * Tests generic type matching for collection @Component refs resolved
 * by type AND name (named refs like @ref1 @ref2 in properties).
 *
 * Mirrors ComponentRefGenericCollectionByTypeTest for all generic flavors,
 * plus name-specific scenarios.
 */
public class ComponentRefGenericCollectionByTypeAndNameTest extends Assert {

    // -----------------------------------------------------------------------
    // Exact type — named collection
    // -----------------------------------------------------------------------

    @Test
    public void exactMatchCollectsNamed() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExactConsumer.class.getName());
        properties.put("consumer.handlers", "@stringInt");
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final ExactConsumer consumer = system.get(ExactConsumer.class);
        assertEquals(1, consumer.getHandlers().size());
        assertTrue(consumer.getHandlers().get(0) instanceof StringIntHandler);
    }

    @Test(expected = ConstructionFailedException.class)
    public void exactMatchRejectsWrongGenericByName() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExactConsumer.class.getName());
        properties.put("consumer.handlers", "@longBool");
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        new System(properties);
    }

    @Test
    public void exactMatchCollectsMultipleNamed() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExactConsumer.class.getName());
        properties.put("consumer.handlers", "@first @second");
        properties.put("first", "new://" + StringIntHandler.class.getName());
        properties.put("second", "new://" + AnotherStringIntHandler.class.getName());

        final System system = new System(properties);
        final ExactConsumer consumer = system.get(ExactConsumer.class);
        assertEquals(2, consumer.getHandlers().size());
    }

    // -----------------------------------------------------------------------
    // Extends wildcard — named collection
    // -----------------------------------------------------------------------

    @Test
    public void extendsMatchCollectsNamed() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExtendsConsumer.class.getName());
        // IntStringHandler has <Integer, String> — Integer extends Number ✓
        properties.put("consumer.handlers", "@intString");
        properties.put("intString", "new://" + IntStringHandler.class.getName());

        final System system = new System(properties);
        final ExtendsConsumer consumer = system.get(ExtendsConsumer.class);
        assertEquals(1, consumer.getHandlers().size());
        assertTrue(consumer.getHandlers().get(0) instanceof IntStringHandler);
    }

    @Test(expected = ConstructionFailedException.class)
    public void extendsRejectsWrongGenericByName() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExtendsConsumer.class.getName());
        // StringIntHandler has <String, Integer> — String does NOT extend Number ✗
        properties.put("consumer.handlers", "@stringInt");
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());

        new System(properties);
    }

    // -----------------------------------------------------------------------
    // Super wildcard — named collection
    // -----------------------------------------------------------------------

    @Test
    public void superMatchCollectsNamed() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperConsumer.class.getName());
        // NumberBoolHandler has <Number, Boolean> — Number is supertype of Integer ✓
        properties.put("consumer.handlers", "@numBool");
        properties.put("numBool", "new://" + NumberBoolHandler.class.getName());

        final System system = new System(properties);
        final SuperConsumer consumer = system.get(SuperConsumer.class);
        assertEquals(1, consumer.getHandlers().size());
        assertTrue(consumer.getHandlers().get(0) instanceof NumberBoolHandler);
    }

    @Test(expected = ConstructionFailedException.class)
    public void superRejectsWrongGenericByName() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperConsumer.class.getName());
        properties.put("consumer.handlers", "@stringInt");
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());

        new System(properties);
    }

    // -----------------------------------------------------------------------
    // Unbounded wildcard — named collection
    // -----------------------------------------------------------------------

    @Test
    public void unboundedMatchesAnyByName() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + UnboundedConsumer.class.getName());
        properties.put("consumer.handlers", "@longBool @stringInt");
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());

        final System system = new System(properties);
        final UnboundedConsumer consumer = system.get(UnboundedConsumer.class);
        assertEquals(2, consumer.getHandlers().size());
    }

    // -----------------------------------------------------------------------
    // Raw — named collection
    // -----------------------------------------------------------------------

    @Test
    public void rawMatchesAnyByName() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + RawConsumer.class.getName());
        properties.put("consumer.handlers", "@longBool");
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final RawConsumer consumer = system.get(RawConsumer.class);
        assertEquals(1, consumer.getHandlers().size());
    }

    // -----------------------------------------------------------------------
    // Multi-level interface hierarchy — named collection
    // -----------------------------------------------------------------------

    @Test
    public void multiLevelInterfaceByName() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExactConsumer.class.getName());
        properties.put("consumer.handlers", "@improved");
        properties.put("improved", "new://" + ImprovedStringIntHandler.class.getName());

        final System system = new System(properties);
        final ExactConsumer consumer = system.get(ExactConsumer.class);
        assertEquals(1, consumer.getHandlers().size());
        assertTrue(consumer.getHandlers().get(0) instanceof ImprovedStringIntHandler);
    }

    // -----------------------------------------------------------------------
    // All declaration types in one named collection
    // -----------------------------------------------------------------------

    @Test
    public void collectsByNameFromAllDeclarationTypes() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExactConsumer.class.getName());
        properties.put("consumer.handlers", "@declared @factoryMade @built");
        properties.put("declared", "new://" + StringIntHandler.class.getName());
        properties.put("factoryMade", "new://" + FactoryHandlerHost.class.getName());
        properties.put("built", "new://" + BuilderHandlerHost.class.getName());

        final System system = new System(properties);
        final ExactConsumer consumer = system.get(ExactConsumer.class);
        assertEquals(3, consumer.getHandlers().size());
    }

    @Test
    public void collectsByNameIncludingPreBuiltInstance() throws Exception {
        final StringIntHandler prebuilt = new StringIntHandler("prebuilt");

        final System system = System.builder()
                .add("prebuilt", prebuilt)
                .definition(ExactConsumer.class, "consumer")
                .comp("handlers", "prebuilt")
                .build();

        final ExactConsumer consumer = system.get(ExactConsumer.class);
        assertEquals(1, consumer.getHandlers().size());
        assertSame(prebuilt, consumer.getHandlers().get(0));
    }

    /**
     * All four producer types (Instance, Constructor, Factory, Builder)
     * collected into one named list.
     */
    @Test
    public void collectsByNameFromAllProducerTypes() throws Exception {
        final StringIntHandler prebuilt = new StringIntHandler("prebuilt");

        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExactConsumer.class.getName());
        properties.put("consumer.handlers", "@prebuilt @declared @factoryMade @built");
        properties.put("declared", "new://" + AnotherStringIntHandler.class.getName());
        properties.put("factoryMade", "new://" + FactoryHandlerHost.class.getName());
        properties.put("built", "new://" + BuilderHandlerHost.class.getName());

        // Add the pre-built instance before load so it's available for named resolution
        final System system = new System();
        system.add("prebuilt", prebuilt);
        system.load(properties);

        final ExactConsumer consumer = system.get(ExactConsumer.class);
        assertEquals(4, consumer.getHandlers().size());
    }

    // -----------------------------------------------------------------------
    // Name-specific: correct generics but wrong name
    // -----------------------------------------------------------------------

    @Test(expected = ConstructionFailedException.class)
    public void correctGenericButWrongName() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExactConsumer.class.getName());
        properties.put("consumer.handlers", "@nonexistent");
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());

        new System(properties);
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

    public static class AnotherStringIntHandler implements RequestHandler<String, Integer> {
        public AnotherStringIntHandler(final @Name String name) {
        }

        @Override
        public Integer handle(final String input) {
            return input.length() * 2;
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

    // Factory & Builder for unified test

    public static class FactoryHandlerHost {
        private FactoryHandlerHost() {
        }

        @Factory
        public static RequestHandler<String, Integer> create(final @Name String name) {
            return input -> input.length();
        }
    }

    public static class GenericHandlerBuilder<I, O> {
        private String label;

        public GenericHandlerBuilder<I, O> label(@Param("label") @Nullable final String label) {
            this.label = label;
            return this;
        }

        public RequestHandler<I, O> build() {
            return (I input) -> null;
        }
    }

    public static class BuilderHandlerHost {
        private BuilderHandlerHost() {
        }

        @Builder
        public static GenericHandlerBuilder<String, Integer> builder() {
            return new GenericHandlerBuilder<>();
        }
    }

    // =======================================================================
    // Fixtures — Consumer classes
    // =======================================================================

    public static class ExactConsumer {
        private final List<RequestHandler<String, Integer>> handlers;

        public ExactConsumer(
                final @Name String name,
                final @Param("handlers") @Component List<RequestHandler<String, Integer>> handlers
        ) {
            this.handlers = handlers;
        }

        public List<RequestHandler<String, Integer>> getHandlers() {
            return handlers;
        }
    }

    public static class ExtendsConsumer {
        private final List<RequestHandler<? extends Number, ?>> handlers;

        public ExtendsConsumer(
                final @Name String name,
                final @Param("handlers") @Component List<RequestHandler<? extends Number, ?>> handlers
        ) {
            this.handlers = handlers;
        }

        public List<RequestHandler<? extends Number, ?>> getHandlers() {
            return handlers;
        }
    }

    public static class SuperConsumer {
        private final List<RequestHandler<? super Integer, ?>> handlers;

        public SuperConsumer(
                final @Name String name,
                final @Param("handlers") @Component List<RequestHandler<? super Integer, ?>> handlers
        ) {
            this.handlers = handlers;
        }

        public List<RequestHandler<? super Integer, ?>> getHandlers() {
            return handlers;
        }
    }

    public static class UnboundedConsumer {
        private final List<RequestHandler<?, ?>> handlers;

        public UnboundedConsumer(
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
    public static class RawConsumer {
        private final List<RequestHandler> handlers;

        public RawConsumer(
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
