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

/**
 * Tests that pre-built instances (added via System.builder().add())
 * have their runtime class's generic type information correctly resolved
 * for matching. The instance's actual class hierarchy determines the
 * generic type arguments.
 */
public class ComponentProducerGenericInstanceTest extends Assert {

    // -----------------------------------------------------------------------
    // Basic generic match
    // -----------------------------------------------------------------------

    @Test
    public void instanceMatchesExactGeneric() throws Exception {
        final StringIntHandler handler = new StringIntHandler("prebuilt");

        final System system = System.builder()
                .add("handler", handler)
                .definition(HandlerConsumer.class, "consumer")
                .build();

        final HandlerConsumer consumer = system.get(HandlerConsumer.class);
        assertNotNull(consumer);
        assertSame(handler, consumer.getHandler());
    }

    @Test(expected = ConstructionFailedException.class)
    public void instanceRejectsWrongGeneric() throws Exception {
        final LongBoolHandler handler = new LongBoolHandler("prebuilt");

        System.builder()
                .add("handler", handler)
                .definition(HandlerConsumer.class, "consumer")
                .build();
    }

    // -----------------------------------------------------------------------
    // Subtype with inherited generics
    // -----------------------------------------------------------------------

    @Test
    public void instanceSubtypeWithInheritedGenerics() throws Exception {
        final ImprovedStringIntHandler handler = new ImprovedStringIntHandler("prebuilt");

        final System system = System.builder()
                .add("handler", handler)
                .definition(HandlerConsumer.class, "consumer")
                .build();

        final HandlerConsumer consumer = system.get(HandlerConsumer.class);
        assertNotNull(consumer);
        assertSame(handler, consumer.getHandler());
    }

    @Test(expected = ConstructionFailedException.class)
    public void instanceSubtypeWithWrongInheritedGenerics() throws Exception {
        final ImprovedLongBoolHandler handler = new ImprovedLongBoolHandler("prebuilt");

        System.builder()
                .add("handler", handler)
                .definition(HandlerConsumer.class, "consumer")
                .build();
    }

    // -----------------------------------------------------------------------
    // Multi-level inheritance
    // -----------------------------------------------------------------------

    @Test
    public void instanceMultiLevelInheritance() throws Exception {
        final SpecialStringIntHandler handler = new SpecialStringIntHandler("prebuilt");

        final System system = System.builder()
                .add("handler", handler)
                .definition(HandlerConsumer.class, "consumer")
                .build();

        final HandlerConsumer consumer = system.get(HandlerConsumer.class);
        assertNotNull(consumer);
        assertSame(handler, consumer.getHandler());
    }

    // -----------------------------------------------------------------------
    // Deferred type variable through abstract class
    // -----------------------------------------------------------------------

    @Test
    public void instanceDeferredTypeVariable() throws Exception {
        final DeferredStringIntHandler handler = new DeferredStringIntHandler("prebuilt");

        final System system = System.builder()
                .add("handler", handler)
                .definition(HandlerConsumer.class, "consumer")
                .build();

        final HandlerConsumer consumer = system.get(HandlerConsumer.class);
        assertNotNull(consumer);
        assertSame(handler, consumer.getHandler());
    }

    // -----------------------------------------------------------------------
    // Bounded type parameter
    // -----------------------------------------------------------------------

    @Test
    public void instanceBoundedTypeParameter() throws Exception {
        final StringSorter sorter = new StringSorter("prebuilt");

        final System system = System.builder()
                .add("sorter", sorter)
                .definition(SorterConsumer.class, "consumer")
                .build();

        final SorterConsumer consumer = system.get(SorterConsumer.class);
        assertNotNull(consumer);
        assertSame(sorter, consumer.getSorter());
    }

    @Test(expected = ConstructionFailedException.class)
    public void instanceBoundedTypeParameterNoMatch() throws Exception {
        final IntSorter sorter = new IntSorter("prebuilt");

        System.builder()
                .add("sorter", sorter)
                .definition(SorterConsumer.class, "consumer")
                .build();
    }

    // -----------------------------------------------------------------------
    // Mixed generics: some from subclass, some from class hierarchy
    // -----------------------------------------------------------------------

    @Test
    public void instanceMixedGenericsMatch() throws Exception {
        // StringBooleanHandler extends BooleanHandler<String>
        //   where BooleanHandler<I> implements Handler<I, Boolean>
        //   String from subclass, Boolean from BooleanHandler hierarchy
        final StringBooleanHandler handler = new StringBooleanHandler("prebuilt");

        final System system = System.builder()
                .add("handler", handler)
                .definition(MixedConsumer.class, "consumer")
                .build();

        final MixedConsumer consumer = system.get(MixedConsumer.class);
        assertNotNull(consumer);
        assertSame(handler, consumer.getHandler());
    }

    @Test(expected = ConstructionFailedException.class)
    public void instanceMixedGenericsNoMatch() throws Exception {
        final IntBooleanHandler handler = new IntBooleanHandler("prebuilt");

        System.builder()
                .add("handler", handler)
                .definition(MixedConsumer.class, "consumer")
                .build();
    }

    @Test(expected = ConstructionFailedException.class)
    public void instanceMixedGenericsWrongFixedType() throws Exception {
        // Consumer wants Handler<String, Integer>
        // StringBooleanHandler has Handler<String, Boolean> — O=Boolean doesn't match Integer
        final StringBooleanHandler handler = new StringBooleanHandler("prebuilt");

        System.builder()
                .add("handler", handler)
                .definition(HandlerConsumer.class, "consumer")
                .build();
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

    public static class SpecialStringIntHandler extends StringIntHandler {
        public SpecialStringIntHandler(final @Name String name) {
            super(name);
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
    // Fixtures — Sorter with bounded type parameter
    // =======================================================================

    public static abstract class Sorter<T extends Comparable<T>> {
        public Sorter(final @Name String name) {
        }

        public abstract T sort();
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

    // =======================================================================
    // Fixtures — Mixed generics (partial binding through class hierarchy)
    // =======================================================================

    public static abstract class BooleanHandler<I> implements RequestHandler<I, Boolean> {
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
