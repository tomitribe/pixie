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
package org.tomitribe.pixie.observer;

import org.junit.Test;
import org.tomitribe.pixie.Observes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Verifies {@link Resolver#bestMatch} against the JVM itself as the oracle.
 *
 * Each happy-path Scenario declares overloaded {@code observe(...)} methods over a type
 * graph and fires an event at its concrete static type. The JVM picks the overload via
 * normal resolution; the invoked body self-reports its own parameter type. We then assert
 * the Resolver's selected type, the type the JVM dispatched into, and the test's stated
 * expectation all agree.
 *
 * Ambiguous and no-match cases cannot use the JVM oracle - an ambiguous call site does not
 * compile (that compile error is the JVM's "answer"), and a no-match call site does not
 * compile either - so those exercise the Resolver directly.
 */
public class ResolverTest {

    /**
     * Base for JVM-witnessed scenarios. The JVM decides which {@code observe} body runs; the
     * body merely names its own parameter type. A mislabel cannot pass silently - it would
     * disagree with the expected type in {@link #assertDispatch}.
     */
    public static class Scenario {
        Method invoked;

        protected void record(final Class<?> parameterType) {
            this.invoked = observeMethod(getClass(), parameterType);
        }
    }

    // ============================================================
    // Happy path - a unique most-specific match exists
    // ============================================================

    /** Most-specific class wins over its supertypes. */
    // @formatter:off
    public static class ClassHierarchy extends Scenario {
        interface Animal {}
        interface Pet extends Animal {}
        public static class Dog implements Pet {}

        public void observe(@Observes final Object o) { record(Object.class); }
        public void observe(@Observes final Animal a) { record(Animal.class); }
        public void observe(@Observes final Pet p)    { record(Pet.class); }
        public void observe(@Observes final Dog d)    { record(Dog.class); }
    }
    // @formatter:on

    @Test
    public void classHierarchy() {
        final ClassHierarchy observer = new ClassHierarchy();
        final ClassHierarchy.Dog event = new ClassHierarchy.Dog();
        observer.observe(event);
        assertDispatch(observer, event, ClassHierarchy.Dog.class);
    }

    /** With no class overload, a sub-interface beats the super-interface it extends. */
    // @formatter:off
    public static class SubInterface extends Scenario {
        interface Animal {}
        interface Pet extends Animal {}
        public static class Dog implements Pet {}

        public void observe(@Observes final Animal a) { record(Animal.class); }
        public void observe(@Observes final Pet p)    { record(Pet.class); }
    }
    // @formatter:on

    @Test
    public void subInterfaceBeatsSuperInterface() {
        final SubInterface observer = new SubInterface();
        final SubInterface.Dog event = new SubInterface.Dog();
        observer.observe(event);
        assertDispatch(observer, event, SubInterface.Pet.class);
    }

    /** A multi-level interface chain resolves to the deepest interface. */
    // @formatter:off
    public static class InterfaceChain extends Scenario {
        interface A1 {}
        interface A2 extends A1 {}
        interface A3 extends A2 {}
        public static class X implements A3 {}

        public void observe(@Observes final A1 a) { record(A1.class); }
        public void observe(@Observes final A2 a) { record(A2.class); }
        public void observe(@Observes final A3 a) { record(A3.class); }
    }
    // @formatter:on

    @Test
    public void deepestInterfaceInChainWins() {
        final InterfaceChain observer = new InterfaceChain();
        final InterfaceChain.X event = new InterfaceChain.X();
        observer.observe(event);
        assertDispatch(observer, event, InterfaceChain.A3.class);
    }

    /** When only a supertype is observed, that supertype is the match. */
    // @formatter:off
    public static class OnlySupertype extends Scenario {
        interface A {}
        interface B extends A {}
        public static class C implements A {} // an A, but not a B

        public void observe(@Observes final A a) { record(A.class); }
        public void observe(@Observes final B b) { record(B.class); }
    }
    // @formatter:on

    @Test
    public void onlyMatchingSupertypeWins() {
        final OnlySupertype observer = new OnlySupertype();
        final OnlySupertype.C event = new OnlySupertype.C();
        observer.observe(event);
        assertDispatch(observer, event, OnlySupertype.A.class);
    }

    /**
     * Two parallel interface chains joined by Y: a textbook diamond that is NOT ambiguous,
     * because the event implements the join Y, which is the unique minimum of the lattice.
     */
    // @formatter:off
    public static class JoinCollapsesDiamond extends Scenario {
        interface A1 {}
        interface A2 extends A1 {}
        interface A3 extends A2 {}
        interface B1 {}
        interface B2 extends B1 {}
        interface B3 extends B2 {}
        interface Y extends A3, B3 {}
        public static class X implements Y {}

        public void observe(@Observes final A1 a) { record(A1.class); }
        public void observe(@Observes final A2 a) { record(A2.class); }
        public void observe(@Observes final A3 a) { record(A3.class); }
        public void observe(@Observes final B1 b) { record(B1.class); }
        public void observe(@Observes final B2 b) { record(B2.class); }
        public void observe(@Observes final B3 b) { record(B3.class); }
        public void observe(@Observes final Y y)  { record(Y.class); }
    }
    // @formatter:on

    @Test
    public void joinTypeCollapsesDiamond() {
        final JoinCollapsesDiamond observer = new JoinCollapsesDiamond();
        final JoinCollapsesDiamond.X event = new JoinCollapsesDiamond.X();
        observer.observe(event);
        assertDispatch(observer, event, JoinCollapsesDiamond.Y.class);
    }

    // ============================================================
    // Candidate gathering - only @Observes types, including inherited
    // ============================================================

    /** A catch-all Object observer matches any event. */
    public static class CatchAll extends Scenario {
        public void observe(@Observes final Object o) { record(Object.class); }
    }

    @Test
    public void catchAllObjectObserverMatchesAnything() {
        final CatchAll observer = new CatchAll();
        final String event = "anything";
        observer.observe(event);
        assertDispatch(observer, event, Object.class);
    }

    /** @Observes methods inherited from a superclass participate in resolution. */
    public static class BaseObserver extends Scenario {
        interface Animal {}
        public void observe(@Observes final Animal a) { record(Animal.class); }
    }

    // @formatter:off
    public static class DerivedObserver extends BaseObserver {
        public static class Dog implements Animal {}
        public static class Cat implements Animal {}

        public void observe(@Observes final Dog d) { record(Dog.class); }
    }
    // @formatter:on

    @Test
    public void inheritedObserverMethodIsUsed() {
        final DerivedObserver observer = new DerivedObserver();
        final DerivedObserver.Cat event = new DerivedObserver.Cat(); // an Animal, not a Dog
        observer.observe(event);
        assertDispatch(observer, event, BaseObserver.Animal.class);
    }

    // ============================================================
    // Ambiguous - two or more incomparable minima (Resolver throws)
    // ============================================================

    /** Two unrelated interfaces, both implemented, no common subtype: a true diamond. */
    // @formatter:off
    public static class InterfaceDiamond {
        interface A {}
        interface B {}
        public static class C implements A, B {}

        public void observe(@Observes final A a) {}
        public void observe(@Observes final B b) {}
    }
    // @formatter:on

    @Test
    public void interfaceDiamondIsAmbiguous() {
        assertAmbiguous(InterfaceDiamond.class, new InterfaceDiamond.C());
    }

    /** Mechanism A: a class gets NO preference over an unrelated interface - it is a tie. */
    // @formatter:off
    public static class ClassVsInterface {
        public static class Base {}
        interface I {}
        public static class Derived extends Base implements I {}

        public void observe(@Observes final Base b) {}
        public void observe(@Observes final I i)    {}
    }
    // @formatter:on

    @Test
    public void classVsUnrelatedInterfaceIsAmbiguous() {
        assertAmbiguous(ClassVsInterface.class, new ClassVsInterface.Derived());
    }

    /**
     * A join type exists in the type system but the event does NOT implement it, so it never
     * enters the candidate set and cannot disambiguate the two incomparable branches.
     */
    // @formatter:off
    public static class DiamondJoinNotImplemented {
        interface J {}
        interface A extends J {}
        interface B extends J {}
        interface Y extends A, B {}            // join exists...
        public static class C implements A, B {} // ...but C is not a Y

        public void observe(@Observes final J j) {}
        public void observe(@Observes final A a) {}
        public void observe(@Observes final B b) {}
        public void observe(@Observes final Y y) {}
    }
    // @formatter:on

    @Test
    public void unimplementedJoinDoesNotDisambiguate() {
        assertAmbiguous(DiamondJoinNotImplemented.class, new DiamondJoinNotImplemented.C());
    }

    /** The ambiguity message must name the colliding types so the user can fix it. */
    @Test
    public void ambiguousMessageNamesCollidingTypes() {
        try {
            final Class<?> actual = Resolver.bestMatch(InterfaceDiamond.C.class, candidates(InterfaceDiamond.class));
            fail("Expected AmbiguousObserverException but resolved to " + actual);
        } catch (final Resolver.AmbiguousObserverException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("$A"));
            assertTrue(e.getMessage(), e.getMessage().contains("$B"));
        }
    }

    // ============================================================
    // No match - nothing observes the event (Resolver returns null)
    // ============================================================

    // @formatter:off
    public static class NoMatch {
        public static class Dog {}

        public void observe(@Observes final Dog d) {}
    }
    // @formatter:on

    @Test
    public void unobservedEventReturnsNull() {
        assertNull(Resolver.bestMatch(String.class, candidates(NoMatch.class)));
    }

    // ============================================================
    // Helpers
    // ============================================================

    /** Asserts the Resolver's selected type, the JVM's pick, and the expectation all agree. */
    private void assertDispatch(final Scenario observer, final Object event, final Class<?> expectedParam) {
        final Class<?> jvm = observer.invoked.getParameterTypes()[0];   // overload the JVM ran
        final Class<?> ours = Resolver.bestMatch(event.getClass(), candidates(observer.getClass()));

        assertEquals("JVM pick", expectedParam, jvm);
        assertEquals("Resolver pick", expectedParam, ours);
    }

    private static void assertAmbiguous(final Class<?> observer, final Object event) {
        try {
            final Class<?> actual = Resolver.bestMatch(event.getClass(), candidates(observer));
            fail("Expected AmbiguousObserverException but resolved to " + actual);
        } catch (final Resolver.AmbiguousObserverException expected) {
            // correct - the Resolver refused to guess
        }
    }

    /** The @Observes parameter types an observer registers - what bestMatch chooses among. */
    private static Collection<Class> candidates(final Class<?> observer) {
        final List<Class> types = new ArrayList<>();
        for (final Method method : observer.getMethods()) {
            if (isObserver(method)) types.add(method.getParameterTypes()[0]);
        }
        return types;
    }

    private static boolean isObserver(final Method method) {
        if (method.getParameterCount() != 1) return false;
        for (final Annotation annotation : method.getParameterAnnotations()[0]) {
            if (annotation.annotationType().equals(Observes.class)) return true;
        }
        return false;
    }

    private static Method observeMethod(final Class<?> type, final Class<?> param) {
        try {
            return type.getMethod("observe", param);
        } catch (final NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }
}
