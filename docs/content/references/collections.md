---
title: "Collections"
weight: 3
description: "Inject multiple components into a collection"
---

A `@Component` parameter can be any `Collection` type to inject multiple components at once. This supports both by-type and by-name resolution.

## Supported Collection Types

Pixie supports any type assignable to `java.util.Collection`:

| Parameter Type | Default Implementation |
|---------------|----------------------|
| `List<T>` | `ArrayList` |
| `Set<T>` | `LinkedHashSet` (preserves insertion order) |
| `Queue<T>` | `ArrayDeque` |
| `Collection<T>` | `ArrayList` |
| Any concrete collection class | Instantiated directly |

```java
@Param("handlers") @Component final List<Handler> handlers
@Param("handlers") @Component final Set<Handler> handlers
@Param("handlers") @Component final Queue<Handler> handlers
```

## Collect All by Type

When no value is specified for the property, Pixie collects **all** components of the matching type:

```java
public class SolarSystem {
    public SolarSystem(@Name final String name,
                       @Param("worlds") @Component final List<World> worlds) {
        this.worlds = worlds;
    }
}
```

```properties
helios = new://org.example.SolarSystem

mercury = new://org.example.World
venus = new://org.example.World
earth = new://org.example.World
mars = new://org.example.World
```

All four `World` instances will be injected into the `worlds` list. Components added via `system.add()` and components defined in properties are both collected.

As with [single references]({{< relref "/references/by-type" >}}), matching is based on assignability — the element type can be an interface or superclass.

## Select Specific by Name

To inject only specific components in a specific order, list their names with `@` references separated by spaces:

```properties
helios = new://org.example.SolarSystem
helios.worlds = @jupiter @earth @mars
```

This injects exactly three `World` instances, in the order listed. The remaining `World` components in the system are not included.

## Generic Filtering

When the collection element type has generic type arguments, only components with matching type arguments are collected:

```java
public interface RequestHandler<I, O> {
    O handle(I input);
}

public class CountHandler implements RequestHandler<String, Integer> { ... }
public class LengthHandler implements RequestHandler<String, Integer> { ... }
public class ValidHandler implements RequestHandler<String, Boolean> { ... }
public class FetchHandler implements RequestHandler<URI, String> { ... }
```

```java
public class Pipeline {
    public Pipeline(@Param("handlers") @Component
                    final List<RequestHandler<String, Integer>> handlers) {
        // handlers contains CountHandler and LengthHandler
        // ValidHandler and FetchHandler are excluded
    }
}
```

If the collection uses a raw type, all implementations are collected regardless of type arguments:

```java
// Collects ALL RequestHandler implementations
@Param("handlers") @Component List<RequestHandler> handlers
```

Wildcards work too:

```java
// Collects any RequestHandler whose input type extends Number
@Param("handlers") @Component List<RequestHandler<? extends Number, ?>> handlers
```

See [Generics]({{< relref "/references/generics" >}}) for more on type argument matching.
