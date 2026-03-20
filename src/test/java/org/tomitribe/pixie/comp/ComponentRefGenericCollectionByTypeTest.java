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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tests generic type matching for collection @Component refs resolved by type.
 * Verifies that List<RequestHandler<...>> only collects matching implementations.
 *
 * Also verifies that all producer types (Instance, Constructor, Factory, Builder)
 * are collected uniformly into a single list.
 */
public class ComponentRefGenericCollectionByTypeTest extends Assert {

    // -----------------------------------------------------------------------
    // Exact, extends, super, unbounded, raw
    // -----------------------------------------------------------------------

    @Test
    public void collectsOnlyMatchingGenericType() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExactConsumer.class.getName());
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());
        properties.put("intString", "new://" + IntStringHandler.class.getName());

        final System system = new System(properties);
        final ExactConsumer consumer = system.get(ExactConsumer.class);

        assertEquals(1, consumer.getHandlers().size());
        assertTrue(consumer.getHandlers().get(0) instanceof StringIntHandler);
    }

    @Test
    public void collectsNoneWhenNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExactConsumer.class.getName());
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final ExactConsumer consumer = system.get(ExactConsumer.class);

        assertEquals(0, consumer.getHandlers().size());
    }

    @Test
    public void extendsWildcardCollectsMatching() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExtendsConsumer.class.getName());
        properties.put("intString", "new://" + IntStringHandler.class.getName());
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final ExtendsConsumer consumer = system.get(ExtendsConsumer.class);

        assertEquals(2, consumer.getHandlers().size());
        assertTrue(consumer.getHandlers().stream().anyMatch(h -> h instanceof IntStringHandler));
        assertTrue(consumer.getHandlers().stream().anyMatch(h -> h instanceof LongBoolHandler));
    }

    @Test
    public void superWildcardCollectsMatching() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperConsumer.class.getName());
        properties.put("numBool", "new://" + NumberBoolHandler.class.getName());
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());

        final System system = new System(properties);
        final SuperConsumer consumer = system.get(SuperConsumer.class);

        assertEquals(1, consumer.getHandlers().size());
        assertTrue(consumer.getHandlers().get(0) instanceof NumberBoolHandler);
    }

    @Test
    public void unboundedWildcardCollectsAll() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + UnboundedConsumer.class.getName());
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());
        properties.put("intString", "new://" + IntStringHandler.class.getName());

        final System system = new System(properties);
        final UnboundedConsumer consumer = system.get(UnboundedConsumer.class);

        assertEquals(3, consumer.getHandlers().size());
    }

    @Test
    public void rawCollectionCollectsAll() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + RawConsumer.class.getName());
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final RawConsumer consumer = system.get(RawConsumer.class);

        assertEquals(2, consumer.getHandlers().size());
    }

    @Test
    public void collectionMatchesThroughInterfaceHierarchy() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ExactConsumer.class.getName());
        properties.put("improved", "new://" + ImprovedStringIntHandler.class.getName());
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final ExactConsumer consumer = system.get(ExactConsumer.class);

        assertEquals(1, consumer.getHandlers().size());
        assertTrue(consumer.getHandlers().get(0) instanceof ImprovedStringIntHandler);
    }

    // -----------------------------------------------------------------------
    // Unified: all producer types in one list
    // -----------------------------------------------------------------------

    @Test
    public void collectsAllProducerTypes() throws Exception {
        final ConstructorHandler prebuilt = new ConstructorHandler("prebuilt");

        final System system = System.builder()
                .add("prebuilt", prebuilt)
                .definition(ExactConsumer.class, "consumer")
                .definition(DeclaredHandler.class, "declared")
                .definition(FactoryHandlerHost.class, "factoryMade")
                .definition(BuilderHandlerHost.class, "built")
                .build();

        final ExactConsumer consumer = system.get(ExactConsumer.class);
        assertEquals(4, consumer.getHandlers().size());
    }

    @Test
    public void excludesNonMatchingAcrossAllProducerTypes() throws Exception {
        final ConstructorHandler matching = new ConstructorHandler("matching");
        final WrongHandler wrong = new WrongHandler("wrong");

        final System system = System.builder()
                .add("matching", matching)
                .add("wrong", wrong)
                .definition(ExactConsumer.class, "consumer")
                .definition(DeclaredHandler.class, "declared")
                .definition(WrongDeclaredHandler.class, "wrongDeclared")
                .definition(FactoryHandlerHost.class, "factoryMade")
                .definition(WrongFactoryHost.class, "wrongFactory")
                .definition(BuilderHandlerHost.class, "built")
                .definition(WrongBuilderHost.class, "wrongBuilt")
                .build();

        final ExactConsumer consumer = system.get(ExactConsumer.class);
        assertEquals(4, consumer.getHandlers().size());
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

    // Aliases for unified tests
    public static class ConstructorHandler extends StringIntHandler {
        public ConstructorHandler(final @Name String name) {
            super(name);
        }
    }

    public static class DeclaredHandler extends StringIntHandler {
        public DeclaredHandler(final @Name String name) {
            super(name);
        }
    }

    public static class WrongHandler implements RequestHandler<Long, Boolean> {
        public WrongHandler(final @Name String name) {
        }

        @Override
        public Boolean handle(final Long input) {
            return input > 0;
        }
    }

    public static class WrongDeclaredHandler implements RequestHandler<Long, Boolean> {
        public WrongDeclaredHandler(final @Name String name) {
        }

        @Override
        public Boolean handle(final Long input) {
            return input > 0;
        }
    }

    // Factory producers
    public static class FactoryHandlerHost {
        private FactoryHandlerHost() {
        }

        @Factory
        public static RequestHandler<String, Integer> create(final @Name String name) {
            return input -> input.length();
        }
    }

    public static class WrongFactoryHost {
        private WrongFactoryHost() {
        }

        @Factory
        public static RequestHandler<Long, Boolean> create(final @Name String name) {
            return input -> true;
        }
    }

    // Builder producers
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

    public static class WrongBuilderHost {
        private WrongBuilderHost() {
        }

        @Builder
        public static GenericHandlerBuilder<Long, Boolean> builder() {
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
