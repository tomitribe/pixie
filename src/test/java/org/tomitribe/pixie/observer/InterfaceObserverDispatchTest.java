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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Integration coverage for interface @Observes dispatch through the full ObserverManager
 * (register + fireEvent), now that {@link Resolver} is wired into resolution. The fine-grained
 * resolution rules are unit-tested in {@link ResolverTest}; this confirms they actually drive
 * a live fire.
 */
public class InterfaceObserverDispatchTest {

    interface Animal {}
    interface Pet extends Animal {}
    public static class Dog implements Pet {}

    /** A sub-interface observer is selected over the super-interface for a fired event. */
    public static class Observer {
        String observed;

        public void observe(@Observes final Animal a) { observed = "Animal"; }
        public void observe(@Observes final Pet p)    { observed = "Pet"; }
    }

    @Test
    public void mostSpecificInterfaceIsInvoked() {
        final ObserverManager observers = new ObserverManager();
        final Observer observer = new Observer();
        observers.addObserver(observer);

        observers.fireEvent(new Dog());

        assertEquals("Pet", observer.observed);
    }

    /** Two incomparable interface observers with no common subtype: a genuine diamond. */
    interface A {}
    interface B {}
    public static class C implements A, B {}

    public static class Ambiguous {
        public void observe(@Observes final A a) {}
        public void observe(@Observes final B b) {}
    }

    @Test
    public void diamondThrowsOnFire() {
        final ObserverManager observers = new ObserverManager();
        observers.addObserver(new Ambiguous());

        try {
            observers.fireEvent(new C());
            fail("Expected AmbiguousObserverException");
        } catch (final Resolver.AmbiguousObserverException e) {
            assertEquals(true, e.getMessage().contains("$A"));
            assertEquals(true, e.getMessage().contains("$B"));
        }
    }

    /**
     * A non-@Observes method must never be dispatched, even when its parameter is the
     * most-specific match. The Observer constructor only registers @Observes methods, so
     * handle(Dog) never enters the map and observe(Animal) is selected. This guards the
     * filter at its production home (registration), now that Resolver no longer filters.
     */
    public static class HasUnannotated {
        String dispatched;

        public void observe(@Observes final Animal a) { dispatched = "observe"; }
        public void handle(final Dog d) { dispatched = "handle"; } // NOT @Observes
    }

    @Test
    public void unannotatedMethodIsNeverDispatched() {
        final ObserverManager observers = new ObserverManager();
        final HasUnannotated observer = new HasUnannotated();
        observers.addObserver(observer);

        observers.fireEvent(new Dog());

        assertEquals("observe", observer.dispatched);
    }
}
