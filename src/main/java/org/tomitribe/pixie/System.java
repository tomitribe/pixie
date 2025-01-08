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

import org.tomitribe.pixie.comp.ConstructionFailedException;
import org.tomitribe.pixie.comp.Constructors;
import org.tomitribe.pixie.comp.EventReferences;
import org.tomitribe.pixie.comp.InjectionPoint;
import org.tomitribe.pixie.comp.InvalidParamValueException;
import org.tomitribe.pixie.comp.MissingComponentClassException;
import org.tomitribe.pixie.comp.MissingRequiredParamException;
import org.tomitribe.pixie.comp.MultipleComponentIssuesException;
import org.tomitribe.pixie.comp.NamedComponentNotFoundException;
import org.tomitribe.pixie.comp.References;
import org.tomitribe.pixie.comp.UnknownPropertyException;
import org.tomitribe.pixie.event.ComponentAdded;
import org.tomitribe.pixie.event.PixieClose;
import org.tomitribe.pixie.event.PixieLoad;
import org.tomitribe.pixie.observer.ObserverManager;
import org.tomitribe.pixie.comp.ComponentException;
import org.tomitribe.pixie.comp.ComponentReferenceSyntaxException;
import org.tomitribe.pixie.comp.InvalidConstructorException;
import org.tomitribe.pixie.comp.InvalidNullableWithDefaultException;
import org.tomitribe.util.Join;
import org.tomitribe.util.editor.Converter;

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This class is designed to never be seen.  We want to discourage
 * the need to reference a class which does so much.  Ideally you
 * are getting your needs met through dependency injection.
 */
public class System implements Closeable {

    private final boolean warnOnUnusedProperties;

    private final Map<String, String> parameters = new ConcurrentHashMap<>();

    private final List<Instance> objects = new CopyOnWriteArrayList<>();

    private final ObserverManager observerManager = new ObserverManager();

    private static final Logger LOGGER = Logger.getLogger(System.class.getName());

    public System() {
        this(new Properties(), false);
    }

    public System(final boolean wantOnUnusedProperties) {
        this(new Properties(), wantOnUnusedProperties);
    }

    public System(final Properties properties) {
        this(properties, false);
    }

    public System(final Properties properties, final boolean warnOnUnusedProperties) {
        this.warnOnUnusedProperties = warnOnUnusedProperties;
        // Add System as a component that can be injected
        add(this, "system");
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
                .map(this::createDeclaration)
                .collect(Collectors.toList());

        build(declarations);

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

        @Override
        public String toString() {
            return "Instance{" +
                    "name='" + name + '\'' +
                    ", object=" + object +
                    '}';
        }
    }

    public <T> void add(final T value, final String name) {
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
        if (declaration.getName() == null) return Collections.EMPTY_LIST;

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

    private static void override(final Declaration declaration, final Map<String, String> overrides) {
        for (final Map.Entry<String, String> property : overrides.entrySet()) {
            if (declaration.getParam(property.getKey()) != null) {

                declaration.getParam(property.getKey()).setValue(property.getValue());

            } else if (declaration.getReference(property.getKey()) != null && property.getValue().startsWith("@")) {

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

            return Collections.emptyList();
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
        for (final Parameter parameter : declaration.constructor.getParameters()) {
            final String defaultValue = getDefault(parameter);
            final boolean isNullable = parameter.isAnnotationPresent(Nullable.class);

            if (parameter.isAnnotationPresent(Component.class)) {

                final String referenceName = parameter.getAnnotation(Component.class).value();
                final Class<?> referenceType = parameter.getType();

                declaration.addReference(referenceName, referenceType, defaultValue, isNullable);

            } else if (parameter.isAnnotationPresent(Param.class)) {

                final String optionName = parameter.getAnnotation(Param.class).value();
                declaration.addParam(optionName, defaultValue, isNullable);

            }
        }
    }

    private static String getDefault(final Parameter parameter) {
        final Default annotation = parameter.getAnnotation(Default.class);
        return annotation == null ? null : annotation.value();
    }

    public class Declaration<T> {
        private final String name;
        private final String sortingName;
        private final Class<T> clazz;
        private final Constructor<T> constructor;
        private final Map<String, ParamValue> params = new HashMap<>();
        private final List<InjectionPoint> injectionPoints = new ArrayList<>();
        private T instance;

        public Declaration(final String name, final Class clazz) {
            this.name = name;
            this.clazz = clazz;
            this.constructor = Constructors.findConstructor(this.clazz);
            this.sortingName = (name != null) ? name : clazz.getSimpleName() + java.lang.System.nanoTime();

            loadAnnotatedDefaults(this);
        }

        public String getReferenceId() {
            return sortingName;
        }

        public Class<T> getClazz() {
            return clazz;
        }

        public Constructor<T> getConstructor() {
            return constructor;
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


        private List<InjectionPoint> getInjectionPoints(final Constructor<?> constructor) {
            final List<InjectionPoint> points = new ArrayList<>();

            for (final Parameter parameter : constructor.getParameters()) {
                points.add(buildInjectionPoint(constructor, parameter));
            }
            return points;
        }

        private InjectionPoint buildInjectionPoint(final Constructor constructor, final Parameter parameter) {
            if (parameter.isAnnotationPresent(Component.class)) return new Declaration.ComponentInjection(parameter);
            if (parameter.isAnnotationPresent(Param.class)) return new ParamInjection(parameter);
            if (parameter.isAnnotationPresent(Name.class)) return new Declaration.NameInjection();
            if (parameter.isAnnotationPresent(Event.class)) return new Declaration.EventInjection(parameter);

            throw new InvalidConstructorException(constructor.getDeclaringClass(), constructor);
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

        private final Map<String, Reference> referencess = new HashMap<>();

        public List<Reference> getUnresolvedReferences() {
            return referencess.values().stream()
                    .filter(Reference::isUnresolved)
                    .collect(Collectors.toList());
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
            return referencess.values().stream()
                    .map(Reference::getTarget)
                    .filter(o -> o instanceof Declaration)
                    .map(Declaration.class::cast)
                    .map(Declaration::getReferenceId)
                    .collect(Collectors.toSet());
        }

        public class Reference {
            private final String name;
            private final Class<?> type;
            private Object target;
            private boolean isNullable;

            public Reference(final String name, final Class<?> type, final Object target, final boolean isNullable) {
                this.name = name;
                this.type = type;
                this.target = target;
                this.isNullable = isNullable;
            }

            public String getName() {
                return name;
            }

            public Class<?> getType() {
                return type;
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
                return System.this.observerManager.consumersOf(eventType);
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
            private final Parameter parameter;
            private final Default defaultValue;

            public ComponentInjection(final Parameter parameter) {
                this.parameter = parameter;
                this.component = parameter.getAnnotation(Component.class);
                this.defaultValue = parameter.getAnnotation(Default.class);
            }

            @Override
            public Object resolveValue() {
                // Grab the name as it was configured
                final Reference reference = Declaration.this.getReference(component.value());

                // The following two should be unreachable statements
                if (reference == null)
                    throw new IllegalStateException(String.format("Reference %s is null", component.value()));
                if (reference.getTarget() == null && !reference.isNullable())
                    throw new IllegalStateException(String.format("Reference value %s is null", reference.getName()));

                // if target is null and @Nullable is used on the parameter, then we are good
                if (reference.getTarget() == null && reference.isNullable())
                    return null;

                final Object target = reference.getTarget();

                if (target instanceof Declaration) {
                    final Declaration declaration = (Declaration) target;
                    return declaration.getInstance();
                }

                if (target instanceof Instance) {
                    final Instance instance = (Instance) target;
                    return instance.getObject();
                }

                throw new IllegalStateException(String.format("Reference %s should be resolved at this point", component.value()));
            }
        }

        private Instance<T> buildInstance() {
            return new Instance<T>(getReferenceId(), build());
        }

        public T getInstance() {
            return instance;
        }

        private T build() {
            try {
                // Select the constructor whose parameters are annotated with @Param, @Component or @Name
                final Constructor<T> constructor = Constructors.findConstructor(clazz);

                // Convert the parameters to InjectionPoint instances
                buildInjectionPoints(this, constructor);

                // Resolve or create the needed arguments
                // This may involve creating other components
                final List<Object> args = getArguments();

                // Call our selected constructor
                this.instance = constructor.newInstance(args.toArray());
                return instance;
            } catch (InvocationTargetException e) {
                throw new ConstructionFailedException(clazz, e.getCause());
            } catch (Throwable e) {
                throw new ConstructionFailedException(clazz, e);
            }
        }

        private void buildInjectionPoints(final Declaration declaration, final Constructor<?> constructor) {
            final List<InjectionPoint> injectionPoints = declaration.getInjectionPoints(constructor);
            declaration.getInjectionPoints().clear();
            declaration.getInjectionPoints().addAll(injectionPoints);
        }

        private List<Object> getArguments() {
            return this.getInjectionPoints().stream()
                    .map(InjectionPoint::resolveValue)
                    .collect(Collectors.toList());
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

}
