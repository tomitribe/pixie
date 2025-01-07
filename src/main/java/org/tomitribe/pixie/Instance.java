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
package org.tomitribe.pixie;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple Builder API to create single java objects in a somewhat reflection-like fashion.
 *
 * Classes supplied are expected to be annotated with Pixie annotations such as Option and Component
 */
public class Instance {

    private Instance() {
    }

    public static <T> Builder<T> builder(final Class<T> type) {
        return new Builder<T>(type);
    }

    public static <T> Builder<T> builder(final Class<T> type, final String name) {
        return new Builder<T>(type, name);
    }

    public static class Builder<T> {

        private final AtomicInteger refs = new AtomicInteger(100);

        private final Properties properties = new Properties();
        private final HashMap<String, Object> objects = new HashMap<>();
        private final Class<T> type;
        private final String name;
        private boolean warnOnUnusedProperties = false;

        public Builder(final Class<T> type) {
            this(type, "instance");
        }

        public Builder(final Class<T> type, final String instance) {
            this.type = type;
            this.name = instance;
            properties.put(name, "new://" + type.getName());
        }

        public Builder<T> warnOnUnusedProperties() {
            warnOnUnusedProperties = true;
            return this;
        }

        /**
         * Set a component ref value to the specified object
         *
         * Internally, Pixie wants the value to be a name and to
         * subsequently lookup the specified object by name.  To
         * work around this, we give the specified object a unique
         * name, add the object to Pixie System with that name, and
         * then set a reference to that name.
         *
         * This could be optimized.
         */
        public Builder<T> comp(final String name, final Object value) {
            final String refName = name + refs.incrementAndGet();
            properties.put(this.name + "." + name, "@" + refName);
            objects.put(refName, value);
            return this;
        }

        public Builder<T> comp(final String name, final String refName) {
            properties.put(this.name + "." + name, "@" + refName);
            return this;
        }

        public Builder<T> add(final String name, final Object value) {
            objects.put(name, value);
            return this;
        }

        public Builder<T> option(final String name, final String value) {
            properties.put(this.name + "." + name, value);
            return this;
        }

        public Builder<T> option(final String name, final Integer value) {
            properties.put(this.name + "." + name, value);
            return this;
        }

        public Builder<T> option(final String name, final Boolean value) {
            properties.put(this.name + "." + name, value);
            return this;
        }

        public Builder<T> option(final String name, final Double value) {
            properties.put(this.name + "." + name, value);
            return this;
        }

        public Builder<T> option(final String name, final Float value) {
            properties.put(this.name + "." + name, value);
            return this;
        }

        public Builder<T> option(final String name, final Long value) {
            properties.put(this.name + "." + name, value);
            return this;
        }

        public Builder<T> option(final String name, final Short value) {
            properties.put(this.name + "." + name, value);
            return this;
        }

        public Builder<T> option(final String name, final Byte value) {
            properties.put(this.name + "." + name, value);
            return this;
        }

        public Builder<T> option(final String name, final Character value) {
            properties.put(this.name + "." + name, value);
            return this;
        }

        public <E extends Enum<E>> Builder<T> option(final String name, final E value) {
            properties.put(this.name + "." + name, value);
            return this;
        }

        public T build() {
            final System system = new System(warnOnUnusedProperties);
            for (final Map.Entry<String, Object> entry : objects.entrySet()) {
                system.add(entry.getValue(), entry.getKey());
            }
            system.load(properties);
            return system.get(type);
        }
    }
}
