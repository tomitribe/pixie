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
import org.tomitribe.pixie.Factory;
import org.tomitribe.pixie.Name;
import org.tomitribe.pixie.Nullable;
import org.tomitribe.pixie.Param;
import org.tomitribe.pixie.System;

import java.util.List;
import java.util.Properties;

/**
 * Tests for System.getAll(Class) which returns all components
 * assignable to the given type.
 *
 * Note: getAll currently takes Class (not Type), so it performs
 * raw type matching only — all RequestHandler implementations are
 * returned regardless of their type arguments. These tests document
 * that behavior.
 */
public class SystemGetAllTest extends Assert {

    // -----------------------------------------------------------------------
    // Basic: returns all instances of the requested type
    // -----------------------------------------------------------------------

    @Test
    public void returnsAllMatchingInstances() throws Exception {
        final Properties properties = new Properties();
        properties.put("a", "new://" + StringIntHandler.class.getName());
        properties.put("b", "new://" + LongBoolHandler.class.getName());
        properties.put("c", "new://" + IntStringHandler.class.getName());

        final System system = new System(properties);
        final List<RequestHandler> handlers = system.getAll(RequestHandler.class);

        assertEquals(3, handlers.size());
    }

    @Test
    public void returnsEmptyWhenNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("a", "new://" + StringIntHandler.class.getName());

        final System system = new System(properties);
        final List<Unrelated> result = system.getAll(Unrelated.class);

        assertEquals(0, result.size());
    }

    // -----------------------------------------------------------------------
    // Subtype matching: subtypes are included
    // -----------------------------------------------------------------------

    @Test
    public void includesSubtypes() throws Exception {
        final Properties properties = new Properties();
        properties.put("a", "new://" + StringIntHandler.class.getName());
        properties.put("b", "new://" + ImprovedStringIntHandler.class.getName());

        final System system = new System(properties);
        final List<RequestHandler> handlers = system.getAll(RequestHandler.class);

        assertEquals(2, handlers.size());
    }

    @Test
    public void narrowBySubtype() throws Exception {
        final Properties properties = new Properties();
        properties.put("a", "new://" + StringIntHandler.class.getName());
        properties.put("b", "new://" + ImprovedStringIntHandler.class.getName());

        final System system = new System(properties);
        // Only ImprovedHandler subtypes, not plain RequestHandler impls
        final List<ImprovedHandler> handlers = system.getAll(ImprovedHandler.class);

        assertEquals(1, handlers.size());
        assertTrue(handlers.get(0) instanceof ImprovedStringIntHandler);
    }

    // -----------------------------------------------------------------------
    // Pre-built instances are included
    // -----------------------------------------------------------------------

    @Test
    public void includesPreBuiltInstances() throws Exception {
        final StringIntHandler prebuilt = new StringIntHandler("prebuilt");

        final System system = System.builder()
                .add("prebuilt", prebuilt)
                .definition(LongBoolHandler.class, "declared")
                .build();

        final List<RequestHandler> handlers = system.getAll(RequestHandler.class);

        assertEquals(2, handlers.size());
    }

    // -----------------------------------------------------------------------
    // All producer types are collected
    // -----------------------------------------------------------------------

    @Test
    public void collectsAllProducerTypes() throws Exception {
        final StringIntHandler prebuilt = new StringIntHandler("prebuilt");

        final System system = System.builder()
                .add("prebuilt", prebuilt)
                .definition(LongBoolHandler.class, "declared")
                .definition(FactoryHandlerHost.class, "factoryMade")
                .definition(BuilderHandlerHost.class, "built")
                .build();

        final List<RequestHandler> handlers = system.getAll(RequestHandler.class);

        // Instance + Constructor + Factory + Builder = 4
        assertEquals(4, handlers.size());
    }

    // -----------------------------------------------------------------------
    // Superclass matching
    // -----------------------------------------------------------------------

    @Test
    public void returnsAllSubclasses() throws Exception {
        final Properties properties = new Properties();
        properties.put("a", "new://" + StringProcessor.class.getName());
        properties.put("b", "new://" + IntProcessor.class.getName());

        final System system = new System(properties);
        final List<Processor> processors = system.getAll(Processor.class);

        assertEquals(2, processors.size());
    }

    @Test
    public void deepInheritanceIncluded() throws Exception {
        final Properties properties = new Properties();
        properties.put("a", "new://" + StringProcessor.class.getName());
        properties.put("b", "new://" + SpecialStringProcessor.class.getName());

        final System system = new System(properties);
        final List<Processor> processors = system.getAll(Processor.class);

        assertEquals(2, processors.size());
    }

    // -----------------------------------------------------------------------
    // Raw type matching: getAll(Class) does not filter by generics
    // -----------------------------------------------------------------------

    @Test
    public void rawTypeReturnsAllRegardlessOfGenericArgs() throws Exception {
        final Properties properties = new Properties();
        properties.put("a", "new://" + StringIntHandler.class.getName());
        properties.put("b", "new://" + LongBoolHandler.class.getName());
        properties.put("c", "new://" + IntStringHandler.class.getName());

        final System system = new System(properties);

        // getAll takes Class, not Type — so all RequestHandler impls are returned
        // regardless of their type arguments
        final List<RequestHandler> handlers = system.getAll(RequestHandler.class);

        assertEquals(3, handlers.size());
    }

    // =======================================================================
    // Fixtures — RequestHandler interface hierarchy
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

    public static class ImprovedStringIntHandler implements ImprovedHandler<String, Integer> {
        public ImprovedStringIntHandler(final @Name String name) {
        }

        @Override
        public Integer handle(final String input) {
            return input.length();
        }
    }

    // Factory producer
    public static class FactoryHandlerHost {
        private FactoryHandlerHost() {
        }

        @Factory
        public static RequestHandler<String, Integer> create(final @Name String name) {
            return input -> input.length();
        }
    }

    // Builder producer
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
    // Fixtures — Processor superclass hierarchy
    // =======================================================================

    public static abstract class Processor<T> {
        public Processor(final @Name String name) {
        }

        public abstract T process();
    }

    public static class StringProcessor extends Processor<String> {
        public StringProcessor(final @Name String name) {
            super(name);
        }

        @Override
        public String process() {
            return "done";
        }
    }

    public static class IntProcessor extends Processor<Integer> {
        public IntProcessor(final @Name String name) {
            super(name);
        }

        @Override
        public Integer process() {
            return 42;
        }
    }

    public static class SpecialStringProcessor extends StringProcessor {
        public SpecialStringProcessor(final @Name String name) {
            super(name);
        }
    }

    // =======================================================================
    // Fixtures — Unrelated type
    // =======================================================================

    public static class Unrelated {
    }
}
