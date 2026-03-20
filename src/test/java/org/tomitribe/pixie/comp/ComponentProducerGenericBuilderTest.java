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
import org.tomitribe.pixie.Name;
import org.tomitribe.pixie.Nullable;
import org.tomitribe.pixie.Param;
import org.tomitribe.pixie.System;

/**
 * Tests that builder declarations correctly expose their generic
 * produced type for matching. Covers:
 * - Builder where build() returns T (single TypeVariable)
 * - Builder where build() returns a ParameterizedType with TypeVariables
 * - Bounded type parameters on builder classes
 * - Builder returning a subtype with inherited generics
 */
public class ComponentProducerGenericBuilderTest extends Assert {

    // -----------------------------------------------------------------------
    // build() returns T — single TypeVariable
    // -----------------------------------------------------------------------

    @Test
    public void builderReturningTypeVariable() throws Exception {
        final System system = System.builder()
                .definition(HandlerConsumer.class, "consumer")
                .definition(StringIntBuilderHost.class, "handler")
                .build();

        final HandlerConsumer consumer = system.get(HandlerConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.getHandler());
    }

    @Test(expected = ConstructionFailedException.class)
    public void builderReturningTypeVariableRejectsWrong() throws Exception {
        System.builder()
                .definition(HandlerConsumer.class, "consumer")
                .definition(LongBoolBuilderHost.class, "handler")
                .build();
    }

    // -----------------------------------------------------------------------
    // build() returns ParameterizedType with TypeVariables
    // -----------------------------------------------------------------------

    @Test
    public void builderReturningParameterizedTypeWithTypeVariables() throws Exception {
        final System system = System.builder()
                .definition(HandlerConsumer.class, "consumer")
                .definition(ParameterizedStringIntBuilderHost.class, "handler")
                .build();

        final HandlerConsumer consumer = system.get(HandlerConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.getHandler());
    }

    @Test(expected = ConstructionFailedException.class)
    public void builderReturningParameterizedTypeRejectsWrong() throws Exception {
        System.builder()
                .definition(HandlerConsumer.class, "consumer")
                .definition(ParameterizedLongBoolBuilderHost.class, "handler")
                .build();
    }

    // -----------------------------------------------------------------------
    // Bounded type parameter
    // -----------------------------------------------------------------------

    @Test
    public void builderWithBoundedTypeParameter() throws Exception {
        final System system = System.builder()
                .definition(SorterConsumer.class, "consumer")
                .definition(StringSorterBuilderHost.class, "sorter")
                .build();

        final SorterConsumer consumer = system.get(SorterConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.getSorter());
    }

    @Test(expected = ConstructionFailedException.class)
    public void builderWithBoundedTypeParameterRejectsWrong() throws Exception {
        System.builder()
                .definition(SorterConsumer.class, "consumer")
                .definition(IntSorterBuilderHost.class, "sorter")
                .build();
    }

    // -----------------------------------------------------------------------
    // Builder returning a subtype with inherited generics
    // -----------------------------------------------------------------------

    @Test
    public void builderReturningSubtypeWithInheritedGenerics() throws Exception {
        final System system = System.builder()
                .definition(HandlerConsumer.class, "consumer")
                .definition(ImprovedStringIntBuilderHost.class, "handler")
                .build();

        final HandlerConsumer consumer = system.get(HandlerConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.getHandler());
    }

    @Test(expected = ConstructionFailedException.class)
    public void builderReturningSubtypeWithWrongGenerics() throws Exception {
        System.builder()
                .definition(HandlerConsumer.class, "consumer")
                .definition(ImprovedLongBoolBuilderHost.class, "handler")
                .build();
    }

    // -----------------------------------------------------------------------
    // Bounded type parameter — multi-level (builder produces a deeper subtype)
    // -----------------------------------------------------------------------

    @Test
    public void builderReturningSubtypeOfBoundedSuperclass() throws Exception {
        final System system = System.builder()
                .definition(SorterConsumer.class, "consumer")
                .definition(SpecialStringSorterBuilderHost.class, "sorter")
                .build();

        final SorterConsumer consumer = system.get(SorterConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.getSorter());
    }

    @Test(expected = ConstructionFailedException.class)
    public void builderReturningSubtypeOfBoundedSuperclassRejectsWrong() throws Exception {
        System.builder()
                .definition(SorterConsumer.class, "consumer")
                .definition(SpecialIntSorterBuilderHost.class, "sorter")
                .build();
    }

    // -----------------------------------------------------------------------
    // Mixed generics: some from builder declaration, some from class hierarchy
    // -----------------------------------------------------------------------

    @Test
    public void builderMixedGenericsMatch() throws Exception {
        // Consumer wants Handler<String, Boolean>
        // Builder produces BooleanHandler<String> where:
        //   String comes from builder method return type
        //   Boolean comes from BooleanHandler's class hierarchy (implements Handler<I, Boolean>)
        final System system = System.builder()
                .definition(MixedConsumer.class, "consumer")
                .definition(StringBooleanHandlerBuilderHost.class, "handler")
                .build();

        final MixedConsumer consumer = system.get(MixedConsumer.class);
        assertNotNull(consumer);
        assertNotNull(consumer.getHandler());
    }

    @Test(expected = ConstructionFailedException.class)
    public void builderMixedGenericsNoMatch() throws Exception {
        // Consumer wants Handler<String, Boolean>
        // Builder produces BooleanHandler<Integer> — I=Integer doesn't match String
        System.builder()
                .definition(MixedConsumer.class, "consumer")
                .definition(IntBooleanHandlerBuilderHost.class, "handler")
                .build();
    }

    @Test(expected = ConstructionFailedException.class)
    public void builderMixedGenericsWrongFixedType() throws Exception {
        // Consumer wants Handler<String, Integer>
        // Builder produces BooleanHandler<String> — O=Boolean (from hierarchy) doesn't match Integer
        System.builder()
                .definition(HandlerConsumer.class, "consumer")
                .definition(StringBooleanHandlerBuilderHost.class, "handler")
                .build();
    }

    // =======================================================================
    // Fixtures — RequestHandler with simple T builder (build() returns T)
    // =======================================================================

    public interface RequestHandler<I, O> {
        O handle(I input);
    }

    public interface ImprovedHandler<I, O> extends RequestHandler<I, O> {
    }

    public static class SimpleBuilder<T> {
        private final T instance;

        public SimpleBuilder(final T instance) {
            this.instance = instance;
        }

        public SimpleBuilder<T> configure(@Param("config") @Nullable final String config) {
            return this;
        }

        public T build() {
            return instance;
        }
    }

    public static class StringIntBuilderHost {
        private StringIntBuilderHost() {
        }

        @Builder
        public static SimpleBuilder<RequestHandler<String, Integer>> builder() {
            return new SimpleBuilder<>((RequestHandler<String, Integer>) input -> input.length());
        }
    }

    public static class LongBoolBuilderHost {
        private LongBoolBuilderHost() {
        }

        @Builder
        public static SimpleBuilder<RequestHandler<Long, Boolean>> builder() {
            return new SimpleBuilder<>((RequestHandler<Long, Boolean>) input -> input > 0);
        }
    }

    // =======================================================================
    // Fixtures — RequestHandler with parameterized return builder
    //            (build() returns RequestHandler<I, O>)
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

    public static class ParameterizedStringIntBuilderHost {
        private ParameterizedStringIntBuilderHost() {
        }

        @Builder
        public static GenericHandlerBuilder<String, Integer> builder() {
            return new GenericHandlerBuilder<>();
        }
    }

    public static class ParameterizedLongBoolBuilderHost {
        private ParameterizedLongBoolBuilderHost() {
        }

        @Builder
        public static GenericHandlerBuilder<Long, Boolean> builder() {
            return new GenericHandlerBuilder<>();
        }
    }

    // =======================================================================
    // Fixtures — Builder returning subtype (ImprovedHandler extends RequestHandler)
    // =======================================================================

    public static class ImprovedHandlerBuilder<I, O> {
        private String label;

        public ImprovedHandlerBuilder<I, O> label(@Param("label") @Nullable final String label) {
            this.label = label;
            return this;
        }

        public ImprovedHandler<I, O> build() {
            return (I input) -> null;
        }
    }

    public static class ImprovedStringIntBuilderHost {
        private ImprovedStringIntBuilderHost() {
        }

        @Builder
        public static ImprovedHandlerBuilder<String, Integer> builder() {
            return new ImprovedHandlerBuilder<>();
        }
    }

    public static class ImprovedLongBoolBuilderHost {
        private ImprovedLongBoolBuilderHost() {
        }

        @Builder
        public static ImprovedHandlerBuilder<Long, Boolean> builder() {
            return new ImprovedHandlerBuilder<>();
        }
    }

    // =======================================================================
    // Fixtures — Sorter with bounded type parameter builder
    // =======================================================================

    public static abstract class Sorter<T extends Comparable<T>> {
        public abstract T sort();
    }

    public static class SorterBuilder<T extends Comparable<T>> {
        private String label;

        public SorterBuilder<T> label(@Param("label") @Nullable final String label) {
            this.label = label;
            return this;
        }

        public Sorter<T> build() {
            return new Sorter<T>() {
                @Override
                public T sort() {
                    return null;
                }
            };
        }
    }

    public static class StringSorterBuilderHost {
        private StringSorterBuilderHost() {
        }

        @Builder
        public static SorterBuilder<String> builder() {
            return new SorterBuilder<>();
        }
    }

    public static class IntSorterBuilderHost {
        private IntSorterBuilderHost() {
        }

        @Builder
        public static SorterBuilder<Integer> builder() {
            return new SorterBuilder<>();
        }
    }

    // =======================================================================
    // Fixtures — Builder returning subtype of bounded Sorter
    // =======================================================================

    public static abstract class SpecialSorter<T extends Comparable<T>> extends Sorter<T> {
    }

    public static class SpecialSorterBuilder<T extends Comparable<T>> {
        private String label;

        public SpecialSorterBuilder<T> label(@Param("label") @Nullable final String label) {
            this.label = label;
            return this;
        }

        public SpecialSorter<T> build() {
            return new SpecialSorter<T>() {
                @Override
                public T sort() {
                    return null;
                }
            };
        }
    }

    public static class SpecialStringSorterBuilderHost {
        private SpecialStringSorterBuilderHost() {
        }

        @Builder
        public static SpecialSorterBuilder<String> builder() {
            return new SpecialSorterBuilder<>();
        }
    }

    public static class SpecialIntSorterBuilderHost {
        private SpecialIntSorterBuilderHost() {
        }

        @Builder
        public static SpecialSorterBuilder<Integer> builder() {
            return new SpecialSorterBuilder<>();
        }
    }

    // =======================================================================
    // Fixtures — Mixed generics (partial binding through class hierarchy)
    // =======================================================================

    /**
     * BooleanHandler fixes O=Boolean but leaves I open.
     * The builder method's return type binds I.
     */
    public static abstract class BooleanHandler<I> implements RequestHandler<I, Boolean> {
    }

    public static class BooleanHandlerBuilder<I> {
        private String label;

        public BooleanHandlerBuilder<I> label(@Param("label") @Nullable final String label) {
            this.label = label;
            return this;
        }

        public BooleanHandler<I> build() {
            return new BooleanHandler<I>() {
                @Override
                public Boolean handle(final I input) {
                    return input != null;
                }
            };
        }
    }

    public static class StringBooleanHandlerBuilderHost {
        private StringBooleanHandlerBuilderHost() {
        }

        @Builder
        public static BooleanHandlerBuilder<String> builder() {
            return new BooleanHandlerBuilder<>();
        }
    }

    public static class IntBooleanHandlerBuilderHost {
        private IntBooleanHandlerBuilderHost() {
        }

        @Builder
        public static BooleanHandlerBuilder<Integer> builder() {
            return new BooleanHandlerBuilder<>();
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
