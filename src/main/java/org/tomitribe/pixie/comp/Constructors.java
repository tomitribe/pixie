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

import org.tomitribe.pixie.Component;
import org.tomitribe.pixie.Event;
import org.tomitribe.pixie.Name;
import org.tomitribe.pixie.Param;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Constructors {
    private Constructors() {
    }

    public static boolean isFullyAnnotated(final Parameter parameter) {
        if (parameter.isAnnotationPresent(Component.class)) return true;
        if (parameter.isAnnotationPresent(Param.class)) return true;
        if (parameter.isAnnotationPresent(Name.class)) return true;
        if (parameter.isAnnotationPresent(Event.class)) return true;
        return false;
    }

    public static boolean isFullyAnnotated(final Constructor<?> constructor) {
        for (final Parameter parameter : constructor.getParameters()) {
            if (!isFullyAnnotated(parameter)) return false;
        }
        return true;
    }

    public static boolean isPublic(final Constructor<?> constructor) {
        return Modifier.isPublic(constructor.getModifiers());
    }

    public static boolean isInvalid(final Constructor<?> constructor) {
        int validParameters = 0;
        for (final Parameter parameter : constructor.getParameters()) {
            if (isFullyAnnotated(parameter)) validParameters++;
        }

        return validParameters > 0 && validParameters != constructor.getParameters().length;
    }

    public static <T> Constructor<T> findConstructor(Class<T> clazz) {
        final List<Constructor<?>> invalid = Stream.of(clazz.getConstructors())
                .filter(Constructors::isInvalid)
                .collect(Collectors.toList());

        if (invalid.size() > 0) {
            throw new InvalidConstructorException(clazz, invalid.get(0));
        }

        final List<Constructor<?>> valid = Stream.of(clazz.getConstructors())
                .filter(Constructors::isPublic)
                .filter(Constructors::isFullyAnnotated)
                .collect(Collectors.toList());

        if (valid.size() > 1) {
            eliminateNoArgConstructor(valid);
            if (valid.size() > 1) throw new AmbiguousConstructorException(clazz, valid);
        }

        if (valid.size() == 0) {
            throw new NoValidConstructorException(clazz);
        }

        return (Constructor<T>) valid.get(0);
    }

    private static void eliminateNoArgConstructor(final List<Constructor<?>> valid) {
        final Iterator<Constructor<?>> iterator = valid.iterator();
        while (iterator.hasNext()) {
            final Constructor<?> constructor = iterator.next();
            if (constructor.getParameterCount() == 0) iterator.remove();
        }
    }
}
