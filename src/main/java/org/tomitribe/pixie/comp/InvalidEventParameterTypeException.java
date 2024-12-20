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

public class InvalidEventParameterTypeException extends InvalidEventReferenceException {
    public InvalidEventParameterTypeException(final Parameter parameter) {
        super(message(parameter), parameter);
    }

    private static String message(final Parameter parameter) {
        return String.format("Reference '@Event %s %s' is invalid.  Use '@Event java.util.function.Consumer<Foo> %s'",
                parameter.getType().getSimpleName(),
                parameter.getName(),
                parameter.getName()
        );
    }
}
