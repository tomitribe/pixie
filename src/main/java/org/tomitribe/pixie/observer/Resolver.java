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

import org.tomitribe.pixie.Observes;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * Resolves the single best-matching @Observes method for a fired event, mirroring the
 * "most specific" rule the JVM uses for overload resolution (JLS 15.12.2.5).
 *
 * The matched set is every @Observes method whose parameter type is a supertype of the
 * event's runtime type. Among those, the result is the unique minimum in the subtype
 * order. Zero matches -> null (nothing observes it). Two or more incomparable minima
 * (a genuine diamond) -> AmbiguousObserverException: like javac, we refuse to guess.
 *
 * Matching is on erased parameter types only. Generic narrowing (e.g. distinguishing
 * Foo&lt;A&gt; from Foo&lt;B&gt;) is intentionally NOT supported: the JVM does not dispatch
 * on generics, and neither do we.
 */
public class Resolver {

    private Resolver() {
    }

    public static Method bestMatch(final Class<?> observer, final Object event) {
        final Class<?> type = event.getClass();

        // Candidates: @Observes methods whose parameter is a supertype of the fired type.
        // isAssignableFrom crosses the interface graph for free - that is the whole feature.
        final List<Method> matches = Stream.of(observer.getMethods())
                .filter(Resolver::isObserver)
                .filter(method -> paramType(method).isAssignableFrom(type))
                .collect(toList());

        if (matches.isEmpty()) return null;

        // Reduce to the most-specific: drop any candidate whose parameter is a STRICT
        // supertype of another candidate's. The strict check (types not equal) matters -
        // without it two methods sharing a parameter type would each "dominate" the other
        // and both fall out, yielding an empty set rather than a real ambiguity.
        final List<Method> minimal = matches.stream()
                .filter(k -> matches.stream().noneMatch(m -> m != k
                        && !paramType(k).equals(paramType(m))
                        && paramType(k).isAssignableFrom(paramType(m))))
                .collect(toList());

        if (minimal.size() == 1) return minimal.get(0);

        throw new AmbiguousObserverException(event, minimal); // >= 2 incomparable minima
    }

    public static List<Method> allMatches(final Class<?> observer, final Object event) {
        throw new UnsupportedOperationException(); // for future use (every-match mode)
    }

    private static boolean isObserver(final Method method) {
        if (method.getParameterCount() != 1) return false;
        for (final Annotation annotation : method.getParameterAnnotations()[0]) {
            if (annotation.annotationType().equals(Observes.class)) return true;
        }
        return false;
    }

    private static Class<?> paramType(final Method method) {
        return method.getParameterTypes()[0];
    }

    public static class AmbiguousObserverException extends RuntimeException {
        public AmbiguousObserverException(final Object event, final List<Method> minimal) {
            super(message(event, minimal));
        }

        private static String message(final Object event, final List<Method> minimal) {
            final String methods = minimal.stream()
                    .map(method -> method.getDeclaringClass().getSimpleName() + "."
                            + method.getName() + "(" + paramType(method).getSimpleName() + ")")
                    .collect(joining(", "));
            return "Ambiguous @Observes match for event " + event.getClass().getName()
                    + ": equally-specific observer methods [" + methods + "]."
                    + " Resolve by observing a type they share as a common subtype, or remove one.";
        }
    }
}
