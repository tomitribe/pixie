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
import java.lang.reflect.Type;

public class NonClassEventTypeException extends InvalidEventReferenceException {
    private final Type type;

    public NonClassEventTypeException(final Parameter parameter, final Type type) {
        super(String.format("Reference '@Event Consumer<%s> %s' requires a generic type: non-class type '%s' found"
                , type.getTypeName(), parameter.getName(), type.getClass().getName()
        ), parameter);
        this.type = type;
    }

    public Type getType() {
        return type;
    }
}
