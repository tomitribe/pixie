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
package org.tomitribe.pixie.observer;


import org.tomitribe.pixie.Observes;
import org.tomitribe.pixie.event.AfterEvent;
import org.tomitribe.pixie.event.BeforeEvent;
import org.tomitribe.pixie.event.ObserverAdded;
import org.tomitribe.pixie.event.ObserverFailed;
import org.tomitribe.pixie.event.ObserverNotFound;
import org.tomitribe.pixie.event.ObserverRemoved;
import org.tomitribe.util.Join;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ObserverManager {

    private static final ThreadLocal<Set<Invocation>> seen = new ThreadLocal<Set<Invocation>>() {
        @Override
        protected Set<Invocation> initialValue() {
            return new HashSet<>();
        }
    };

    // lazy init since it is used in SystemInstance
    private static final AtomicReference<Logger> LOGGER = new AtomicReference<>();
    private final Set<Observer> observers = new LinkedHashSet<>();
    private final Map<Class, Invocation> methods = new ConcurrentHashMap<>();
    private final List<ConsumerReference> references = new CopyOnWriteArrayList<>();


    public boolean addObserver(final Object observer) {
        if (observer == null) {
            throw new IllegalArgumentException("observer cannot be null");
        }

        try {
            final Observer wrapper = new Observer(observer);
            if (wrapper.hasObserverMethods() && observers.add(wrapper)) {
                methods.clear();
                references.stream().forEach(ConsumerReference::clear);
                fireEvent(new ObserverAdded(observer));
                return true;
            } else {
                return false;
            }
        } catch (final NotAnObserverException naoe) {
            return false;
        }
    }

    public boolean removeObserver(final Object observer) {
        if (observer == null) {
            throw new IllegalArgumentException("listener cannot be null");
        }
        try {
            if (observers.remove(new Observer(observer))) {
                methods.clear();
                references.stream().forEach(ConsumerReference::clear);
                fireEvent(new ObserverRemoved(observer));
                return true;
            } else {
                return false;
            }
        } catch (final NotAnObserverException naoe) {
            return false;
        }
    }

    public <E> E fireEvent(final E event) {
        if (event == null) {
            throw new IllegalArgumentException("event cannot be null");
        }

        try {
            return doFire(event);
        } finally {
            seen.remove();
        }
    }

    public <E> Consumer<E> consumersOf(final Class<E> eventClass) {
        if (eventClass == null) throw new IllegalArgumentException("eventClass cannot be null");
        final ConsumerReference e = new ConsumerReference(eventClass);
        references.add(e);
        return e;
    }

    private class ConsumerReference<E> implements Consumer<E> {

        private final Class<E> type;
        private final AtomicReference<Invocation> invocation = new AtomicReference<>();

        private ConsumerReference(final Class<E> type) {
            if (type == null) throw new IllegalArgumentException("type cannot be null");
            this.type = type;
        }

        @Override
        public void accept(final E e) {
            try {
                getInvocation().invoke(e);

            } finally {
                seen.remove();
            }
        }

        private Invocation getInvocation() {
            return this.invocation.updateAndGet(this::resolve);
        }

        private Invocation resolve(final Invocation invocation) {
            if (invocation != null) return invocation;
            return ObserverManager.this.getInvocation(type);
        }

        private void clear() {
            invocation.set(null);
        }

        @Override
        public String toString() {
            return "ConsumerReference{" +
                    "type=" + type.getName() +
                    "} " + getInvocation();
        }
    }

    private <E> E doFire(final E event) {
        final Class<?> type = event.getClass();

        final Invocation invocation = getInvocation(type);

        invocation.invoke(event);

        return event;
    }

    private Invocation getInvocation(final Class<?> type) {
        {
            final Invocation invocation = methods.get(type);
            if (invocation != null) {
                return invocation;
            }
        }

        final Invocation invocation = buildInvocation(type);
        methods.put(type, invocation);
        return invocation;
    }

    public void destroy() {
        for (final Observer o : new LinkedList<>(observers)) {
            removeObserver(o.observer);
        }
    }

    private enum Phase {
        BEFORE,
        INVOKE,
        AFTER
    }

    private Invocation buildInvocation(final Class<?> type) {
        final Invocation before = buildInvocation(Phase.BEFORE, type);
        final Invocation after = buildInvocation(Phase.AFTER, type);
        final Invocation invoke = buildInvocation(Phase.INVOKE, type);

        Invocation invocation = IGNORE;

        invocation = link(invocation, before);
        invocation = link(invocation, invoke);
        invocation = link(invocation, after);

        return invocation;
    }

    private Invocation buildInvocation(final Phase phase, final Class<?> type) {

        Invocation invocation = IGNORE;

        for (final Observer observer : observers) {

            final Invocation method = observer.get(phase, type);

            if (method != null && method != IGNORE) {

                invocation = link(invocation, method);
            }
        }

        return invocation;
    }

    private static Invocation link(Invocation first, final Invocation second) {
        if (first == IGNORE) {
            return second;
        } else {
            return first.andThen(second);
        }
    }

    public static class InvocationList implements Invocation {

        private final List<Invocation> invocations = new LinkedList<>();

        @Override
        public Invocation andThen(final Invocation after) {
            add(after);
            return this;
        }

        public boolean add(final Invocation invocation) {
            if (invocation == IGNORE) return true;

            if (invocation instanceof InvocationList) {

                final InvocationList list = (InvocationList) invocation;
                return invocations.addAll(list.invocations);

            } else {
                return invocations.add(invocation);
            }
        }

        @Override
        public void invoke(final Object event) {
            for (final Invocation invocation : invocations) {
                invocation.invoke(event);
            }
        }

        @Override
        public String toString() {
            return String.format("InvocationList{invocations=%s}%n%s%n", invocations.size(), Join.join("\n", invocations));
        }
    }


    /**
     * @version $Rev$ $Date$
     */
    public class Observer {

        private final Map<Class, Invocation> before;
        private final Map<Class, Invocation> methods;
        private final Map<Class, Invocation> after;
        private final Object observer;

        public Observer(final Object observer) {
            if (observer == null) {
                throw new IllegalArgumentException("observer cannot be null");
            }

            final Map<Class, Invocation> before = new HashMap<>();
            final Map<Class, Invocation> methods = new HashMap<>();
            final Map<Class, Invocation> after = new HashMap<>();

            this.observer = observer;
            for (final Method method : getMethods(observer)) {
                if (!isObserver(method)) {
                    continue;
                }

                if (method.getParameterTypes().length > 1) {
                    throw new IllegalArgumentException("@Observes method must have only 1 parameter: " + method);
                }

                if (Modifier.isAbstract(method.getModifiers())) {
                    throw new IllegalArgumentException("@Observes method must not be abstract: " + method);
                }

                if (Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalArgumentException("@Observes method must not be static: " + method);
                }

                if (!Modifier.isPublic(method.getModifiers())) {
                    throw new IllegalArgumentException("@Observes method must be public: " + method);
                }

                final Class<?> type = method.getParameterTypes()[0];

                if (AfterEvent.class.equals(type)) {

                    final Class parameterClass = getParameterClass(method);
                    after.put(parameterClass, new AfterInvocation(method, observer));

                } else if (BeforeEvent.class.equals(type)) {

                    final Class parameterClass = getParameterClass(method);
                    before.put(parameterClass, new BeforeInvocation(method, observer));

                } else {

                    validate(method, type);
                    methods.put(type, new MethodInvocation(method, observer));

                }
            }

            this.after = Collections.unmodifiableMap(after);
            this.before = Collections.unmodifiableMap(before);
            this.methods = Collections.unmodifiableMap(methods);
        }

        public boolean hasObserverMethods() {
            return after.size() + before.size() + methods.size() > 0;
        }

        private Set<Method> getMethods(final Object observer) {
            final Set<Method> methods = new HashSet<>();
            methods.addAll(Arrays.asList(observer.getClass().getMethods()));
            methods.addAll(Arrays.asList(observer.getClass().getDeclaredMethods()));
            return methods;
        }

        private Class getParameterClass(final Method method) {

            final Type[] genericParameterTypes = method.getGenericParameterTypes();

            final Type generic = genericParameterTypes[0];

            if (!(generic instanceof ParameterizedType)) {
                final Class<?> event = method.getParameterTypes()[0];
                throw new IllegalArgumentException("@Observes " + event.getSimpleName() + " missing generic type: " + method);
            }

            final ParameterizedType parameterized = ParameterizedType.class.cast(generic);

            final Type type = parameterized.getActualTypeArguments()[0];

            final Class clazz;

            if (type instanceof Class) {

                clazz = Class.class.cast(type);

            } else if (type instanceof WildcardType) {

                clazz = Object.class;

            } else {

                final Class<?> event = method.getParameterTypes()[0];
                throw new IllegalArgumentException("@Observes " + event.getSimpleName() +
                        " unsupported generic type: " + type.getClass().getSimpleName() +
                        "  " + method);
            }

            validate(method, clazz);

            return clazz;
        }

        private void validate(final Method method, final Class<?> type) {
            if (type.isAnnotation()) {
                throw new IllegalArgumentException("@Observes method parameter must be a concrete class (not an annotation): " + method.toString());
            }

            if (type.isInterface()) {
                throw new IllegalArgumentException("@Observes method parameter must be a concrete class (not an interface): " + method.toString());
            }

            if (type.isArray()) {
                throw new IllegalArgumentException("@Observes method parameter must be a concrete class (not an array): " + method.toString());
            }

            if (type.isPrimitive()) {
                throw new IllegalArgumentException("@Observes method parameter must be a concrete class (not a primitive): " + method.toString());
            }
        }

        private Map<Class, Invocation> map(final Phase event) {
            switch (event) {
                case AFTER:
                    return after;
                case BEFORE:
                    return before;
                case INVOKE:
                    return methods;
                default:
                    throw new IllegalStateException("Unknown Event style " + event);
            }
        }

        public Invocation get(final Phase event, final Class eventType) {
            return get(map(event), eventType);
        }

        private Invocation get(final Map<Class, Invocation> map, final Class eventType) {
            if (eventType == null) {
                return IGNORE;
            }

            final Invocation method = map.get(eventType);

            if (method != null) {
                return method;
            }

            return get(map, eventType.getSuperclass());
        }

        private boolean isObserver(final Method method) {
            for (final Annotation[] annotations : method.getParameterAnnotations()) {
                for (final Annotation annotation : annotations) {
                    if (annotation.annotationType().equals(Observes.class)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final Observer observer1 = (Observer) o;

            return observer.equals(observer1.observer);
        }

        @Override
        public int hashCode() {
            return observer.hashCode();
        }
    }

    public interface Invocation {

        void invoke(Object event);

        default Invocation andThen(Invocation after) {
            if (after == IGNORE) return this;

            final InvocationList list = new InvocationList();
            list.add(this);
            list.add(after);
            return list;
        }

    }


    private static final Invocation IGNORE = new Invocation() {
        @Override
        public void invoke(final Object event) {
            final String name = event.getClass().getName();
            if (!name.startsWith("org.tomitribe.pixie.event.") && !(event instanceof ObserverNotFound)) {
                ObserverManager.logger().info("No observers for event " + name); // not really an error, just informational
                // doFire(new ObserverNotFound(event));
            }
        }

        @Override
        public String toString() {
            return "IGNORED";
        }
    };


    public class MethodInvocation implements Invocation {
        private final Method method;
        private final Object observer;

        public MethodInvocation(final Method method, final Object observer) {
            this.method = method;
            this.observer = observer;
        }

        @Override
        public void invoke(final Object event) {
            try {
                method.invoke(observer, event);

            } catch (final InvocationTargetException e) {
                if (!seen.get().add(this)) {
                    return;
                }

                final Throwable t = e.getTargetException() == null ? e : e.getTargetException();

                if (!(resolveEvent(event) instanceof ObserverFailed)) {
                    doFire(new ObserverFailed(observer, method, event, t));
                }

                if (t instanceof InvocationTargetException && t.getCause() != null) {
                    ObserverManager.logger().log(Level.SEVERE, "error invoking " + observer, t.getCause());
                } else {
                    ObserverManager.logger().log(Level.SEVERE, "error invoking " + observer, t);
                }
            } catch (final IllegalAccessException e) {
                ObserverManager.logger().log(Level.SEVERE, method + " can't be invoked, check it is public");
                throw new NotInvokableObserverException(method + " can't be invoked, check it is public");
            }
        }

        @Override
        public String toString() {
            return method.toString();
        }
    }

    // done lazily since this class is used in SystemInstance
    private static Logger logger() {
        Logger value = LOGGER.get();
        if (value == null) {
            value = Logger.getLogger(ObserverManager.class.getName());
            LOGGER.set(value);
        }
        return value;
    }

    private Object resolveEvent(final Object event) {
        if (event instanceof BeforeEvent) {
            return ((BeforeEvent) event).getEvent();
        } else if (event instanceof AfterEvent) {
            return ((AfterEvent) event).getEvent();
        } else {
            return event;
        }
    }

    private final class AfterInvocation extends MethodInvocation {

        private AfterInvocation(final Method method, final Object observer) {
            super(method, observer);
        }

        @Override
        public void invoke(final Object event) {
            super.invoke(new AfterEvent() {
                @Override
                public Object getEvent() {
                    return event;
                }

                public String toString() {
                    return "AfterEvent{} " + event;
                }
            });
        }
    }

    private final class BeforeInvocation extends MethodInvocation {

        private BeforeInvocation(final Method method, final Object observer) {
            super(method, observer);
        }

        @Override
        public void invoke(final Object event) {
            super.invoke(new BeforeEvent() {
                @Override
                public Object getEvent() {
                    return event;
                }

                public String toString() {
                    return "BeforeEvent{} " + event;
                }
            });
        }
    }

    public static class NotAnObserverException extends IllegalArgumentException {
        public NotAnObserverException(final String s) {
            super(s);
        }
    }

    public static class NotInvokableObserverException extends RuntimeException {
        public NotInvokableObserverException(final String s) {
            super(s);
        }
    }
}
