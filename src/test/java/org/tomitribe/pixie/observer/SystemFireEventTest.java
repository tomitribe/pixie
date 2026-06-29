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
import org.tomitribe.pixie.System;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * The same scenarios as {@link ResolverTest}, but driven end to end through the real Pixie
 * {@link System}: the observer is built and registered by Pixie, and the event is delivered
 * via {@link System#fireEvent}. This confirms the Resolver wiring is actually reached on a
 * live fire - not just that the algorithm is correct in isolation.
 */
public class SystemFireEventTest {

    /** Records which @Observes method the live fire dispatched into. */
    public static class Probe {
        String observed;
    }

    // ===== Happy path =====

    // @formatter:off
    public static class ClassHierarchy extends Probe {
        public interface Animal {}
        public interface Pet extends Animal {}
        public static class Dog implements Pet {}

        public void observe(@Observes final Object o) { observed = "Object"; }
        public void observe(@Observes final Animal a) { observed = "Animal"; }
        public void observe(@Observes final Pet p)    { observed = "Pet"; }
        public void observe(@Observes final Dog d)    { observed = "Dog"; }
    }
    // @formatter:on

    @Test
    public void mostSpecificClassWins() {
        assertFires(ClassHierarchy.class, new ClassHierarchy.Dog(), "Dog");
    }

    // @formatter:off
    public static class SubInterface extends Probe {
        public interface Animal {}
        public interface Pet extends Animal {}
        public static class Dog implements Pet {}

        public void observe(@Observes final Animal a) { observed = "Animal"; }
        public void observe(@Observes final Pet p)    { observed = "Pet"; }
    }
    // @formatter:on

    @Test
    public void subInterfaceBeatsSuperInterface() {
        assertFires(SubInterface.class, new SubInterface.Dog(), "Pet");
    }

    // @formatter:off
    public static class InterfaceChain extends Probe {
        public interface A1 {}
        public interface A2 extends A1 {}
        public interface A3 extends A2 {}
        public static class X implements A3 {}

        public void observe(@Observes final A1 a) { observed = "A1"; }
        public void observe(@Observes final A2 a) { observed = "A2"; }
        public void observe(@Observes final A3 a) { observed = "A3"; }
    }
    // @formatter:on

    @Test
    public void deepestInterfaceInChainWins() {
        assertFires(InterfaceChain.class, new InterfaceChain.X(), "A3");
    }

    // @formatter:off
    public static class OnlySupertype extends Probe {
        public interface A {}
        public interface B extends A {}
        public static class C implements A {} // an A, but not a B

        public void observe(@Observes final A a) { observed = "A"; }
        public void observe(@Observes final B b) { observed = "B"; }
    }
    // @formatter:on

    @Test
    public void onlyMatchingSupertypeWins() {
        assertFires(OnlySupertype.class, new OnlySupertype.C(), "A");
    }

    // @formatter:off
    public static class JoinCollapsesDiamond extends Probe {
        public interface A1 {}
        public interface A2 extends A1 {}
        public interface A3 extends A2 {}
        public interface B1 {}
        public interface B2 extends B1 {}
        public interface B3 extends B2 {}
        public interface Y extends A3, B3 {}
        public static class X implements Y {}

        public void observe(@Observes final A1 a) { observed = "A1"; }
        public void observe(@Observes final A2 a) { observed = "A2"; }
        public void observe(@Observes final A3 a) { observed = "A3"; }
        public void observe(@Observes final B1 b) { observed = "B1"; }
        public void observe(@Observes final B2 b) { observed = "B2"; }
        public void observe(@Observes final B3 b) { observed = "B3"; }
        public void observe(@Observes final Y y)  { observed = "Y"; }
    }
    // @formatter:on

    @Test
    public void joinTypeCollapsesDiamond() {
        assertFires(JoinCollapsesDiamond.class, new JoinCollapsesDiamond.X(), "Y");
    }

    // ===== Ambiguous - fireEvent throws =====

    // @formatter:off
    public static class InterfaceDiamond extends Probe {
        public interface A {}
        public interface B {}
        public static class C implements A, B {}

        public void observe(@Observes final A a) { observed = "A"; }
        public void observe(@Observes final B b) { observed = "B"; }
    }
    // @formatter:on

    @Test
    public void interfaceDiamondThrowsOnFire() {
        assertAmbiguous(InterfaceDiamond.class, new InterfaceDiamond.C());
    }

    // @formatter:off
    public static class ClassVsInterface extends Probe {
        public static class Base {}
        public interface I {}
        public static class Derived extends Base implements I {}

        public void observe(@Observes final Base b) { observed = "Base"; }
        public void observe(@Observes final I i)    { observed = "I"; }
    }
    // @formatter:on

    @Test
    public void classVsUnrelatedInterfaceThrowsOnFire() {
        assertAmbiguous(ClassVsInterface.class, new ClassVsInterface.Derived());
    }

    // @formatter:off
    public static class DiamondJoinNotImplemented extends Probe {
        public interface J {}
        public interface A extends J {}
        public interface B extends J {}
        public interface Y extends A, B {}            // join exists...
        public static class C implements A, B {}      // ...but C is not a Y

        public void observe(@Observes final J j) { observed = "J"; }
        public void observe(@Observes final A a) { observed = "A"; }
        public void observe(@Observes final B b) { observed = "B"; }
        public void observe(@Observes final Y y) { observed = "Y"; }
    }
    // @formatter:on

    @Test
    public void unimplementedJoinThrowsOnFire() {
        assertAmbiguous(DiamondJoinNotImplemented.class, new DiamondJoinNotImplemented.C());
    }

    // ===== No match - fire is silent =====

    @Test
    public void unobservedEventIsSilent() {
        final System system = System.builder().definition(SubInterface.class).build();
        final SubInterface observer = system.get(SubInterface.class);

        system.fireEvent("not an animal"); // nothing observes String

        assertNull(observer.observed);
    }

    // ===== Helpers =====

    private static void assertFires(final Class<? extends Probe> type, final Object event, final String expected) {
        final System system = System.builder().definition(type).build();
        final Probe observer = system.get(type);

        system.fireEvent(event);

        assertEquals(expected, observer.observed);
    }

    private static void assertAmbiguous(final Class<? extends Probe> type, final Object event) {
        final System system = System.builder().definition(type).build();

        try {
            system.fireEvent(event);
            fail("Expected AmbiguousObserverException");
        } catch (final Resolver.AmbiguousObserverException expected) {
            // correct - the Resolver refused to guess, surfaced through System.fireEvent
        }
    }
}
