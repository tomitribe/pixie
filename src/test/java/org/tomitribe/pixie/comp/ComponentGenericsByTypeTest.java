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
 * Tests that generic type arguments on @Component parameters are used
 * to narrow which components are eligible for injection.
 *
 * Covers interfaces, superclasses, exact types, wildcards (extends, super,
 * unbounded), raw types, and multi-level inheritance.
 */
public class ComponentGenericsByTypeTest extends Assert {

    // -----------------------------------------------------------------------
    // Interface — exact type match
    // -----------------------------------------------------------------------

    @Test
    public void interfaceExactMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceExactConsumer.class.getName());
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
        properties.put("longBool", "new://" + LongBoolHandler.class.getName());

        new System(properties);
    }

    @Test
    public void interfaceRawMatchesAny() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceRawConsumer.class.getName());
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
        // IntStringHandler has <Integer, String> — Integer extends Number
        properties.put("handler", "new://" + IntStringHandler.class.getName());

        final System system = new System(properties);
        final InterfaceExtendsConsumer consumer = system.get(InterfaceExtendsConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof IntStringHandler);
    }

    @Test(expected = ConstructionFailedException.class)
    public void interfaceExtendsNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceExtendsConsumer.class.getName());
        // StringIntHandler has <String, Integer> — String does NOT extend Number
        properties.put("handler", "new://" + StringIntHandler.class.getName());

        new System(properties);
    }

    // -----------------------------------------------------------------------
    // Interface — super wildcard
    // -----------------------------------------------------------------------

    @Test
    public void interfaceSuperMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceSuperConsumer.class.getName());
        // NumberBoolHandler has <Number, Boolean> — Number is a supertype of Integer
        properties.put("handler", "new://" + NumberBoolHandler.class.getName());

        final System system = new System(properties);
        final InterfaceSuperConsumer consumer = system.get(InterfaceSuperConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof NumberBoolHandler);
    }

    @Test(expected = ConstructionFailedException.class)
    public void interfaceSuperNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceSuperConsumer.class.getName());
        // StringIntHandler has <String, Integer> — String is NOT a supertype of Integer
        properties.put("handler", "new://" + StringIntHandler.class.getName());

        new System(properties);
    }

    // -----------------------------------------------------------------------
    // Interface — unbounded wildcard
    // -----------------------------------------------------------------------

    @Test
    public void interfaceUnboundedMatchesAny() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceUnboundedConsumer.class.getName());
        properties.put("handler", "new://" + LongBoolHandler.class.getName());

        final System system = new System(properties);
        final InterfaceUnboundedConsumer consumer = system.get(InterfaceUnboundedConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof LongBoolHandler);
    }

    // -----------------------------------------------------------------------
    // Superclass — exact type match
    // -----------------------------------------------------------------------

    @Test
    public void superclassExactMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassExactConsumer.class.getName());
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
        properties.put("intProc", "new://" + IntProcessor.class.getName());

        new System(properties);
    }

    @Test
    public void superclassRawMatchesAny() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassRawConsumer.class.getName());
        properties.put("intProc", "new://" + IntProcessor.class.getName());

        final System system = new System(properties);
        final SuperclassRawConsumer consumer = system.get(SuperclassRawConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getProcessor() instanceof IntProcessor);
    }

    // -----------------------------------------------------------------------
    // Superclass — extends wildcard
    // -----------------------------------------------------------------------

    @Test
    public void superclassExtendsMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassExtendsConsumer.class.getName());
        // IntProcessor has Processor<Integer> — Integer extends Number
        properties.put("processor", "new://" + IntProcessor.class.getName());

        final System system = new System(properties);
        final SuperclassExtendsConsumer consumer = system.get(SuperclassExtendsConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getProcessor() instanceof IntProcessor);
    }

    @Test(expected = ConstructionFailedException.class)
    public void superclassExtendsNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassExtendsConsumer.class.getName());
        // StringProcessor has Processor<String> — String does NOT extend Number
        properties.put("processor", "new://" + StringProcessor.class.getName());

        new System(properties);
    }

    // -----------------------------------------------------------------------
    // Superclass — super wildcard
    // -----------------------------------------------------------------------

    @Test
    public void superclassSuperMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassSuperConsumer.class.getName());
        // NumberProcessor has Processor<Number> — Number is a supertype of Integer
        properties.put("processor", "new://" + NumberProcessor.class.getName());

        final System system = new System(properties);
        final SuperclassSuperConsumer consumer = system.get(SuperclassSuperConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getProcessor() instanceof NumberProcessor);
    }

    @Test(expected = ConstructionFailedException.class)
    public void superclassSuperNoMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassSuperConsumer.class.getName());
        // StringProcessor has Processor<String> — String is NOT a supertype of Integer
        properties.put("processor", "new://" + StringProcessor.class.getName());

        new System(properties);
    }

    // -----------------------------------------------------------------------
    // Superclass — unbounded wildcard
    // -----------------------------------------------------------------------

    @Test
    public void superclassUnboundedMatchesAny() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassUnboundedConsumer.class.getName());
        properties.put("processor", "new://" + IntProcessor.class.getName());

        final System system = new System(properties);
        final SuperclassUnboundedConsumer consumer = system.get(SuperclassUnboundedConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getProcessor() instanceof IntProcessor);
    }

    // -----------------------------------------------------------------------
    // Multi-level interface inheritance
    // -----------------------------------------------------------------------

    @Test
    public void multiLevelInterfaceMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceExactConsumer.class.getName());
        // ImprovedStringIntHandler implements ImprovedHandler<String, Integer>
        // which extends RequestHandler<String, Integer>
        properties.put("handler", "new://" + ImprovedStringIntHandler.class.getName());

        final System system = new System(properties);
        final InterfaceExactConsumer consumer = system.get(InterfaceExactConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof ImprovedStringIntHandler);
    }

    // -----------------------------------------------------------------------
    // Multi-level superclass inheritance
    // -----------------------------------------------------------------------

    @Test
    public void multiLevelSuperclassMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + SuperclassExactConsumer.class.getName());
        // SpecialStringProcessor extends StringProcessor extends Processor<String>
        properties.put("processor", "new://" + SpecialStringProcessor.class.getName());

        final System system = new System(properties);
        final SuperclassExactConsumer consumer = system.get(SuperclassExactConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getProcessor() instanceof SpecialStringProcessor);
    }

    // -----------------------------------------------------------------------
    // Deferred type variable through abstract class
    // -----------------------------------------------------------------------

    @Test
    public void deferredTypeVariableMatch() throws Exception {
        final Properties properties = new Properties();
        properties.put("consumer", "new://" + InterfaceExactConsumer.class.getName());
        // DeferredStringIntHandler extends AbstractHandler<String, Integer>
        // where AbstractHandler<I, O> implements RequestHandler<I, O>
        properties.put("handler", "new://" + DeferredStringIntHandler.class.getName());

        final System system = new System(properties);
        final InterfaceExactConsumer consumer = system.get(InterfaceExactConsumer.class);
        assertNotNull(consumer);
        assertTrue(consumer.getHandler() instanceof DeferredStringIntHandler);
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
    // Fixtures — Consumer classes (interface-based injection points)
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

    // =======================================================================
    // Fixtures — Consumer classes (superclass-based injection points)
    // =======================================================================

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
}
