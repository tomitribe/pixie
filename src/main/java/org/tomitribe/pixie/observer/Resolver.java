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

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;

/**
 * Selects the single most-specific type among a set of candidate observed types for a fired
 * event, mirroring the "most specific" rule the JVM uses for overload resolution (JLS 15.12.2.5).
 *
 * The candidates that apply are those assignable from the event's runtime type; the result is
 * the unique minimum among them in the subtype order. Zero matches -> null (nothing observes
 * it). Two or more incomparable minima (a genuine diamond) -> AmbiguousObserverException: like
 * javac, we refuse to guess.
 *
 * This works purely on types - the caller supplies the candidate types (e.g. an observer's
 * registered @Observes parameter types) and maps the winner back to a handler. Matching is on
 * erased types only; generic narrowing (e.g. distinguishing Foo&lt;A&gt; from Foo&lt;B&gt;) is
 * intentionally NOT supported: the JVM does not dispatch on generics, and neither do we.
 */
public class Resolver {

    private Resolver() {
    }

    public static Class bestMatch(final Class<?> eventType, final Collection<Class> candidates) {
        // isAssignableFrom crosses the interface graph for free - that is the whole feature.
        final List<Class> matches = candidates.stream()
                .filter(candidate -> candidate.isAssignableFrom(eventType))
                .collect(toList());

        if (matches.isEmpty()) return null;

        final List<Class> minimal = minimal(matches, type -> type);

        if (minimal.size() == 1) return minimal.get(0);

        throw new AmbiguousObserverException(eventType, minimal); // >= 2 incomparable minima
    }

    public static List<Class> allMatches(final Class<?> eventType, final Collection<Class> candidates) {
        throw new UnsupportedOperationException(); // for future use (every-match mode)
    }

    /**
     * Reduce candidates to the most-specific (the minimal elements in the subtype order): drop
     * any candidate whose type is a STRICT supertype of another candidate's. The strict check
     * (types not equal) matters - without it two candidates sharing a type would each "dominate"
     * the other and both fall out, yielding an empty set rather than a real tie.
     *
     * For a single-inheritance class chain this leaves exactly the most-derived class. For an
     * interface lattice it leaves one element if a unique minimum exists, or several incomparable
     * minima (a genuine diamond) for the caller to treat as ambiguous.
     */
    private static <T> List<T> minimal(final List<T> candidates, final Function<T, Class<?>> typeOf) {
        return candidates.stream()
                .filter(k -> candidates.stream().noneMatch(m -> m != k
                        && !typeOf.apply(k).equals(typeOf.apply(m))
                        && typeOf.apply(k).isAssignableFrom(typeOf.apply(m))))
                .collect(toList());
    }

    public static class AmbiguousObserverException extends RuntimeException {
        public AmbiguousObserverException(final Class<?> eventType, final Collection<Class> types) {
            super("Ambiguous @Observes match for event " + eventType.getName()
                    + ": equally-specific observers " + types.stream().map(Class::getName).collect(toList())
                    + ". Resolve by observing a type they share as a common subtype, or remove one.");
        }
    }
}
