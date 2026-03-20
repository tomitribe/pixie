---
title: "Changelog"
weight: 7
description: "Version history and major features"
---

## 2.12 — Generic Type Matching

Component injection (`@Component`) now uses generic type arguments when resolving which components are eligible. A parameter of type `RequestHandler<String, Integer>` will only match implementations with those exact type arguments — previously any `RequestHandler` would match.

- **Wildcard support** — `? extends Number`, `? super Integer`, and unbounded `?` in injection points
- **Collection injection** — `List<RequestHandler<String, Integer>>` collects only matching implementations
- **All producer types** — generics resolved correctly across constructors, `@Factory` methods, `@Builder` patterns, and pre-built instances
- **Mixed resolution** — type arguments can come from both the producer declaration and the class hierarchy
- **Backwards compatible** — raw type injection points (no generics) continue to match any implementation

## 2.10 — Factory Methods and Builders

Two new producer types for creating components.

- **`@Factory`** — annotate a public static method to serve as the component factory. Parameters use the same Pixie annotations (`@Param`, `@Component`, `@Name`, `@Event`). The factory can live in the same class or a separate class.
- **`@Builder`** — annotate a public static method that returns a builder object. Pixie calls annotated setter methods on the builder, then calls `build()` to produce the component. Also supports separate builder classes.

Factory methods take priority over builders, and both take priority over constructors.

## 2.9 — Collection Injection

Inject all components of a given type as a `List`:

```java
@Param("handlers") @Component List<RequestHandler> handlers
```

Pixie collects all matching components from both pre-built instances and definitions yet to be constructed.

## 2.8 — `@Component` with `@Param`

Changed `@Component("name")` to `@Param("name") @Component`. This separates the concern of naming a property from declaring it as a component reference, making the annotation model more consistent.

## 2.5 — Event System Improvements

- Observer exceptions are now caught and do not propagate to the event producer
- `ObserverNotFound` event fired when no observers exist for a given event
- Property usage tracking to help diagnose unused or misconfigured properties

## 2.4 — Subclassable System

`System` can now be subclassed to augment behavior.

## 2.2 — Lookup by Annotation

New `getAnnotated(Class<? extends Annotation>)` method to find components by class-level annotation.

## 2.1 — Lookup by Name and Type

New `get(Class<T>, String name)` method to retrieve a component by both type and name.

## 2.0 — Events and Builder API

Major release introducing two new subsystems:

- **Event system** — `@Event` injects a `Consumer<T>` for firing events; `@Observes` marks methods as listeners. Built-in lifecycle events (`PixieLoad`, `PixieClose`, `ComponentAdded`, etc.)
- **`System.builder()`** — fluent Builder API for defining components in code without properties files
- **`Instance.builder()`** — build a single object without a full System
- **`@Param`** — renamed from `@Option` to better reflect that these are required configuration parameters
- **`system.add()`** — add pre-built instances to the System

## 1.0 — Initial Release

Constructor-based dependency injection and configuration from properties files.

- `@Param`, `@Component`, `@Default`, `@Nullable`, `@Name` annotations
- `new://` properties syntax for declaring components
- Automatic type conversion via tomitribe-util `Converter`
- Case-insensitive property matching
- Strict property validation
