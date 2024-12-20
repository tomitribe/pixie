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

import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.function.Consumer;

public class EventReferences {

    private EventReferences() {
        // no-op
    }

    public static Class eventType(final Parameter parameter) {
        if (!Consumer.class.equals(parameter.getType())) {
            throw new InvalidEventParameterTypeException(parameter);
        }

        final Type generic = parameter.getParameterizedType();

        if (!(generic instanceof ParameterizedType)) {
            throw new MissingEventTypeException(parameter);
        }

        final ParameterizedType parameterized = ParameterizedType.class.cast(generic);

        final Type type = parameterized.getActualTypeArguments()[0];

        if (type instanceof WildcardType) {
            throw new WildcardEventTypeException(parameter);
        }

        if (!(type instanceof Class)) {
            throw new NonClassEventTypeException(parameter, type);
        }

        return Class.class.cast(type);
    }
}
