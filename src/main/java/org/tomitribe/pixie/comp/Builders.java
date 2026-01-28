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

    public static Class<?> resolveBuiltType(final Method builderMethod,
                                            final Class<?> builderClass,
                                            final Method buildMethod) {

        final Type genericReturn = buildMethod.getGenericReturnType();
        if (!(genericReturn instanceof TypeVariable<?>)) {
            return buildMethod.getReturnType();
        }

        final TypeVariable<?> returnVariable = (TypeVariable<?>) genericReturn;

        final Type builderReturnType = builderMethod.getGenericReturnType();
        final Class<?> resolved = resolveTypeVariableFromBuilderReturn(builderReturnType, buildMethod, returnVariable);

        if (resolved == null) {
            throw new InvalidBuildMethodException(builderClass);
        }

        return resolved;
    }

    private static Class<?> resolveTypeVariableFromBuilderReturn(final Type builderReturnType,
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
            return toClass(args[0]);
        }

        return null;
    }

    private static Class<?> resolveFromParameterizedBuilderReturn(final ParameterizedType builderReturnType,
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

            return toClass(args[i]);
        }

        return null;
    }

    private static Class<?> toClass(final Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }

        if (type instanceof ParameterizedType) {
            final Type raw = ((ParameterizedType) type).getRawType();
            return (raw instanceof Class<?>) ? (Class<?>) raw : null;
        }

        if (type instanceof WildcardType) {
            return wildcardToClass((WildcardType) type);
        }

        // TypeVariable, GenericArrayType, etc: not resolvable to a concrete Class here.
        return null;
    }

    private static Class<?> wildcardToClass(final WildcardType wildcard) {
        final Type[] lower = wildcard.getLowerBounds();
        if (lower != null && lower.length == 1) {
            return toClass(lower[0]);
        }

        final Type[] upper = wildcard.getUpperBounds();
        if (upper != null && upper.length == 1) {
            return toClass(upper[0]);
        }

        return Object.class;
    }
}