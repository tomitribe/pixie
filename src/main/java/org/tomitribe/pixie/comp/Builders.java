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

import org.tomitribe.util.reflect.Generics;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

public class Builders {

    private Builders() {
    }

    public static Type resolveBuiltType(final Method builderMethod,
                                        final Class<?> builderClass,
                                        final Method buildMethod) {

        final Type genericReturn = buildMethod.getGenericReturnType();

        if (genericReturn instanceof TypeVariable<?>) {
            // build() returns T — resolve T through builder return type
            final TypeVariable<?> returnVariable = (TypeVariable<?>) genericReturn;
            final Type builderReturnType = builderMethod.getGenericReturnType();
            final Type resolved = resolveTypeVariableFromBuilderReturn(builderReturnType, buildMethod, returnVariable);

            if (resolved == null) {
                throw new InvalidBuildMethodException(builderClass);
            }

            return resolved;
        }

        if (genericReturn instanceof ParameterizedType) {
            // build() returns Foo<I, O> — resolve any TypeVariables in the type arguments
            return resolveParameterizedReturn((ParameterizedType) genericReturn, builderMethod, buildMethod);
        }

        // build() returns a concrete, non-generic type
        return genericReturn;
    }

    private static Type resolveParameterizedReturn(final ParameterizedType returnType,
                                                   final Method builderMethod,
                                                   final Method buildMethod) {
        final Type[] args = returnType.getActualTypeArguments();
        final Type builderReturnType = builderMethod.getGenericReturnType();
        boolean modified = false;
        final Type[] resolved = new Type[args.length];

        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof TypeVariable<?>) {
                final Type r = resolveTypeVariableFromBuilderReturn(
                        builderReturnType, buildMethod, (TypeVariable<?>) args[i]);
                if (r != null) {
                    resolved[i] = r;
                    modified = true;
                    continue;
                }
            }
            resolved[i] = args[i];
        }

        if (!modified) return returnType;

        final ParameterizedType original = returnType;
        return new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return resolved;
            }

            @Override
            public Type getRawType() {
                return original.getRawType();
            }

            @Override
            public Type getOwnerType() {
                return original.getOwnerType();
            }

            @Override
            public String toString() {
                return original.getRawType().getTypeName() + "<" +
                        java.util.Arrays.stream(resolved)
                                .map(Type::getTypeName)
                                .reduce((a, b) -> a + ", " + b)
                                .orElse("") + ">";
            }

            @Override
            public boolean equals(final Object obj) {
                if (!(obj instanceof ParameterizedType)) return false;
                final ParameterizedType other = (ParameterizedType) obj;
                return original.getRawType().equals(other.getRawType())
                        && java.util.Arrays.equals(resolved, other.getActualTypeArguments());
            }

            @Override
            public int hashCode() {
                return java.util.Arrays.hashCode(resolved) ^ original.getRawType().hashCode();
            }
        };
    }

    private static Type resolveTypeVariableFromBuilderReturn(final Type builderReturnType,
                                                             final Method buildMethod,
                                                             final TypeVariable<?> returnVariable) {

        // Case 1: builder method returns ProxyBuilder<Person> (or ProxyBuilder<? extends Person>)
        if (builderReturnType instanceof ParameterizedType) {
            final ParameterizedType pt = (ParameterizedType) builderReturnType;
            return resolveFromParameterizedBuilderReturn(pt, buildMethod.getDeclaringClass(), returnVariable);
        }

        // Case 2: builder method returns a concrete subclass (PersonBuilder.class) that binds T in its supertypes
        if (builderReturnType instanceof Class<?>) {
            final Class<?> returnedBuilderClass = (Class<?>) builderReturnType;

            final Type[] args = Generics.getTypeParameters(buildMethod.getDeclaringClass(), returnedBuilderClass);
            if (args == null || args.length == 0) {
                return null;
            }

            // Most common case: build() returns the first type var.
            return args[0];
        }

        return null;
    }

    private static Type resolveFromParameterizedBuilderReturn(final ParameterizedType builderReturnType,
                                                              final Class<?> typeVariableOwner,
                                                              final TypeVariable<?> returnVariable) {

        final Type raw = builderReturnType.getRawType();
        if (!(raw instanceof Class<?>)) {
            return null;
        }

        final TypeVariable<?>[] vars = typeVariableOwner.getTypeParameters();
        final Type[] args = builderReturnType.getActualTypeArguments();

        for (int i = 0; i < vars.length && i < args.length; i++) {
            if (!vars[i].equals(returnVariable)) {
                continue;
            }

            return resolveWildcard(args[i]);
        }

        return null;
    }

    /**
     * Resolve wildcard types to their most specific bound for instantiation purposes.
     * For matching/assignability purposes, the full Type should be used instead.
     */
    static Type resolveWildcard(final Type type) {
        if (type instanceof WildcardType) {
            final WildcardType wildcard = (WildcardType) type;

            final Type[] lower = wildcard.getLowerBounds();
            if (lower != null && lower.length == 1) {
                return resolveWildcard(lower[0]);
            }

            final Type[] upper = wildcard.getUpperBounds();
            if (upper != null && upper.length == 1) {
                return resolveWildcard(upper[0]);
            }

            return Object.class;
        }

        return type;
    }

    public static Class<?> toClass(final Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }

        if (type instanceof ParameterizedType) {
            final Type raw = ((ParameterizedType) type).getRawType();
            return (raw instanceof Class<?>) ? (Class<?>) raw : null;
        }

        if (type instanceof WildcardType) {
            return toClass(resolveWildcard(type));
        }

        // TypeVariable, GenericArrayType, etc: not resolvable to a concrete Class here.
        return null;
    }
}
