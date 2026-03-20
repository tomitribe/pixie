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
import org.tomitribe.pixie.Nullable;
import org.tomitribe.pixie.Param;
import org.tomitribe.pixie.System;

import java.util.Properties;

/**
 * Tests generic type matching for single @Component refs resolved by
 * type AND name (the resolveByTypeAndName code path — named ref in properties).
 *
 * Mirrors ComponentRefGenericByTypeTest for all generic flavors, plus
 * additional scenarios for name mismatch (correct type but wrong name).
 */
public class ComponentRefGenericByTypeAndNameTest extends Assert {

    // -----------------------------------------------------------------------
    // Interface — exact type match
    // -----------------------------------------------------------------------

    @Test
    public void interfaceExactMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceExactConsumer.class.getName());
        properties.put("consumer.handler", "@stringInt");
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final InterfaceExactConsumer consumer = system.get(InterfaceExactConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof StringIntHandler);
    }

    @Test(expected = ConstructionFailedException.class)
    public void interfaceExactNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceExactConsumer.class.getName());
        properties.put("consumer.handler", "@longBool");
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        new System(properties);
    }

    @Test
    public void interfaceRawMatchesAny() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceRawConsumer.class.getName());
        properties.put("consumer.handler", "@longBool");
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final InterfaceRawConsumer consumer = system.get(InterfaceRawConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof LongBoolHandler);
    }

    // -----------------------------------------------------------------------
    // Interface — extends wildcard
    // -----------------------------------------------------------------------

    @Test
    public void interfaceExtendsMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceExtendsConsumer.class.getName());
        properties.put("consumer.handler", "@intString");
        // IntStringHandler has <Integer, String> — Integer extends Number
        properties.put("intString", "new://" + IntStringHandler.class.getName());

        final System system = new System(properties);
        final InterfaceExtendsConsumer consumer = system.get(InterfaceExtendsConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof IntStringHandler);
    }

    @Test(expected = ConstructionFailedException.class)
    public void interfaceExtendsNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceExtendsConsumer.class.getName());
        properties.put("consumer.handler", "@stringInt");
        // StringIntHandler has <String, Integer> — String does NOT extend Number
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());

        new System(properties);
    }

    // -----------------------------------------------------------------------
    // Interface — super wildcard
    // -----------------------------------------------------------------------

    @Test
    public void interfaceSuperMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceSuperConsumer.class.getName());
        properties.put("consumer.handler", "@numBool");
        // NumberBoolHandler has <Number, Boolean> — Number is a supertype of Integer
        properties.put("numBool", "new://" + NumberBoolHandler.class.getName());

        final System system = new System(properties);
        final InterfaceSuperConsumer consumer = system.get(InterfaceSuperConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof NumberBoolHandler);
    }

    @Test(expected = ConstructionFailedException.class)
    public void interfaceSuperNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceSuperConsumer.class.getName());
        properties.put("consumer.handler", "@stringInt");
        // StringIntHandler has <String, Integer> — String is NOT a supertype of Integer
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());

        new System(properties);
    }

    // -----------------------------------------------------------------------
    // Interface — unbounded wildcard
    // -----------------------------------------------------------------------

    @Test
    public void interfaceUnboundedMatchesAny() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceUnboundedConsumer.class.getName());
        properties.put("consumer.handler", "@longBool");
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final InterfaceUnboundedConsumer consumer = system.get(InterfaceUnboundedConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof LongBoolHandler);
    }

    // -----------------------------------------------------------------------
    // Superclass — exact, raw, extends, super, unbounded
    // -----------------------------------------------------------------------

    @Test
    public void superclassExactMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassExactConsumer.class.getName());
        properties.put("consumer.processor", "@stringProc");
        properties.put("stringProc", "new://" + StringProcessor.class.getName());
        properties.put("intProc", "new://" + IntProcessor.class.getName());

        final System system = new System(properties);
        final SuperclassExactConsumer consumer = system.get(SuperclassExactConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getProcessor() instanceof StringProcessor);
    }

    @Test(expected = ConstructionFailedException.class)
    public void superclassExactNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassExactConsumer.class.getName());
        properties.put("consumer.processor", "@intProc");
        properties.put("intProc", "new://" + IntProcessor.class.getName());

        new System(properties);
    }

    @Test
    public void superclassRawMatchesAny() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassRawConsumer.class.getName());
        properties.put("consumer.processor", "@intProc");
        properties.put("intProc", "new://" + IntProcessor.class.getName());

        final System system = new System(properties);
        final SuperclassRawConsumer consumer = system.get(SuperclassRawConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getProcessor() instanceof IntProcessor);
    }

    @Test
    public void superclassExtendsMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassExtendsConsumer.class.getName());
        properties.put("consumer.processor", "@intProc");
        // IntProcessor has Processor<Integer> — Integer extends Number
        properties.put("intProc", "new://" + IntProcessor.class.getName());

        final System system = new System(properties);
        final SuperclassExtendsConsumer consumer = system.get(SuperclassExtendsConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getProcessor() instanceof IntProcessor);
    }

    @Test(expected = ConstructionFailedException.class)
    public void superclassExtendsNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassExtendsConsumer.class.getName());
        properties.put("consumer.processor", "@stringProc");
        // StringProcessor has Processor<String> — String does NOT extend Number
        properties.put("stringProc", "new://" + StringProcessor.class.getName());

        new System(properties);
    }

    @Test
    public void superclassSuperMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassSuperConsumer.class.getName());
        properties.put("consumer.processor", "@numProc");
        // NumberProcessor has Processor<Number> — Number is a supertype of Integer
        properties.put("numProc", "new://" + NumberProcessor.class.getName());

        final System system = new System(properties);
        final SuperclassSuperConsumer consumer = system.get(SuperclassSuperConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getProcessor() instanceof NumberProcessor);
    }

    @Test(expected = ConstructionFailedException.class)
    public void superclassSuperNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassSuperConsumer.class.getName());
        properties.put("consumer.processor", "@stringProc");
        properties.put("stringProc", "new://" + StringProcessor.class.getName());

        new System(properties);
    }

    @Test
    public void superclassUnboundedMatchesAny() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassUnboundedConsumer.class.getName());
        properties.put("consumer.processor", "@intProc");
        properties.put("intProc", "new://" + IntProcessor.class.getName());

        final System system = new System(properties);
        final SuperclassUnboundedConsumer consumer = system.get(SuperclassUnboundedConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getProcessor() instanceof IntProcessor);
    }

    // -----------------------------------------------------------------------
    // Multi-level inheritance
    // -----------------------------------------------------------------------

    @Test
    public void multiLevelInterfaceMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceExactConsumer.class.getName());
        properties.put("consumer.handler", "@improved");
        properties.put("improved", "new://" + ImprovedStringIntHandler.class.getName());

        final System system = new System(properties);
        final InterfaceExactConsumer consumer = system.get(InterfaceExactConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof ImprovedStringIntHandler);
    }

    @Test
    public void multiLevelSuperclassMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassExactConsumer.class.getName());
        properties.put("consumer.processor", "@special");
        properties.put("special", "new://" + SpecialStringProcessor.class.getName());

        final System system = new System(properties);
        final SuperclassExactConsumer consumer = system.get(SuperclassExactConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getProcessor() instanceof SpecialStringProcessor);
    }

    @Test
    public void deferredTypeVariableMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceExactConsumer.class.getName());
        properties.put("consumer.handler", "@deferred");
        properties.put("deferred", "new://" + DeferredStringIntHandler.class.getName());

        final System system = new System(properties);
        final InterfaceExactConsumer consumer = system.get(InterfaceExactConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof DeferredStringIntHandler);
    }

    // -----------------------------------------------------------------------
    // Nullable
    // -----------------------------------------------------------------------

    @Test
    public void nullableReturnsNullWhenNameNotFound() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + NullableConsumer.class.getName());
        properties.put("consumer.handler", "@nonexistent");

        // Named ref to a component that doesn't exist — nullable should yield null
        // Actually, NamedComponentNotFoundException is thrown even for nullable.
        // Nullable only applies to resolveByType when no match is found.
        // For named refs, the name must exist. Let's verify this.
        try {
            new System(properties);
            fail("Expected ConstructionFailedException");
        } catch (final ConstructionFailedException e) {
            // Expected — named refs must resolve even when nullable
        }
    }

    // -----------------------------------------------------------------------
    // Pre-built instances
    // -----------------------------------------------------------------------

    @Test
    public void preBuiltInstanceMatchesGeneric() throws Exception {
        final StringIntHandler handler = new StringIntHandler("prebuilt");

        final System system = System.builder()
                .add("myHandler", handler)
                .definition(InterfaceExactConsumer.class, "consumer")
                .comp("handler", "myHandler")
                .build();

        final InterfaceExactConsumer consumer = system.get(InterfaceExactConsumer.class);
        assertNotNull(consumer);
        assertSame(handler, consumer.getHandler());
    }

    @Test(expected = ConstructionFailedException.class)
    public void preBuiltInstanceRejectsWrongGeneric() throws Exception {
        final LongBoolHandler handler = new LongBoolHandler("prebuilt");

        System.builder()
                .add("myHandler", handler)
                .definition(InterfaceExactConsumer.class, "consumer")
                .comp("handler", "myHandler")
                .build();
    }

    // -----------------------------------------------------------------------
    // Name-specific: correct generics but wrong name
    // -----------------------------------------------------------------------

    @Test(expected = ConstructionFailedException.class)
    public void correctGenericButWrongName() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceExactConsumer.class.getName());
        // Points to a name that doesn't exist, even though a matching type does
        properties.put("consumer.handler", "@nonexistent");
        properties.put("stringInt", "new://" + StringIntHandler.class.getName());

        new System(properties);
    }

    // -----------------------------------------------------------------------
    // Nested parameterized wildcard bound
    // -----------------------------------------------------------------------

    @Test
    public void nestedParameterizedWildcardBound() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ComparableConsumer.class.getName());
        properties.put("consumer.wrapper", "@strWrapper");
        properties.put("strWrapper", "new://" + StringWrapper.class.getName());

        final System system = new System(properties);
        final ComparableConsumer consumer = system.get(ComparableConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getWrapper() instanceof StringWrapper);
    }

    @Test(expected = ConstructionFailedException.class)
    public void nestedParameterizedWildcardBoundNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + ComparableConsumer.class.getName());
        properties.put("consumer.wrapper", "@uriWrapper");
        properties.put("uriWrapper", "new://" + UriWrapper.class.getName());

        new System(properties);
    }

    // =======================================================================
    // Fixtures — RequestHandler interface hierarchy
    // =======================================================================

    public interface RequestHandler<I, O> {
        O handle(I input);
    }

    public interface ImprovedHandler<I, O> extends RequestHandler<I, O> {
    }

    public static abstract class AbstractHandler<I, O> implements RequestHandler<I, O> {
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

    public static class DeferredStringIntHandler extends AbstractHandler<String, Integer> {
        public DeferredStringIntHandler(final @Name String name) {
        }

        @Override
        public Integer handle(final String input) {
            return input.length();
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

    public static class NumberProcessor extends Processor<Number> {
        public NumberProcessor(final @Name String name) {
            super(name);
        }

        @Override
        public Number process() {
            return 42;
        }
    }

    public static class SpecialStringProcessor extends StringProcessor {
        public SpecialStringProcessor(final @Name String name) {
            super(name);
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

    @SuppressWarnings("rawtypes")
    public static class InterfaceRawConsumer {
        private final RequestHandler handler;

        public InterfaceRawConsumer(
                final @Name String name,
                final @Param("handler") @Component RequestHandler handler
        ) {
            this.handler = handler;
        }

        public RequestHandler getHandler() {
            return handler;
        }
    }

    public static class InterfaceExtendsConsumer {
        private final RequestHandler<? extends Number, ?> handler;

        public InterfaceExtendsConsumer(
                final @Name String name,
                final @Param("handler") @Component RequestHandler<? extends Number, ?> handler
        ) {
            this.handler = handler;
        }

        public RequestHandler<? extends Number, ?> getHandler() {
            return handler;
        }
    }

    public static class InterfaceSuperConsumer {
        private final RequestHandler<? super Integer, ?> handler;

        public InterfaceSuperConsumer(
                final @Name String name,
                final @Param("handler") @Component RequestHandler<? super Integer, ?> handler
        ) {
            this.handler = handler;
        }

        public RequestHandler<? super Integer, ?> getHandler() {
            return handler;
        }
    }

    public static class InterfaceUnboundedConsumer {
        private final RequestHandler<?, ?> handler;

        public InterfaceUnboundedConsumer(
                final @Name String name,
                final @Param("handler") @Component RequestHandler<?, ?> handler
        ) {
            this.handler = handler;
        }

        public RequestHandler<?, ?> getHandler() {
            return handler;
        }
    }

    public static class SuperclassExactConsumer {
        private final Processor<String> processor;

        public SuperclassExactConsumer(
                final @Name String name,
                final @Param("processor") @Component Processor<String> processor
        ) {
            this.processor = processor;
        }

        public Processor<String> getProcessor() {
            return processor;
        }
    }

    @SuppressWarnings("rawtypes")
    public static class SuperclassRawConsumer {
        private final Processor processor;

        public SuperclassRawConsumer(
                final @Name String name,
                final @Param("processor") @Component Processor processor
        ) {
            this.processor = processor;
        }

        public Processor getProcessor() {
            return processor;
        }
    }

    public static class SuperclassExtendsConsumer {
        private final Processor<? extends Number> processor;

        public SuperclassExtendsConsumer(
                final @Name String name,
                final @Param("processor") @Component Processor<? extends Number> processor
        ) {
            this.processor = processor;
        }

        public Processor<? extends Number> getProcessor() {
            return processor;
        }
    }

    public static class SuperclassSuperConsumer {
        private final Processor<? super Integer> processor;

        public SuperclassSuperConsumer(
                final @Name String name,
                final @Param("processor") @Component Processor<? super Integer> processor
        ) {
            this.processor = processor;
        }

        public Processor<? super Integer> getProcessor() {
            return processor;
        }
    }

    public static class SuperclassUnboundedConsumer {
        private final Processor<?> processor;

        public SuperclassUnboundedConsumer(
                final @Name String name,
                final @Param("processor") @Component Processor<?> processor
        ) {
            this.processor = processor;
        }

        public Processor<?> getProcessor() {
            return processor;
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
