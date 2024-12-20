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

/**
 * Indicates a needed component with the specified name was not found.
 *
 * A component of the given type may exist in the system, however if it does
 * not have the expected name it will not be returned.
 *
 * In this context both getComponent() and getName() refer to the component not found,
 * not the component that needed it.
 */
public class NamedComponentNotFoundException extends ComponentException {

    private final String name;

    public <T> NamedComponentNotFoundException(final String name, final Class<T> type) {
        super(type);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String getMessage() {
        return String.format("Unable to find component with name '%s' and type %s", getName(), getComponent().getName());
    }
}
