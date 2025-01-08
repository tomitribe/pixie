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
import java.util.Objects;
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

    /**
     * Build an instance of the specified class
     */
    public static <T> Builder<T> builder(final Class<T> type) {
        return new Builder<T>(type);
    }

    /**
     * Build an instance of the specified class with the specified name.
     * The name will only be relevant if the class uses the @Name
     */
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
        private System.Declaration<T> declaration;

        public Builder(final Class<T> type) {
            this(type, "instance");
        }

        public Builder(final Class<T> type, final String name) {
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(name, "name must not be null");

            this.type = type;
            this.name = name;
            properties.put(this.name, "new://" + type.getName());
            declaration = new System().new Declaration<>(name, type);
        }

        /**
         * Use of comp(*) and option(*) create the expectation the
         * class definition has explicitly declared a need for these
         * things via the @Component and @Option annotations.
         *
         * When no such declaration exists an exception will be thrown
         * on build() unless warnOnUnusedProperties() has been called.
         */
        public Builder<T> warnOnUnusedProperties() {
            warnOnUnusedProperties = true;
            return this;
        }

        /**
         * Sets the value of an explicitly declared @Component reference
         * declared in the class of the instance we are creating.
         *
         * If the class does not declare a @Component reference with the
         * name specified an exception will be thrown unless `warnOnUnusedProperties`
         *
         * Use add(*) to offer objects that may be useful to the created instance
         * but are not known as explicit requirements.
         */
        public Builder<T> comp(final String name, final Object value) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(value, "value must not be null");

            /*
             * Internally, Pixie wants the value to be a name and to
             * subsequently lookup the specified object by name.  To
             * work around this, we give the specified object a unique
             * name, add the object to Pixie System with that name, and
             * then set a reference to that name.
             *
             * This could be optimized.
             */
            final String refName = "unnamed$" + name + refs.incrementAndGet();
            properties.put(this.name + "." + name, "@" + refName);
            objects.put(refName, value);
            return this;
        }

        public Builder<T> comp(final String name, final String refName) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(refName, "refName must not be null");

            properties.put(this.name + "." + name, "@" + refName);
            return this;
        }

        /**
         * Adds an object to be possibly consumed by the created instance.
         *
         * Calling this method does not create the expectation the created
         * instance needs an object of this type and will not result in
         * any form of exception or warning.
         *
         * Use comp(*) to explicitly create an expectation the supplied
         * object value must be consumed by the created instance.
         *
         * As no name is specified for this object it may only be referenced by type
         * and adding more objects of the same type will create ambiguity in resolution.
         *
         * @param value The object instance we anticipate may be useful to the created instance.
         */
        public Builder<T> add(final Object value) {
            Objects.requireNonNull(value, "value must not be null");

            final String name = "unnamed$" + value.getClass().getSimpleName() + refs.incrementAndGet();
            objects.put(name, value);

            return this;
        }

        /**
         * Adds an object to be possibly consumed by the created instance.
         *
         * Calling this method does not create the expectation the created
         * instance needs an object of this type and will not result in
         * any form of exception or warning.
         *
         * Use comp(*) to explicitly create an expectation the supplied
         * object value must be consumed by the created instance.
         *
         * @param name Sets an explicit name which enables several instance of the same type to exist and be referenced
         * @param value The object instance we anticipate may be useful to the created instance.
         */
        public Builder<T> add(final String name, final Object value) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(value, "value must not be null");

            if (declaration.getOption(name) != null) {

                properties.put(this.name + "." + name, value);

            } else if (declaration.getReference(name) != null) {

                comp(name, value);

            }

            return this;
        }

        /**
         * Sets the value of an explicitly declared @Option reference
         * declared in the class of the instance we are creating.
         *
         * If the class does not declare a @Option reference with the
         * name specified an exception will be thrown unless `warnOnUnusedProperties`
         *
         * Use add(*) to offer objects that may be useful to the created instance
         * but are not known as explicit requirements.
         */
        public Builder<T> option(final String name, final String value) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(value, "value must not be null");

            properties.put(this.name + "." + name, value);
            return this;
        }

        /**
         * Sets the value of an explicitly declared @Option reference
         * declared in the class of the instance we are creating.
         *
         * If the class does not declare a @Option reference with the
         * name specified an exception will be thrown unless `warnOnUnusedProperties`
         *
         * Use add(*) to offer objects that may be useful to the created instance
         * but are not known as explicit requirements.
         */
        public Builder<T> option(final String name, final Integer value) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(value, "value must not be null");

            properties.put(this.name + "." + name, value);
            return this;
        }

        /**
         * Sets the value of an explicitly declared @Option reference
         * declared in the class of the instance we are creating.
         *
         * If the class does not declare a @Option reference with the
         * name specified an exception will be thrown unless `warnOnUnusedProperties`
         *
         * Use add(*) to offer objects that may be useful to the created instance
         * but are not known as explicit requirements.
         */
        public Builder<T> option(final String name, final Boolean value) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(value, "value must not be null");

            properties.put(this.name + "." + name, value);
            return this;
        }

        /**
         * Sets the value of an explicitly declared @Option reference
         * declared in the class of the instance we are creating.
         *
         * If the class does not declare a @Option reference with the
         * name specified an exception will be thrown unless `warnOnUnusedProperties`
         *
         * Use add(*) to offer objects that may be useful to the created instance
         * but are not known as explicit requirements.
         */
        public Builder<T> option(final String name, final Double value) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(value, "value must not be null");

            properties.put(this.name + "." + name, value);
            return this;
        }

        /**
         * Sets the value of an explicitly declared @Option reference
         * declared in the class of the instance we are creating.
         *
         * If the class does not declare a @Option reference with the
         * name specified an exception will be thrown unless `warnOnUnusedProperties`
         *
         * Use add(*) to offer objects that may be useful to the created instance
         * but are not known as explicit requirements.
         */
        public Builder<T> option(final String name, final Float value) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(value, "value must not be null");

            properties.put(this.name + "." + name, value);
            return this;
        }

        /**
         * Sets the value of an explicitly declared @Option reference
         * declared in the class of the instance we are creating.
         *
         * If the class does not declare a @Option reference with the
         * name specified an exception will be thrown unless `warnOnUnusedProperties`
         *
         * Use add(*) to offer objects that may be useful to the created instance
         * but are not known as explicit requirements.
         */
        public Builder<T> option(final String name, final Long value) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(value, "value must not be null");

            properties.put(this.name + "." + name, value);
            return this;
        }

        /**
         * Sets the value of an explicitly declared @Option reference
         * declared in the class of the instance we are creating.
         *
         * If the class does not declare a @Option reference with the
         * name specified an exception will be thrown unless `warnOnUnusedProperties`
         *
         * Use add(*) to offer objects that may be useful to the created instance
         * but are not known as explicit requirements.
         */
        public Builder<T> option(final String name, final Short value) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(value, "value must not be null");

            properties.put(this.name + "." + name, value);
            return this;
        }

        /**
         * Sets the value of an explicitly declared @Option reference
         * declared in the class of the instance we are creating.
         *
         * If the class does not declare a @Option reference with the
         * name specified an exception will be thrown unless `warnOnUnusedProperties`
         *
         * Use add(*) to offer objects that may be useful to the created instance
         * but are not known as explicit requirements.
         */
        public Builder<T> option(final String name, final Byte value) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(value, "value must not be null");

            properties.put(this.name + "." + name, value);
            return this;
        }

        /**
         * Sets the value of an explicitly declared @Option reference
         * declared in the class of the instance we are creating.
         *
         * If the class does not declare a @Option reference with the
         * name specified an exception will be thrown unless `warnOnUnusedProperties`
         *
         * Use add(*) to offer objects that may be useful to the created instance
         * but are not known as explicit requirements.
         */
        public Builder<T> option(final String name, final Character value) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(value, "value must not be null");

            properties.put(this.name + "." + name, value);
            return this;
        }

        /**
         * Sets the value of an explicitly declared @Option reference
         * declared in the class of the instance we are creating.
         *
         * If the class does not declare a @Option reference with the
         * name specified an exception will be thrown unless `warnOnUnusedProperties`
         *
         * Use add(*) to offer objects that may be useful to the created instance
         * but are not known as explicit requirements.
         */
        public <E extends Enum<E>> Builder<T> option(final String name, final E value) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(value, "value must not be null");

            properties.put(this.name + "." + name, value.name());
            return this;
        }

        /**
         *
         * Creates and returns a new instance of the class specified in the build() method
         * using the values supplied to the builder
         * @return a new instance of the class specified in the build() method
         * @throws org.tomitribe.pixie.comp.ComponentException if the instance cannot be constructed
         */
        public T build() {
            final System system = new System(warnOnUnusedProperties);

            /*
             * Add the objects for component references
             */
            for (final Map.Entry<String, Object> entry : objects.entrySet()) {
                system.add(entry.getValue(), entry.getKey());
            }

            /*
             * Swap the context classloader to whatever ClassLoader
             * loaded the target class
             */
            {
                final Thread thread = Thread.currentThread();
                final ClassLoader old = thread.getContextClassLoader();
                thread.setContextClassLoader(type.getClassLoader());
                try {
                    system.load(properties);
                } finally {
                    thread.setContextClassLoader(old);
                }
            }

            return system.get(type);
        }
    }
}
