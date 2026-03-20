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

import java.util.Properties;

/**
 * Advanced generics tests covering named refs, factory methods, builders,
 * nullable, System.builder() API, and nested parameterized wildcard bounds.
 */
public class ComponentGenericsAdvancedTest extends Assert {

    // =======================================================================
    // 1. Named component refs with generics (resolveByTypeAndName path)
    // =======================================================================

    @Test
    public void namedRefMatchesCorrectGeneric() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + NamedConsumer.class.getName());
        properties.put("consumer.handler", "@stringInt");
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final NamedConsumer consumer = system.get(NamedConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof StringIntHandler);
    }

    @Test(expected = ConstructionFailedException.class)
    public void namedRefRejectsWrongGeneric() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + NamedConsumer.class.getName());
        // longBool is RequestHandler<Long, Boolean> — does not match <String, Integer>
        properties.put("consumer.handler", "@longBool");
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        new System(properties);
    }

    // =======================================================================
    // 2. Factory method producer with generic return type
    // =======================================================================

    @Test
    public void factoryGenericReturnMatchesConsumer() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceExactConsumer.class.getName());
        properties.put("handler", "new://" + StringIntHandlerFactory.class.getName());

        final System system = new System(properties);
        final InterfaceExactConsumer consumer = system.get(InterfaceExactConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.getHandler());
        assertEquals(42, (int) consumer.getHandler().handle("hello"));
    }

    @Test(expected = ConstructionFailedException.class)
    public void factoryGenericReturnRejectsWrongConsumer() throws Exception {
        final Properties properties = new Properties();
        // Consumer wants <String, Integer> but factory produces <Long, Boolean>
        properties.put("consumer", "new://" + InterfaceExactConsumer.class.getName());
        properties.put("handler", "new://" + LongBoolHandlerFactory.class.getName());

        new System(properties);
    }

    // =======================================================================
    // 3. Builder producer with generic type
    // =======================================================================

    @Test
    public void builderGenericMatchesConsumer() throws Exception {
        final System system = System.builder()
                .definition(InterfaceExactConsumer.class, "consumer")
                .definition(StringIntHandlerBuilder.class, "handler")
                .build();

        final InterfaceExactConsumer consumer = system.get(InterfaceExactConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.getHandler());
    }

    @Test(expected = ConstructionFailedException.class)
    public void builderGenericRejectsWrongConsumer() throws Exception {
        // Consumer wants <String, Integer> but builder produces <Long, Boolean>
        System.builder()
                .definition(InterfaceExactConsumer.class, "consumer")
                .definition(LongBoolHandlerBuilder.class, "handler")
                .build();
    }

    // =======================================================================
    // 4. Nullable + generics
    // =======================================================================

    @Test
    public void nullableReturnsNullWhenNoGenericMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + NullableConsumer.class.getName());
        // Only a LongBool handler exists — no match for <String, Integer>
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final NullableConsumer consumer = system.get(NullableConsumer.class);
        assertNotNull(consumer);
        assertNull(consumer.getHandler());
    }

    @Test
    public void nullableInjectsWhenGenericMatches() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + NullableConsumer.class.getName());
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());

        final System system = new System(properties);
        final NullableConsumer consumer = system.get(NullableConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.getHandler());
        assertTrue(consumer.getHandler() instanceof StringIntHandler);
    }

    // =======================================================================
    // 5. Pre-built instances via System.builder().add()
    // =======================================================================

    @Test
    public void preBuiltInstanceMatchesGeneric() throws Exception {
        final StringIntHandler handler = new StringIntHandler("manual");

        final System system = System.builder()
                .add("handler", handler)
                .definition(InterfaceExactConsumer.class, "consumer")
                .build();

        final InterfaceExactConsumer consumer = system.get(InterfaceExactConsumer.class);
        assertNotNull(consumer);
        assertSame(handler, consumer.getHandler());
    }

    @Test(expected = ConstructionFailedException.class)
    public void preBuiltInstanceRejectsWrongGeneric() throws Exception {
        final LongBoolHandler handler = new LongBoolHandler("manual");

        // Consumer wants <String, Integer> but the pre-built instance is <Long, Boolean>
        System.builder()
                .add("handler", handler)
                .definition(InterfaceExactConsumer.class, "consumer")
                .build();
    }

    // =======================================================================
    // 6. Nested parameterized type in wildcard bound
    //    ? extends Comparable<String> where actual is String
    //    (String implements Comparable<String>)
    // =======================================================================

    @Test
    public void nestedParameterizedWildcardBound() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ComparableConsumer.class.getName());
        properties.put("wrapper", "new://" + StringWrapper.class.getName());

        final System system = new System(properties);
        final ComparableConsumer consumer = system.get(ComparableConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getWrapper() instanceof StringWrapper);
    }

    @Test(expected = ConstructionFailedException.class)
    public void nestedParameterizedWildcardBoundNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ComparableConsumer.class.getName());
        // UriWrapper has Wrapper<java.net.URI> — URI does NOT implement Comparable<String>
        properties.put("wrapper", "new://" + UriWrapper.class.getName());

        new System(properties);
    }

    // =======================================================================
    // Fixtures — RequestHandler interface
    // =======================================================================

    public interface RequestHandler<I, O> {
        O handle(I input);
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

    // =======================================================================
    // Fixtures — Factory method producers
    // =======================================================================

    public static class StringIntHandlerFactory {
        private StringIntHandlerFactory() {
        }

        @Factory
        public static RequestHandler<String, Integer> create(final @Name String name) {
            return input -> 42;
        }
    }

    public static class LongBoolHandlerFactory {
        private LongBoolHandlerFactory() {
        }

        @Factory
        public static RequestHandler<Long, Boolean> create(final @Name String name) {
            return input -> true;
        }
    }

    // =======================================================================
    // Fixtures — Builder producers
    // =======================================================================

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

    public static class StringIntHandlerBuilder {
        private StringIntHandlerBuilder() {
        }

        @Builder
        public static GenericHandlerBuilder<String, Integer> builder() {
            return new GenericHandlerBuilder<>();
        }
    }

    public static class LongBoolHandlerBuilder {
        private LongBoolHandlerBuilder() {
        }

        @Builder
        public static GenericHandlerBuilder<Long, Boolean> builder() {
            return new GenericHandlerBuilder<>();
        }
    }

    // =======================================================================
    // Fixtures — Wrapper for nested parameterized wildcard test
    // =======================================================================

    public interface Wrapper<T> {
        T getValue();
    }

    public static class StringWrapper implements Wrapper<String> {
        public StringWrapper(final @Name String name) {
        }

        @Override
        public String getValue() {
            return "hello";
        }
    }

    public static class UriWrapper implements Wrapper<java.net.URI> {
        public UriWrapper(final @Name String name) {
        }

        @Override
        public java.net.URI getValue() {
            return java.net.URI.create("http://example.com");
        }
    }

    // =======================================================================
    // Fixtures — Consumer classes
    // =======================================================================

    public static class NamedConsumer {
        private final RequestHandler<String, Integer> handler;

        public NamedConsumer(
                final @Name String name,
                final @Param("handler") @Component RequestHandler<String, Integer> handler
        ) {
            this.handler = handler;
        }

        public RequestHandler<String, Integer> getHandler() {
            return handler;
        }
    }

    public static class InterfaceExactConsumer {
        private final RequestHandler<String, Integer> handler;

        public InterfaceExactConsumer(
                final @Name String name,
                final @Param("handler") @Component RequestHandler<String, Integer> handler
        ) {
            this.handler = handler;
        }

        public RequestHandler<String, Integer> getHandler() {
            return handler;
        }
    }

    public static class NullableConsumer {
        private final RequestHandler<String, Integer> handler;

        public NullableConsumer(
                final @Name String name,
                final @Param("handler") @Nullable @Component RequestHandler<String, Integer> handler
        ) {
            this.handler = handler;
        }

        public RequestHandler<String, Integer> getHandler() {
            return handler;
        }
    }

    public static class ComparableConsumer {
        private final Wrapper<? extends Comparable<String>> wrapper;

        public ComparableConsumer(
                final @Name String name,
                final @Param("wrapper") @Component Wrapper<? extends Comparable<String>> wrapper
        ) {
            this.wrapper = wrapper;
        }

        public Wrapper<? extends Comparable<String>> getWrapper() {
            return wrapper;
        }
    }
}
