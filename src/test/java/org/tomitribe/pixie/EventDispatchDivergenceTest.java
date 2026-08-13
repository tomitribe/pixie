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
package org.tomitribe.pixie;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reproduces the divergence described in HANDOFF-event-dispatch-divergence.md.
 *
 * Publishing the SAME event object reaches DIFFERENT observers depending on how
 * it is published:
 *
 *   - system.fireEvent(event)          -> dispatches on event.getClass() (runtime type)
 *   - @Event Consumer<T> bus; bus.accept(event) -> dispatches on the static generic T
 *
 * A Consumer<Object> therefore starts its up-walk at Object and never reaches an
 * observer registered under a subtype such as PatchCreated.
 *
 * These tests assert the EXPECTED (consistent) behaviour: both publish paths should
 * dispatch on the event's runtime type. They FAIL today, pinning the bug.
 *
 * The event hierarchy also has a SIBLING subtype PatchUpdated (both extend PatchChanged).
 * Publishing a PatchCreated must never reach an observer registered only on PatchUpdated:
 * dispatch walks UP the hierarchy (to supertypes), never sideways to siblings nor down to
 * subtypes. This guards against the "over-deliver" fix the handoff warns against.
 */
public class EventDispatchDivergenceTest extends Assert {

    private static final List<String> observed = new ArrayList<>();

    /** Control: fireEvent dispatches on the runtime type, so the concrete observer fires. */
    @Test
    public void fireEventReachesConcreteObserver() {
        final System system = System.builder()
                .definition(Probe.class)
                .definition(Logger.class)
                .definition(Publisher.class)
                .build();

        observed.clear();

        final Publisher publisher = system.get(Publisher.class);
        publisher.fireEvent(new PatchCreated());

        assertObserved(system, "concrete observer should fire via fireEvent");
    }

    /**
     * Bug: publishing a PatchCreated through an injected Consumer<Object> dispatches on
     * the static type Object, so the @Observes PatchCreated observer is never invoked.
     *
     * Expected behaviour (asserted here) mirrors fireEvent: the concrete observer fires.
     * FAILS today (probe.created == 0).
     */
    @Test
    public void consumerOfObjectReachesConcreteObserver() {
        final System system = System.builder()
                .definition(Probe.class)
                .definition(Logger.class)
                .definition(Publisher.class)
                .build();

        observed.clear();

        final Publisher publisher = system.get(Publisher.class);
        publisher.accept(new PatchCreated());

        assertObserved(system, "concrete observer should fire via Consumer<Object>");
    }

    private static void assertObserved(final System system, final String message) {
        final String actual = observed.stream().sorted()
                .reduce((s, s2) -> s + "\n" + s2)
                .orElse("");

        assertEquals("" +
                "Logger.onAny(@Observes final Object e)\n" +
                "Probe.onCreated(@Observes final PatchCreated e)", actual);
    }

    // --- event hierarchy: PatchCreated extends PatchChanged (mirrors harminie) ---

    public static class PatchChanged {
    }

    public static class PatchCreated extends PatchChanged {
    }

    public static class PatchUpdated extends PatchChanged {
    }

    public static class Probe {

        public void onCreated(@Observes final PatchCreated e) {
            observed.add("Probe.onCreated(@Observes final PatchCreated e)");
        }

        public void onUpdated(@Observes final PatchUpdated e) {
            observed.add("Probe.onUpdated(@Observes final PatchUpdated e)");
        }

        public void onAny(@Observes final Object e) {
            observed.add("Probe.onAny(@Observes final Object e)");
        }
    }

    /**
     * A SEPARATE observer (distinct from Probe) that catches every event via @Observes Object.
     * Because most-specific matching is per-observer, this one is NOT shadowed by Probe.onCreated:
     * a distinct observer contributes its own best match, so it should fire for a PatchCreated too.
     */
    public static class Logger {
        public void onAny(@Observes final Object e) {
            observed.add("Logger.onAny(@Observes final Object e)");
        }
    }

    public static class Publisher {
        private final Consumer<Object> bus;
        private final System system;

        public Publisher(@Event final Consumer<Object> bus, @Component @Param("system") final System system) {
            this.bus = bus;
            this.system = system;
        }

        public void accept(final Object e) {
            bus.accept(e);
        }

        public void fireEvent(final Object e) {
            system.fireEvent(e);
        }
    }
}