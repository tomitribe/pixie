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

import java.util.Map;

public final class Id<T> {
    private final String name;
    private final Class<T> type;

    public Id(final String name, final Class<T> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Class<T> getType() {
        return type;
    }

    public static <T> Id<T> from(final Class<T> type) {
        return new Id<>(type.getSimpleName(), type);
    }

    private static <T> Id<T> from(final Map.Entry<String, String> entry) {
        final String name = entry.getKey();
        final String className = entry.getValue().replace("new://", "");

        final Class type;
        try {
            // Load the components implementation class
            final ClassLoader loader = Thread.currentThread().getContextClassLoader();
            type = loader.loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot load component class: " + className, e);
        }

        return new Id<>(name, type);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
