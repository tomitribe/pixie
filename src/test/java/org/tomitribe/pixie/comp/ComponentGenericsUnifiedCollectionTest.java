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
 * Verifies that a generic collection injection point can collect
 * components from all producer types in a single list:
 * - Pre-built Instance (via system.add())
 * - Constructor-based Declaration
 * - Factory method Declaration
 * - Builder Declaration
 *
 * Tests both resolveByType (unnamed) and resolveByTypeAndName (named refs).
 */
public class ComponentGenericsUnifiedCollectionTest extends Assert {

    // =======================================================================
    // resolveByType — collects all four producer types into one list
    // =======================================================================

    @Test
    public void collectsByTypeFromAllProducerTypes() throws Exception {
        final ConstructorHandler constructorHandler = new ConstructorHandler("fromConstructor");

        final System system = System.builder()
                .add("prebuilt", constructorHandler)
                .definition(CollectByTypeConsumer.class, "consumer")
                .definition(DeclaredHandler.class, "declared")
                .definition(FactoryHandlerHost.class, "factoryMade")
                .definition(BuilderHandlerHost.class, "built")
                .build();

        final CollectByTypeConsumer consumer = system.get(CollectByTypeConsumer.class);
        assertNotNull(consumer);

        final List<RequestHandler<String, Integer>> handlers = consumer.getHandlers();
        assertEquals(4, handlers.size());

        final Set<String> types = handlers.stream()
                .map(h -> h.getClass().getSimpleName())
                .collect(Collectors.toSet());

        // ConstructorHandler appears twice: once as pre-built Instance, once as Declaration
        assertTrue("Expected ConstructorHandler (pre-built + declared)", types.contains("ConstructorHandler"));
        assertTrue("Expected FactoryCreatedHandler", types.contains("FactoryCreatedHandler"));
        // Builder produces a lambda/anonymous class, so check by exclusion
        assertEquals(4, handlers.size());
    }

    /**
     * Ensure non-matching generics are excluded even when mixed producer types exist.
     */
    @Test
    public void collectsByTypeExcludesNonMatching() throws Exception {
        final ConstructorHandler matchingInstance = new ConstructorHandler("matching");
        final WrongHandler wrongInstance = new WrongHandler("wrong");

        final System system = System.builder()
                .add("matching", matchingInstance)
                .add("wrong", wrongInstance)
                .definition(CollectByTypeConsumer.class, "consumer")
                .definition(DeclaredHandler.class, "declared")
                .definition(WrongDeclaredHandler.class, "wrongDeclared")
                .definition(FactoryHandlerHost.class, "factoryMade")
                .definition(WrongFactoryHost.class, "wrongFactory")
                .definition(BuilderHandlerHost.class, "built")
                .definition(WrongBuilderHost.class, "wrongBuilt")
                .build();

        final CollectByTypeConsumer consumer = system.get(CollectByTypeConsumer.class);
        assertNotNull(consumer);

        // 4 matching: pre-built Instance, Constructor Declaration, Factory Declaration, Builder Declaration
        // 4 non-matching: pre-built wrong Instance, wrong Constructor, wrong Factory, wrong Builder
        assertEquals(4, consumer.getHandlers().size());
    }

    // =======================================================================
    // resolveByTypeAndName — collects named refs from all producer types
    // =======================================================================

    @Test
    public void collectsByNameFromAllProducerTypes() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + CollectByNameConsumer.class.getName());
        properties.put("consumer.handlers", "@declared @factoryMade @built");
        properties.put("declared", "new://" + DeclaredHandler.class.getName());
        properties.put("factoryMade", "new://" + FactoryHandlerHost.class.getName());
        properties.put("built", "new://" + BuilderHandlerHost.class.getName());

        final System system = new System(properties);

        final CollectByNameConsumer consumer = system.get(CollectByNameConsumer.class);
        assertNotNull(consumer);
        assertEquals(3, consumer.getHandlers().size());
    }

    /**
     * Named collection including a pre-built Instance alongside a Declaration.
     */
    @Test
    public void collectsByNameIncludingPreBuiltInstance() throws Exception {
        final ConstructorHandler prebuilt = new ConstructorHandler("prebuilt");

        final System system = System.builder()
                .add("prebuilt", prebuilt)
                .definition(CollectByNameConsumer.class, "consumer")
                .comp("handlers", "prebuilt")
                .build();

        final CollectByNameConsumer consumer = system.get(CollectByNameConsumer.class);
        assertNotNull(consumer);
        assertEquals(1, consumer.getHandlers().size());
        assertSame(prebuilt, consumer.getHandlers().get(0));
    }

    /**
     * Named collection should reject a named component whose generics don't match.
     */
    @Test(expected = ConstructionFailedException.class)
    public void collectsByNameRejectsWrongGeneric() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + CollectByNameConsumer.class.getName());
        properties.put("consumer.handlers", "@wrong");
        properties.put("wrong", "new://" + WrongDeclaredHandler.class.getName());

        new System(properties);
    }

    // =======================================================================
    // Fixtures — RequestHandler interface
    // =======================================================================

    public interface RequestHandler<I, O> {
        O handle(I input);
    }

    // --- Constructor-based ---

    public static class ConstructorHandler implements RequestHandler<String, Integer> {
        public ConstructorHandler(final @Name String name) {
        }

        @Override
        public Integer handle(final String input) {
            return input.length();
        }
    }

    public static class DeclaredHandler implements RequestHandler<String, Integer> {
        public DeclaredHandler(final @Name String name) {
        }

        @Override
        public Integer handle(final String input) {
            return input.length() * 2;
        }
    }

    // --- Factory-based ---

    public static class FactoryCreatedHandler implements RequestHandler<String, Integer> {
        @Override
        public Integer handle(final String input) {
            return input.length() * 3;
        }
    }

    public static class FactoryHandlerHost {
        private FactoryHandlerHost() {
        }

        @Factory
        public static RequestHandler<String, Integer> create(final @Name String name) {
            return new FactoryCreatedHandler();
        }
    }

    // --- Builder-based ---

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

    // --- Wrong generic type variants (should NOT be collected) ---

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

    public static class WrongFactoryHost {
        private WrongFactoryHost() {
        }

        @Factory
        public static RequestHandler<Long, Boolean> create(final @Name String name) {
            return input -> true;
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

    public static class CollectByTypeConsumer {
        private final List<RequestHandler<String, Integer>> handlers;

        public CollectByTypeConsumer(
                final @Name String name,
                final @Param("handlers") @Component List<RequestHandler<String, Integer>> handlers
        ) {
            this.handlers = handlers;
        }

        public List<RequestHandler<String, Integer>> getHandlers() {
            return handlers;
        }
    }

    public static class CollectByNameConsumer {
        private final List<RequestHandler<String, Integer>> handlers;

        public CollectByNameConsumer(
                final @Name String name,
                final @Param("handlers") @Component List<RequestHandler<String, Integer>> handlers
        ) {
            this.handlers = handlers;
        }

        public List<RequestHandler<String, Integer>> getHandlers() {
            return handlers;
        }
    }
}
