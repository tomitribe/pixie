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

import java.lang.reflect.Modifier;

public class ComponentNotFoundForTypeException extends ComponentException {

    private final Class<?> requiredBy;

    public ComponentNotFoundForTypeException(final Class<?> type, final Class<?> requiredBy) {
        super(type);
        this.requiredBy = requiredBy;
    }

    public Class<?> getRequiredBy() {
        return requiredBy;
    }

    @Override
    public String getMessage() {
        final String kind;
        if (component.isInterface()) {
            kind = "an interface";
        } else if (Modifier.isAbstract(component.getModifiers())) {
            kind = "an abstract class";
        } else {
            kind = "a class";
        }

        final String typeName = component.getName();

        if (component.isInterface() || Modifier.isAbstract(component.getModifiers())) {
            return String.format(
                    "No component found of type %s.%n" +
                    "%n" +
                    "  %s is %s and cannot be instantiated directly.%n" +
                    "  An implementation must be provided to the System via one of:%n" +
                    "%n" +
                    "   - system.add(\"name\", myInstance)        Pre-built instance%n" +
                    "   - System.builder().definition(Impl.class) Builder API%n" +
                    "   - impl = new://com.example.MyImpl         Properties file",
                    typeName, component.getSimpleName(), kind);
        }

        return String.format(
                "No component found of type %s.%n" +
                "%n" +
                "  Provide an instance to the System via one of:%n" +
                "%n" +
                "   - system.add(\"name\", myInstance)        Pre-built instance%n" +
                "   - System.builder().definition(%s.class)  Builder API%n" +
                "   - impl = new://%s                        Properties file",
                typeName, component.getSimpleName(), typeName);
    }
}
