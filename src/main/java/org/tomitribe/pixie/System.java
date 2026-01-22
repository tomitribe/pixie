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

import org.tomitribe.pixie.comp.ComponentException;
import org.tomitribe.pixie.comp.ComponentReferenceSyntaxException;
import org.tomitribe.pixie.comp.ConstructionFailedException;
import org.tomitribe.pixie.comp.Constructors;
import org.tomitribe.pixie.comp.EventReferences;
import org.tomitribe.pixie.comp.InjectionPoint;
import org.tomitribe.pixie.comp.InvalidConstructorException;
import org.tomitribe.pixie.comp.InvalidFactoryMethodException;
import org.tomitribe.pixie.comp.InvalidNullableWithDefaultException;
import org.tomitribe.pixie.comp.InvalidParamValueException;
import org.tomitribe.pixie.comp.MissingComponentClassException;
import org.tomitribe.pixie.comp.MissingComponentDeclarationException;
import org.tomitribe.pixie.comp.MissingRequiredParamException;
import org.tomitribe.pixie.comp.MultipleComponentIssuesException;
import org.tomitribe.pixie.comp.NamedComponentNotFoundException;
import org.tomitribe.pixie.comp.References;
import org.tomitribe.pixie.comp.UnknownPropertyException;
import org.tomitribe.pixie.event.ComponentAdded;
import org.tomitribe.pixie.event.PixieClose;
import org.tomitribe.pixie.event.PixieLoad;
import org.tomitribe.pixie.observer.ObserverManager;
import org.tomitribe.util.Join;
import org.tomitribe.util.editor.Converter;
import org.tomitribe.util.reflect.Generics;

import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is designed to never be seen.  We want to discourage
 * the need to reference a class which does so much.  Ideally you
 * are getting your needs met through dependency injection.
 */
public class System implements Closeable {

    protected final boolean warnOnUnusedProperties;

    protected final Map<String, String> parameters = new ConcurrentHashMap<>();

    protected final Map<String, String> usedParameters = new ConcurrentHashMap<>();

    protected final List<Instance> objects = new CopyOnWriteArrayList<>();

    protected final ObserverManager observerManager = new ObserverManager();

    protected static final Logger LOGGER = Logger.getLogger(System.class.getName());

    public System() {
        this(new Properties(), false);
    }

    public System(final boolean warnOnUnusedProperties) {
        this(new Properties(), warnOnUnusedProperties);
    }

    public System(final Properties properties) {
        this(properties, false);
    }

    public System(final Properties properties, final boolean warnOnUnusedProperties) {
        this.warnOnUnusedProperties = warnOnUnusedProperties;
        // Add System as a component that can be injected
        add("system", this);
        load(properties);
    }

    /**
     * It is ok to call this method from tests, but do not call it from production code.
     * <p>
     * Yes, it will work.  Yes, it is neat.  No, we don't want things created the user
     * cannot see.  Current efforts are to put all components in a single pixie.properties
     * file the user can update.
     */
    public void load(final Properties properties) {
        // Convert the properties to Map<String,String>
        parameters.putAll(toMap(properties));

        // Get the things that were explicitly declared in the configuration
        final List<Declaration> declarations = toMap(properties).entrySet().stream()
                .filter(entry -> entry.getValue().startsWith("new://"))
                .peek(entry -> usedParameters.put(entry.getKey(), entry.getValue()))
                .map(this::createDeclaration)
                .collect(Collectors.toList());

        build(declarations);

        // Did the user specify any properties that were not used?
        if (warnOnUnusedProperties) {
            parameters.entrySet().stream()
                    .sorted(Comparator.comparing(Map.Entry::getKey))
                    .filter(entry -> !usedParameters.containsKey(entry.getKey()))
                    .filter(entry -> !entry.getKey().startsWith("@"))
                    .forEach(entry -> {
                        LOGGER.warning("Warning: Unused property '" + entry.getKey() + "'");
                    });
        }

        // fire an event at the end so components can do something after
        observerManager.fireEvent(new PixieLoad(properties));
    }

    private void build(final List<Declaration> declarations) {
        if (declarations.size() == 0) return;

        for (final Declaration declaration : new ArrayList<>(declarations)) {
            resolveReferences(declaration, declarations);
        }

        final List<Declaration> sorted = sortDependencies(declarations);

        for (final Declaration declaration : sorted) {
            addInstance(declaration.buildInstance());
        }
    }

    private void resolveReferences(final Declaration declaration, final List<Declaration> declarations) {
        final List<Declaration.Reference> unresolved = declaration.getUnresolvedReferences();
        for (final Declaration.Reference reference : unresolved) {

            try {
                if (reference.getTarget() instanceof String) {

                    resolveByTypeAndName(declarations, reference);

                } else if (reference.getTarget() == null) {

                    resolveByType(declarations, reference);

                } else {
                    // should be an unreachable statement
                    throw new IllegalStateException(String.format("Reference %s should be null or String at this stage of the code", reference.getName()));
                }
            } catch (Exception e) {
                throw new ConstructionFailedException(reference.getDeclaration().getClazz(), e);
            }
        }
    }

    private void resolveByType(final List<Declaration> declarations, final Declaration.Reference reference) {
        final List<Instance> usableInstances = objects.stream()
                .filter(instance -> instance.isAssignableTo(reference.getType()))
                .collect(Collectors.toList());

        if (reference.getCollectionType() != null) {
            final Collection<Object> collection = newInstance(reference.getCollectionType());
            collection.addAll(usableInstances);

            final List<Declaration> usableDeclarations = declarations.stream()
                    .filter(declaration -> declaration.isAssignableTo(reference.getType()))
                    .collect(Collectors.toList());
            collection.addAll(usableDeclarations);

            reference.set(collection);
            return;
        }

        if (usableInstances.size() > 0) {
            reference.set(usableInstances.get(0));
            return;
        }

        final List<Declaration> usableDeclarations = declarations.stream()
                .filter(declaration -> declaration.isAssignableTo(reference.getType()))
                .collect(Collectors.toList());

        if (usableDeclarations.size() > 0) {
            // Our reference points to an to-be-built Declaration
            reference.set(usableDeclarations.get(0));
            return;
        }

        // when a reference is nullable, we don't want to lazily create an instance
        if (reference.isNullable()) {
            return;
        }

        // Attempt to auto-create a declaration based in type
        final Declaration declaration = createDeclaration(reference.getType(), null);
        reference.set(declaration);
        declarations.add(declaration);
        resolveReferences(declaration, declarations);
    }

    private void resolveByTypeAndName(final List<Declaration> declarations, final Declaration.Reference reference) {

        if (reference.getCollectionType() != null) {
            final Collection<Object> collection = newInstance(reference.getCollectionType());
            final String target = (String) reference.getTarget();

            for (final String name : target.split("\\s*@")) {
                final List<Instance> usableInstances = objects.stream()
                        .filter(instance -> instance.isAssignableTo(reference.getType()))
                        .filter(instance -> name.equalsIgnoreCase(instance.getName()))
                        .collect(Collectors.toList());

                if (usableInstances.size() > 0) {
                    collection.add(usableInstances.get(0));
                    continue;
                }

                final List<Declaration> usableDeclarations = declarations.stream()
                        .filter(declaration -> declaration.isAssignableTo(reference.getType()))
                        .filter(declaration -> name.equalsIgnoreCase(declaration.getName()))
                        .collect(Collectors.toList());

                if (usableDeclarations.size() > 0) {
                    // Our reference points to an to-be-built Declaration
                    collection.add(usableDeclarations.get(0));
                    continue;
                }

                throw new NamedComponentNotFoundException(name, reference.getType());
            }

            reference.set(collection);
            return;
        }

        final String name = (String) reference.getTarget();

        final List<Instance> usableInstances = objects.stream()
                .filter(instance -> instance.isAssignableTo(reference.getType()))
                .filter(instance -> name.equalsIgnoreCase(instance.getName()))
                .collect(Collectors.toList());

        if (usableInstances.size() > 0) {
            reference.set(usableInstances.get(0));
            return;
        }

        final List<Declaration> usableDeclarations = declarations.stream()
                .filter(declaration -> declaration.isAssignableTo(reference.getType()))
                .filter(declaration -> name.equalsIgnoreCase(declaration.getName()))
                .collect(Collectors.toList());

        if (usableDeclarations.size() > 0) {
            // Our reference points to an to-be-built Declaration
            reference.set(usableDeclarations.get(0));
            return;
        }

        throw new NamedComponentNotFoundException(name, reference.getType());
    }

    private List<Declaration> sortDependencies(final List<Declaration> declarations) {
        // We need to start building the components that have no references
        // first and work our way to the components that refer to them.
        // To achieve this we sort the list
        return References.sort(declarations, new References.Visitor<Declaration>() {
            @Override
            public String getName(final Declaration declaration) {
                return declaration.getReferenceId();
            }

            @Override
            public Set<String> getReferences(final Declaration declaration) {
                // We intentionally only include unbuilt references
                return new HashSet<>(declaration.getDeclarationReferences());
            }
        });
    }

    private static Map<String, String> toMap(final Properties properties) {
        final Map<String, String> map = new HashMap<>();

        for (final Map.Entry<Object, Object> entry : properties.entrySet()) {
            map.put(entry.getKey() + "", entry.getValue() + "");
        }
        return map;
    }

    @Override
    public void close() {
        // todo: should we do some sort of cleanup?
        observerManager.fireEvent(new PixieClose());
    }

    public static class Instance<T> {
        private final String name;
        private final T object;

        public Instance(final String name, final T object) {
            if (name == null) throw new IllegalStateException("Name cannot be null");
            this.name = name.toLowerCase();
            this.object = object;
        }

        public String getName() {
            return name;
        }

        public T getObject() {
            return object;
        }

        public boolean isAssignableTo(final Class<?> clazz) {
            return clazz.isAssignableFrom(object.getClass());
        }

        public boolean isAnnotationPresent(final Class<? extends Annotation> type) {
            return object.getClass().isAnnotationPresent(type);
        }

        @Override
        public String toString() {
            return "Instance{" +
                    "name='" + name + '\'' +
                    ", object=" + object +
                    '}';
        }

    }

    public <T> void add(final String name, final T value) {
        addInstance(new Instance<>(name, value));
    }

    private <T> void addInstance(final Instance<T> e) {
        this.objects.add(e);

        final T object = e.getObject();
        final Class<T> type = (Class<T>) object.getClass();
        this.fireEvent(new ComponentAdded<>(type, object));

        addObserver(object);
    }

    public <T> T get(final Class<T> type) {
        return get(type, true);
    }

    public <T> T get(final Class<T> type, final boolean create) {
        return get(type, null, create);
    }

    public <T> T get(final Class<T> type, final String name) {
        return get(type, name, true);
    }

    public <T> T get(final Class<T> type, final boolean create, final String name) {
        return get(type, name, create);
    }

    /**
     * For the moment, let's not expose the complexity of looking
     * things up by name.  We've lived without it for quite a while,
     * so let's keep it constrained to the config file for now.
     *
     * @param type The type of component needed
     * @param name Optionally narrow the search by name
     * @param create Should Trixy lazily create the instance if there is none
     * @return An instance that is or implements the specified type
     * @throws NamedComponentNotFoundException if name is not null and there is no matching component
     */
    private <T> T get(final Class<T> type, final String name, final boolean create) {
        // First filter by type
        final List<Instance> assignable = objects.stream()
                .filter(instance -> instance.isAssignableTo(type))
                .collect(Collectors.toList());

        final Predicate<Instance> implies;
        final Predicate<Instance> explicit;
        final Supplier<T> fallback;

        if (name == null) {
            // They want any instance of this type.
            // Do our best or create one if create flag is true
            explicit = instance -> instance.getName() == null;
            implies = instance -> true;
            fallback = () -> {
                if (create) {
                    return create(type, null);

                } else {
                    return null;
                }
            };

        } else {
            // They want specifically named instance of this type.
            // Give an exact match or fail
            explicit = instance -> name.toLowerCase().equals(instance.getName());
            implies = instance -> false;
            fallback = () -> {
                throw new NamedComponentNotFoundException(name, type);
            };
        }


        { // Filter by name
            final Optional<Instance> exact = assignable.stream()
                    .filter(explicit)
                    .findFirst();

            if (exact.isPresent()) return (T) exact.get().getObject();
        }

        { // Can we safely imply a match?
            final Optional<Instance> implied = assignable.stream()
                    .filter(implies)
                    .findFirst();

            if (implied.isPresent()) return (T) implied.get().getObject();
        }

        return fallback.get();
    }

    public <T> List<T> getAll(final Class<T> type) {
        return (List<T>) objects.stream()
                .filter(instance -> instance.isAssignableTo(type))
                .map(Instance::getObject)
                .collect(Collectors.toList());
    }

    public List<Object> getAnnotated(final Class<? extends Annotation> type) {
        return objects.stream()
                .filter(instance -> instance.isAnnotationPresent(type))
                .map(Instance::getObject)
                .collect(Collectors.toList());
    }


    public <E> E fireEvent(final E event) {
        return observerManager.fireEvent(event);
    }

    public <E> Consumer<E> consumersOf(final Class<E> eventClass) {
        return observerManager.consumersOf(eventClass);
    }

    public boolean addObserver(final Object observer) {
        return observerManager.addObserver(observer);
    }

    public boolean removeObserver(final Object observer) {
        return observerManager.removeObserver(observer);
    }

    private Declaration createDeclaration(final Map.Entry<String, String> entry) {
        final Class<?> clazz = loadComponentClass(entry);
        final String key = entry.getKey();

        return createDeclaration(clazz, key);
    }

    private Declaration createDeclaration(final Class<?> clazz, final String key) {

        final List<Throwable> issues = new ArrayList<>();

        try {
            final Declaration declaration = new Declaration(key, clazz);

            issues.addAll(checkForNullableWithDefault(declaration));

            applyImplicitOverrides(declaration);

            issues.addAll(applyExplicitOverrides(declaration));

            issues.addAll(checkForMissingParameters(declaration));


            if (issues.size() == 0) return declaration;

        } catch (ConstructionFailedException t) {
            throw t;
        } catch (Throwable t) {
            issues.add(t);
        }

        if (issues.size() == 1) {
            throw new ConstructionFailedException(clazz, issues.get(0));
        }

        throw new ConstructionFailedException(clazz,
                new MultipleComponentIssuesException(clazz, issues)
        );
    }

    private Collection<? extends ComponentException> checkForMissingParameters(final Declaration declaration) {
        final Map<String, Declaration.ParamValue> parameters = declaration.getParams();
        return parameters.values().stream()
                .filter(paramValue -> paramValue.getValue() == null)
                .filter(paramValue -> !paramValue.isNullable())
                .map(paramValue -> new MissingRequiredParamException(declaration.getClazz(), paramValue.getName()))
                .collect(Collectors.toList())
                ;
    }

    // this is invalid to have both @Nullable and @Default
    private Collection<? extends ComponentException> checkForNullableWithDefault(final Declaration declaration) {
        final Map<String, Declaration.ParamValue> params = declaration.getParams();
        return params.values().stream()
                .filter(paramValue -> paramValue.getValue() != null)
                .filter(Declaration.ParamValue::isNullable)
                .map(paramValue -> new InvalidNullableWithDefaultException(declaration.getClazz(), paramValue.getName(), paramValue.getValue()))
                .collect(Collectors.toList())
                ;
    }

    private List<ComponentException> applyExplicitOverrides(final Declaration declaration) {
        if (declaration.getName() == null) return java.util.Collections.EMPTY_LIST;

        final String prefix = declaration.getName().toLowerCase() + ".";

        // Select just the entries that pertain to this declaration
        final Map<String, String> overrides = selectOverrides(prefix);

        final List<ComponentException> issues = new ArrayList<>();

        // Has the user specified any properties this declaration doesn't actually support?
        issues.addAll(checkForUnknownProperties(declaration, overrides));

        // Are they referring to things correctly with or without "@" annotation?
        issues.addAll(checkForMisusedProperties(declaration, overrides));

        // Error handling is above, so this logic is simple
        override(declaration, overrides);

        return issues;
    }

    private void override(final Declaration declaration, final Map<String, String> overrides) {
        for (final Map.Entry<String, String> property : overrides.entrySet()) {
            if (declaration.getParam(property.getKey()) != null) {

                usedParameters.put(property.getKey(), property.getValue());
                declaration.getParam(property.getKey()).setValue(property.getValue());

            } else if (declaration.getReference(property.getKey()) != null && property.getValue().startsWith("@")) {

                usedParameters.put(property.getKey(), property.getValue());
                declaration.getReference(property.getKey()).set(property.getValue().substring(1));
            }
        }
    }

    private List<ComponentException> checkForUnknownProperties(final Declaration declaration, final Map<String, String> overrides) {
        final Predicate<Map.Entry<String, String>> isSupported = entry -> {
            if (declaration.getReference(entry.getKey()) != null) return true;
            if (declaration.getParam(entry.getKey()) != null) return true;
            return false;
        };

        if (warnOnUnusedProperties) {
            overrides.entrySet().stream()
                    .filter(isSupported.negate())
                    .forEach(entry -> {
                        LOGGER.warning("Warning: Unused property '" + entry.getKey() + "' in " + declaration.getClazz().getName());
                    });

            return java.util.Collections.emptyList();
        } else {
            return overrides.entrySet().stream()
                    .filter(isSupported.negate())
                    .map(entry -> new UnknownPropertyException(declaration.getClazz(), entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList());
        }

    }

    private static List<ComponentException> checkForMisusedProperties(final Declaration declaration, final Map<String, String> overrides) {
        return overrides.entrySet().stream()
                .filter(entry -> declaration.getReference(entry.getKey()) != null)
                .filter(entry -> !entry.getValue().startsWith("@"))
                .map(entry -> new ComponentReferenceSyntaxException(declaration.getClazz(), entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());
    }

    private Map<String, String> selectOverrides(final String prefix) {
        final Map<String, String> overrides = new HashMap<>();

        this.parameters.entrySet().stream()
                .filter(entry -> entry.getKey().toLowerCase().startsWith(prefix))
                .peek(entry -> usedParameters.put(entry.getKey(), entry.getValue()))
                .forEach(entry -> {
                    final String key = entry.getKey().substring(prefix.length());
                    overrides.put(key, entry.getValue());
                });
        return overrides;
    }

    private void applyImplicitOverrides(final Declaration declaration) {
        // Error handling is above, so this logic is simple
        override(declaration, this.parameters);
    }

    private Class<?> loadComponentClass(final Map.Entry<String, String> entry) {
        final String className = entry.getValue().replace("new://", "");
        return loadDeclarationClass(className);
    }

    private static <T> void loadAnnotatedDefaults(final Declaration<T> declaration) {
        for (final org.tomitribe.util.reflect.Parameter parameter : declaration.getProducer().getParams()) {
            final String defaultValue = getDefault(parameter);
            final boolean isNullable = parameter.isAnnotationPresent(Nullable.class);

            if (parameter.isAnnotationPresent(Component.class)) {

                //@Since 3.0
                //Now we obtain the reference name from the Param annotation instead.
                String referenceName = null;
                try {
                    referenceName = parameter.getAnnotation(Param.class).value();
                } catch (final Exception e) {
                    // TODO Convert to an exception that advises the user
                    // to put the @Param annotation on the parameter
                    throw new ConstructionFailedException(declaration.clazz, e);
                }
                final Class<?> referenceType = parameter.getType();

                if (Collection.class.isAssignableFrom(referenceType)) {
                    final Type type = Generics.getType(parameter);
                    declaration.addCollectionReference(referenceName, (Class<?>) type, defaultValue, isNullable, referenceType);
                } else {
                    declaration.addReference(referenceName, referenceType, defaultValue, isNullable);
                }

            } else if (parameter.isAnnotationPresent(Param.class)) {

                final String optionName = parameter.getAnnotation(Param.class).value();
                declaration.addParam(optionName, defaultValue, isNullable);

            }
        }
    }

    private static String getDefault(final org.tomitribe.util.reflect.Parameter parameter) {
        final Default annotation = parameter.getAnnotation(Default.class);
        return annotation == null ? null : annotation.value();
    }

    public class Declaration<T> {
        private final String name;
        private final String sortingName;
        private final Class<T> clazz;
        private final Producer<T> producer;
        private final Map<String, ParamValue> params = new HashMap<>();
        private final List<InjectionPoint> injectionPoints = new ArrayList<>();
        private final Map<String, Reference> referencess = new HashMap<>();
        private T instance;

        public Declaration(final String name, final Class clazz) {
            this.name = name;
            this.producer = producer(clazz);
            this.clazz = producer.getType();
            this.sortingName = (name != null) ? name : clazz.getSimpleName() + java.lang.System.nanoTime();

            loadAnnotatedDefaults(this);
        }

        public Producer<T> getProducer() {
            return producer;
        }

        public String getReferenceId() {
            return sortingName;
        }

        public Class<T> getClazz() {
            return clazz;
        }

        public boolean isAssignableTo(final Class<?> clazz) {
            return clazz.isAssignableFrom(this.clazz);
        }

        public List<InjectionPoint> getInjectionPoints() {
            return injectionPoints;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return clazz.getName();
        }

        public Map<String, ParamValue> getParams() {
            return params;
        }

        public ParamValue getParam(String name) {
            return params.get(name.toLowerCase());
        }

        public void addParam(final String paramName, final String defaultValue, final boolean isNullable) {
            this.params.put(paramName.toLowerCase(), new ParamValue(paramName, defaultValue, isNullable));
        }


        public class ParamValue {
            private final String name;
            private final boolean isNullable;
            private String value;

            public ParamValue(final String name, final String value, final boolean isNullable) {
                this.name = name;
                this.value = value;
                this.isNullable = isNullable;
            }

            public String getName() {
                return name;
            }

            public String getValue() {
                return value;
            }

            public boolean isNullable() {
                return isNullable;
            }

            public void setValue(final String value) {
                this.value = value;
            }
        }


        public List<Reference> getUnresolvedReferences() {
            return referencess.values().stream()
                    .filter(Reference::isUnresolved)
                    .collect(Collectors.toList());
        }

        public void addCollectionReference(final String name, final Class<?> type, final String defaultValue, final boolean isNullable, final Class<?> collectionType) {
            this.referencess.put(name.toLowerCase(), new Reference(name, type, defaultValue, isNullable, collectionType));
        }

        public void addReference(final String name, final Class<?> type, final String defaultValue, final boolean isNullable) {
            this.referencess.put(name.toLowerCase(), new Reference(name, type, defaultValue, isNullable));
        }

        public Collection<Reference> getReferences() {
            return referencess.values();
        }

        public Reference getReference(final String name) {
            return this.referencess.get(name.toLowerCase());
        }

        /**
         * Returns a set of the ids for all references to declarations
         * This is used for sorting the dependency chain
         */
        public Set<String> getDeclarationReferences() {
            final Set<String> ids = new LinkedHashSet<>();

            for (final Reference reference : referencess.values()) {
                final Object target = reference.getTarget();

                if (target instanceof Declaration) {
                    final Declaration<?> declaration = (Declaration<?>) target;
                    ids.add(declaration.getReferenceId());
                    continue;
                }

                if (target instanceof Collection) {
                    final Collection<?> collection = (Collection<?>) target;
                    for (final Object element : collection) {
                        if (element instanceof Declaration) {
                            final Declaration<?> declaration = (Declaration<?>) element;
                            ids.add(declaration.getReferenceId());
                        }
                    }
                }
            }

            return ids;
        }

        public class Reference {
            private final String name;
            private final Class<?> type;
            private final Class<?> collectionType;
            private Object target;
            private boolean isNullable;

            public Reference(final String name, final Class<?> type, final Object target, final boolean isNullable) {
                this(name, type, target, isNullable, null);
            }

            public Reference(final String name, final Class<?> type, final Object target, final boolean isNullable, final Class<?> collectionType) {
                this.name = name;
                this.type = type;
                this.target = target;
                this.isNullable = isNullable;
                this.collectionType = collectionType;
            }

            public String getName() {
                return name;
            }

            public Class<?> getType() {
                return type;
            }

            public Class<?> getCollectionType() {
                return collectionType;
            }

            public void set(final Object referenceId) {
                this.target = referenceId;
            }

            public boolean isUnresolved() {
                return target == null || target instanceof String;
            }

            public Object getTarget() {
                return target;
            }

            public Declaration getDeclaration() {
                return Declaration.this;
            }

            public boolean isNullable() {
                return isNullable;
            }

            @Override
            public String toString() {
                return "Reference{" +
                        "name='" + name + '\'' +
                        ", type=" + type +
                        ", target=" + target +
                        ", isNullable=" + isNullable +
                        '}';
            }
        }


        public class NameInjection implements InjectionPoint {
            @Override
            public Object resolveValue() {
                return Declaration.this.getName();
            }
        }

        public class EventInjection implements InjectionPoint {
            private final Parameter parameter;

            public EventInjection(final Parameter parameter) {
                this.parameter = parameter;
            }

            @Override
            public Object resolveValue() {
                final Class<?> eventType = EventReferences.eventType(parameter);
                return consumersOf(eventType);
            }
        }

        public class ParamInjection implements InjectionPoint {

            private final Parameter parameter;
            private final Param param;
            private final Default defaultValue;
            private final boolean isNullable;

            public ParamInjection(final Parameter parameter) {
                this.parameter = parameter;
                this.param = parameter.getAnnotation(Param.class);
                this.defaultValue = parameter.getAnnotation(Default.class);
                this.isNullable = parameter.isAnnotationPresent(Nullable.class);
            }

            @Override
            public Object resolveValue() {
                final String parameterName = param.value();
                final ParamValue param = Declaration.this.getParam(parameterName);

                if (param == null || param.getValue() == null) {
                    if (isNullable) {
                        return null;

                    } else {
                        throw new MissingRequiredParamException(Declaration.this.clazz, parameterName);
                    }
                }

                try {
                    return Converter.convert(param.getValue(), parameter.getType(), parameterName);
                } catch (Exception e) {
                    throw new InvalidParamValueException(Declaration.this.clazz, e, parameterName, param.getValue(), parameter.getType());
                }
            }
        }

        public class ComponentInjection implements InjectionPoint {

            private final Component component;
            private final Param paramRef;
            private final Parameter parameter;
            private final Default defaultValue;

            public ComponentInjection(final Parameter parameter) {
                this.parameter = parameter;
                this.component = parameter.getAnnotation(Component.class);
                this.paramRef = parameter.getAnnotation(Param.class);
                this.defaultValue = parameter.getAnnotation(Default.class);
            }

            @Override
            public Object resolveValue() {
                // Grab the name as it was configured via the @Param(" ") annotation
                final Reference reference = Declaration.this.getReference(paramRef.value());

                // The following two should be unreachable statements
                if (reference == null)
                    throw new IllegalStateException(String.format("Reference %s is null", paramRef.value()));
                if (reference.getTarget() == null && !reference.isNullable())
                    throw new IllegalStateException(String.format("Reference value %s is null", reference.getName()));

                // if target is null and @Nullable is used on the parameter, then we are good
                if (reference.getTarget() == null && reference.isNullable())
                    return null;

                final Object target = reference.getTarget();

                if (target instanceof Collection) {
                    final Collection collection = (Collection) target;
                    final Collection<Object> copy = newInstance(collection.getClass());
                    for (final Object object : collection) {
                        if (object instanceof Declaration) {
                            final Declaration declaration = (Declaration) object;
                            copy.add(declaration.getInstance());
                        }

                        if (object instanceof Instance) {
                            final Instance instance = (Instance) object;
                            copy.add(instance.getObject());
                        }
                    }

                    return copy;
                }

                if (target instanceof Declaration) {
                    final Declaration declaration = (Declaration) target;
                    return declaration.getInstance();
                }

                if (target instanceof Instance) {
                    final Instance instance = (Instance) target;
                    return instance.getObject();
                }

                throw new IllegalStateException(String.format("Reference %s should be resolved at this point", paramRef.value()));
            }
        }

        private Instance<T> buildInstance() {
            return new Instance<T>(getReferenceId(), build());
        }

        public T getInstance() {
            return instance;
        }

        private T build() {
            return instance = producer.build();
        }

        private List<Object> getArguments() {
            return this.getInjectionPoints().stream()
                    .map(InjectionPoint::resolveValue)
                    .collect(Collectors.toList());
        }

        public Producer<T> producer(final Class<?> clazz) {
            final Method factoryMethod = Stream.of(clazz.getMethods())
                    .filter(method -> Modifier.isStatic(method.getModifiers()))
                    .filter(method -> !Void.TYPE.equals(method.getReturnType()))
                    .filter(method -> !method.getReturnType().isPrimitive())
                    .filter(method -> method.isAnnotationPresent(Factory.class))
                    .min(Comparator.comparing(Method::getName))
                    .orElse(null);

            if (factoryMethod != null) {
                return new FactoryMethodProducer(factoryMethod);
            }

            final Class<T> type = (Class<T>) clazz;
            return new ConstructorProducer(type, Constructors.findConstructor(type));
        }

        public class ConstructorProducer implements Producer<T> {
            private final Constructor<T> constructor;
            private final Class<T> clazz;

            public ConstructorProducer(final Class<T> clazz, final Constructor<T> constructor) {
                this.constructor = constructor;
                this.clazz = clazz;
            }

            @Override
            public Class<T> getType() {
                return clazz;
            }

            public Iterable<org.tomitribe.util.reflect.Parameter> getParams() {
                return org.tomitribe.util.reflect.Reflection.params(constructor);
            }

            public T build() {
                try {
                    // Convert the parameters to InjectionPoint instances
                    buildInjectionPoints(Declaration.this, constructor);

                    // Resolve or create the needed arguments
                    // This may involve creating other components
                    final List<Object> args = getArguments();

                    // Call our selected constructor
                    return constructor.newInstance(args.toArray());
                } catch (InvocationTargetException e) {
                    throw new ConstructionFailedException(clazz, e.getCause());
                } catch (Throwable e) {
                    throw new ConstructionFailedException(clazz, e);
                }
            }

            private void buildInjectionPoints(final Declaration declaration, final Constructor<?> constructor) {
                final List<InjectionPoint> points = new ArrayList<>();

                for (final Parameter parameter : constructor.getParameters()) {
                    points.add(buildInjectionPoint(constructor, parameter));
                }

                declaration.getInjectionPoints().clear();
                declaration.getInjectionPoints().addAll(points);
            }

            private InjectionPoint buildInjectionPoint(final Constructor constructor, final Parameter parameter) {
                if (parameter.isAnnotationPresent(Component.class))
                    return new Declaration.ComponentInjection(parameter);
                if (parameter.isAnnotationPresent(Param.class)) return new ParamInjection(parameter);
                if (parameter.isAnnotationPresent(Name.class)) return new Declaration.NameInjection();
                if (parameter.isAnnotationPresent(Event.class)) return new Declaration.EventInjection(parameter);

                throw new InvalidConstructorException(constructor.getDeclaringClass(), constructor);
            }
        }

        public class FactoryMethodProducer implements Producer<T> {
            private final Method method;

            public FactoryMethodProducer(final Method method) {
                this.method = method;
            }

            @Override
            public Class<T> getType() {
                return (Class<T>) method.getReturnType();
            }

            public Iterable<org.tomitribe.util.reflect.Parameter> getParams() {
                return org.tomitribe.util.reflect.Reflection.params(method);
            }

            public T build() {
                try {
                    // Select the constructor whose parameters are annotated with @Param, @Component or @Name

                    // Convert the parameters to InjectionPoint instances
                    buildInjectionPoints(Declaration.this, method);

                    // Resolve or create the needed arguments
                    // This may involve creating other components
                    final List<Object> args = getArguments();

                    // Call our selected constructor
                    return (T) method.invoke(null, args.toArray());
                } catch (InvocationTargetException e) {
                    throw new ConstructionFailedException(clazz, e.getCause());
                } catch (Throwable e) {
                    throw new ConstructionFailedException(clazz, e);
                }
            }

            private void buildInjectionPoints(final Declaration declaration, final Method method) {
                final List<InjectionPoint> points = new ArrayList<>();

                for (final Parameter parameter : method.getParameters()) {
                    points.add(buildInjectionPoint(method, parameter));
                }

                declaration.getInjectionPoints().clear();
                declaration.getInjectionPoints().addAll(points);
            }

            private InjectionPoint buildInjectionPoint(final Method method, final Parameter parameter) {
                if (parameter.isAnnotationPresent(Component.class))
                    return new Declaration.ComponentInjection(parameter);
                if (parameter.isAnnotationPresent(Param.class)) return new ParamInjection(parameter);
                if (parameter.isAnnotationPresent(Name.class)) return new Declaration.NameInjection();
                if (parameter.isAnnotationPresent(Event.class)) return new Declaration.EventInjection(parameter);

                throw new InvalidFactoryMethodException(method.getDeclaringClass(), method);
            }
        }

        @Override
        public String toString() {
            return "Declaration{" +
                    "name='" + name + '\'' +
                    ", clazz=" + clazz +
                    '}' + refs();

        }

        private String refs() {
            if (getReferences().size() == 0) return "";
            final List<String> names = getReferences().stream()
                    .map(Reference::getName)
                    .collect(Collectors.toList());
            return " Refs{" + Join.join(", ", names + "}");
        }

    }

    public interface Producer<T> {

        T build();

        Iterable<org.tomitribe.util.reflect.Parameter> getParams();

        Class<T> getType();
    }


    private Class<?> loadDeclarationClass(final String type) {
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            // Load the components implementation class
            return loader.loadClass(type);
        } catch (ClassNotFoundException e) {
            throw new MissingComponentClassException(type, e);
        }
    }

    private <T> T create(final Class<T> type, final String name) {

        try {
            final Declaration<T> declaration = createDeclaration(type, name);

            final List<Declaration> declarations = new ArrayList<Declaration>();
            declarations.add(declaration);

            build(declarations);

            return declaration.getInstance();
        } catch (ConstructionFailedException e) {
            throw e;
        } catch (Throwable e) {
            throw new ConstructionFailedException(type, e);
        }
    }

    public static SystemBuilder builder() {
        return new SystemBuilder();
    }

    public static class SystemBuilder {

        private final AtomicInteger refs = new AtomicInteger(100);

        private final Properties properties = new Properties();

        private final HashMap<String, Object> objects = new HashMap<>();

        private boolean warnOnUnusedProperties = false;

        /**
         * Build an instance of the specified class
         */
        public DefinitionBuilder definition(final Class<?> type) {
            return new DefinitionBuilder(type);
        }

        /**
         * Build an instance of the specified class with the specified name.
         * The name will only be relevant if the class uses the @Name
         */
        public DefinitionBuilder definition(final Class<?> type, final String name) {
            return new DefinitionBuilder(type, name);
        }

        /**
         * Use of comp(*) and option(*) create the expectation the
         * class definition has explicitly declared a need for these
         * things via the @Component and @Param annotations.
         *
         * When no such declaration exists an exception will be thrown
         * on build() unless warnOnUnusedProperties() has been called.
         */
        public SystemBuilder warnOnUnusedProperties() {
            warnOnUnusedProperties = true;
            return this;
        }

        /**
         * Adds an object to be possibly consumed by the created instances.
         *
         * As no name is specified for this object it may only be referenced by type
         * and adding more objects of the same type will create ambiguity in resolution.
         *
         * @param value The object instance we anticipate may be useful to the created instances.
         */
        public SystemBuilder add(final Object value) {
            Objects.requireNonNull(value, "value must not be null");

            final String name = "unnamed$" + value.getClass().getSimpleName() + refs.incrementAndGet();
            objects.put(name, value);

            return this;
        }

        /**
         * Adds an object to be possibly consumed by the created instances.
         *
         * @param value The object instance we anticipate may be useful to the created instances.
         */
        public SystemBuilder add(final String name, final Object value) {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(value, "value must not be null");

            objects.put(name, value);

            return this;
        }


        public class DefinitionBuilder {

            private final HashMap<String, Object> required = new HashMap<>();
            private final Class<?> type;
            private final String name;
            private final System.Declaration<?> declaration;

            public DefinitionBuilder(final Class<?> type) {
                this(type, "instance" + refs.incrementAndGet());
            }

            public DefinitionBuilder(final Class<?> type, final String name) {
                Objects.requireNonNull(type, "type must not be null");
                Objects.requireNonNull(name, "name must not be null");

                this.type = type;
                this.name = name;
                properties.put(this.name, "new://" + type.getName());
                declaration = new System().new Declaration<>(name, type);
            }

            /**
             * Build an instance of the specified class
             */
            public DefinitionBuilder definition(final Class<?> type) {
                validate();
                return SystemBuilder.this.definition(type);
            }

            /**
             * Build an instance of the specified class with the specified name.
             * The name will only be relevant if the class uses the @Name
             */
            public DefinitionBuilder definition(final Class<?> type, final String name) {
                validate();
                return SystemBuilder.this.definition(type, name);
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
            public DefinitionBuilder comp(final String name, final Object value) {
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
                required.put(refName, value);
                return this;
            }

            /**
             * Sets the value of a declared @Component reference using type
             * alone to resolve the injection.  Useful in scenarios were we
             * do not know or care what name the class has used to refer to the
             * type, we simply know it needs or must accept an object of the
             * provided type.
             *
             * If the class does not declare a @Component reference with the
             * appropriate type an exception will be thrown unless `warnOnUnusedProperties`
             *
             * Use add(*) to offer objects that may be useful to the created instance
             * but are not known as explicit requirements.
             */
            public DefinitionBuilder comp(final Object value) {
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
                required.put(refName, value);
                return this;
            }

            public DefinitionBuilder comp(final String name, final String refName) {
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
             * @param name Sets an explicit name which enables several instance of the same type to exist and be referenced
             * @param value The object instance we anticipate may be useful to the created instance.
             */
            public DefinitionBuilder optional(final String name, final Object value) {
                Objects.requireNonNull(name, "name must not be null");
                Objects.requireNonNull(value, "value must not be null");

                if (declaration.getParam(name) != null) {

                    properties.put(this.name + "." + name, value);

                } else if (declaration.getReference(name) != null) {

                    comp(name, value);

                }

                return this;
            }

            /**
             * Sets the value of an explicitly declared @Param reference
             * declared in the class of the instance we are creating.
             *
             * If the class does not declare a @Param reference with the
             * name specified an exception will be thrown unless `warnOnUnusedProperties`
             *
             * Use add(*) to offer objects that may be useful to the created instance
             * but are not known as explicit requirements.
             */
            public DefinitionBuilder param(final String name, final String value) {
                Objects.requireNonNull(name, "name must not be null");
                Objects.requireNonNull(value, "value must not be null");

                properties.put(this.name + "." + name, value);
                return this;
            }

            /**
             * Sets the value of an explicitly declared @Param reference
             * declared in the class of the instance we are creating.
             *
             * If the class does not declare a @Param reference with the
             * name specified an exception will be thrown unless `warnOnUnusedProperties`
             *
             * Use add(*) to offer objects that may be useful to the created instance
             * but are not known as explicit requirements.
             */
            public DefinitionBuilder param(final String name, final Integer value) {
                Objects.requireNonNull(name, "name must not be null");
                Objects.requireNonNull(value, "value must not be null");

                properties.put(this.name + "." + name, value);
                return this;
            }

            /**
             * Sets the value of an explicitly declared @Param reference
             * declared in the class of the instance we are creating.
             *
             * If the class does not declare a @Param reference with the
             * name specified an exception will be thrown unless `warnOnUnusedProperties`
             *
             * Use add(*) to offer objects that may be useful to the created instance
             * but are not known as explicit requirements.
             */
            public DefinitionBuilder param(final String name, final Boolean value) {
                Objects.requireNonNull(name, "name must not be null");
                Objects.requireNonNull(value, "value must not be null");

                properties.put(this.name + "." + name, value);
                return this;
            }

            /**
             * Sets the value of an explicitly declared @Param reference
             * declared in the class of the instance we are creating.
             *
             * If the class does not declare a @Param reference with the
             * name specified an exception will be thrown unless `warnOnUnusedProperties`
             *
             * Use add(*) to offer objects that may be useful to the created instance
             * but are not known as explicit requirements.
             */
            public DefinitionBuilder param(final String name, final Double value) {
                Objects.requireNonNull(name, "name must not be null");
                Objects.requireNonNull(value, "value must not be null");

                properties.put(this.name + "." + name, value);
                return this;
            }

            /**
             * Sets the value of an explicitly declared @Param reference
             * declared in the class of the instance we are creating.
             *
             * If the class does not declare a @Param reference with the
             * name specified an exception will be thrown unless `warnOnUnusedProperties`
             *
             * Use add(*) to offer objects that may be useful to the created instance
             * but are not known as explicit requirements.
             */
            public DefinitionBuilder param(final String name, final Float value) {
                Objects.requireNonNull(name, "name must not be null");
                Objects.requireNonNull(value, "value must not be null");

                properties.put(this.name + "." + name, value);
                return this;
            }

            /**
             * Sets the value of an explicitly declared @Param reference
             * declared in the class of the instance we are creating.
             *
             * If the class does not declare a @Param reference with the
             * name specified an exception will be thrown unless `warnOnUnusedProperties`
             *
             * Use add(*) to offer objects that may be useful to the created instance
             * but are not known as explicit requirements.
             */
            public DefinitionBuilder param(final String name, final Long value) {
                Objects.requireNonNull(name, "name must not be null");
                Objects.requireNonNull(value, "value must not be null");

                properties.put(this.name + "." + name, value);
                return this;
            }

            /**
             * Sets the value of an explicitly declared @Param reference
             * declared in the class of the instance we are creating.
             *
             * If the class does not declare a @Param reference with the
             * name specified an exception will be thrown unless `warnOnUnusedProperties`
             *
             * Use add(*) to offer objects that may be useful to the created instance
             * but are not known as explicit requirements.
             */
            public DefinitionBuilder param(final String name, final Short value) {
                Objects.requireNonNull(name, "name must not be null");
                Objects.requireNonNull(value, "value must not be null");

                properties.put(this.name + "." + name, value);
                return this;
            }

            /**
             * Sets the value of an explicitly declared @Param reference
             * declared in the class of the instance we are creating.
             *
             * If the class does not declare a @Param reference with the
             * name specified an exception will be thrown unless `warnOnUnusedProperties`
             *
             * Use add(*) to offer objects that may be useful to the created instance
             * but are not known as explicit requirements.
             */
            public DefinitionBuilder param(final String name, final Byte value) {
                Objects.requireNonNull(name, "name must not be null");
                Objects.requireNonNull(value, "value must not be null");

                properties.put(this.name + "." + name, value);
                return this;
            }

            /**
             * Sets the value of an explicitly declared @Param reference
             * declared in the class of the instance we are creating.
             *
             * If the class does not declare a @Param reference with the
             * name specified an exception will be thrown unless `warnOnUnusedProperties`
             *
             * Use add(*) to offer objects that may be useful to the created instance
             * but are not known as explicit requirements.
             */
            public DefinitionBuilder param(final String name, final Character value) {
                Objects.requireNonNull(name, "name must not be null");
                Objects.requireNonNull(value, "value must not be null");

                properties.put(this.name + "." + name, value);
                return this;
            }

            /**
             * Sets the value of an explicitly declared @Param reference
             * declared in the class of the instance we are creating.
             *
             * If the class does not declare a @Param reference with the
             * name specified an exception will be thrown unless `warnOnUnusedProperties`
             *
             * Use add(*) to offer objects that may be useful to the created instance
             * but are not known as explicit requirements.
             */
            public <E extends Enum<E>> DefinitionBuilder param(final String name, final E value) {
                Objects.requireNonNull(name, "name must not be null");
                Objects.requireNonNull(value, "value must not be null");

                properties.put(this.name + "." + name, value.name());
                return this;
            }

            private void validate() {
                /*
                 * Add the optional objects for component references
                 */
                for (final Map.Entry<String, Object> entry : required.entrySet()) {

                    final boolean failIfNotUsed = !warnOnUnusedProperties;
                    if (failIfNotUsed) {
                        boolean used = false;
                        final Object value = entry.getValue();
                        for (final System.Declaration<?>.Reference reference : declaration.getReferences()) {
                            if (reference.getType().isAssignableFrom(value.getClass())) {
                                used = true;
                            }
                        }
                        if (!used) {
                            throw new MissingComponentDeclarationException(type, value.getClass());
                        }
                    }

                    objects.put(entry.getKey(), entry.getValue());
                }
            }

            public System build() {
                validate();
                return SystemBuilder.this.build();
            }
        }

        private System build() {
            final System system = new System(warnOnUnusedProperties);

            /*
             * Add the optional objects for component references
             */
            for (final Map.Entry<String, Object> entry : objects.entrySet()) {
                system.add(entry.getKey(), entry.getValue());
            }

            /*
             * Swap the context classloader to whatever ClassLoader
             * loaded the target class
             */
            system.load(properties);

            return system;
        }

    }


    static Collection<Object> newInstance(final Class<?> collectionType) {
        if (collectionType == null) {
            throw new IllegalArgumentException("collectionType must not be null");
        }

        if (!Collection.class.isAssignableFrom(collectionType)) {
            throw new IllegalArgumentException("Not a Collection type: " + collectionType.getName());
        }

        /*
         * If it's a concrete class, just instantiate it.
         */
        if (!collectionType.isInterface()
                && !java.lang.reflect.Modifier.isAbstract(collectionType.getModifiers())) {
            try {
                return (Collection<Object>) collectionType.getDeclaredConstructor().newInstance();
            } catch (final Exception e) {
                throw new IllegalStateException("Failed to instantiate collection type: " + collectionType.getName(), e);
            }
        }

        /*
         * Interfaces / abstract types: choose sane defaults.
         */
        if (List.class.isAssignableFrom(collectionType)) {
            return new ArrayList<>();
        }

        if (Set.class.isAssignableFrom(collectionType)) {
            return new LinkedHashSet<>();
        }

        if (Queue.class.isAssignableFrom(collectionType)) {
            return new ArrayDeque<>();
        }

        /*
         * Fallback — very rare, but keeps behavior predictable.
         */
        throw new IllegalStateException("Unsupported Collection type: " + collectionType.getName());
    }

}
