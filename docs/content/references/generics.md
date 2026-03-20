---
title: "Generics"
weight: 4
description: "Narrow component matching using generic type arguments"
---

When a `@Component` parameter uses generic type arguments, Pixie narrows matching to only those components whose resolved generic types are compatible. This works for interfaces, superclasses, and any depth of inheritance.

If the parameter is a raw type (no generics), all implementations are eligible — this is fully backwards compatible.

## Example

```java
public interface RequestHandler<I, O> {
    O handle(I input);
}

public class ApiGateway {
    public ApiGateway(
            @Param("handler") @Component
            final RequestHandler<APIGatewayProxyRequestEvent,
                                 APIGatewayV2HTTPResponse> handler) {
        // Only RequestHandler implementations with matching type
        // arguments will be injected. Other RequestHandler
        // implementations are ignored.
    }
}
```

## Wildcards

Wildcards in `@Component` parameters follow standard Java assignability rules:

```java
// Matches any RequestHandler whose first type argument extends Number
@Component RequestHandler<? extends Number, ?> handler

// Matches any RequestHandler whose first type argument is a supertype of Integer
@Component RequestHandler<? super Integer, ?> handler

// Matches any RequestHandler regardless of type arguments
@Component RequestHandler<?, ?> handler
```

`? extends Number` matches `Integer`, `Long`, `Double`, etc. `? super Integer` matches `Integer`, `Number`, `Object`, etc.

## Across Producer Types

Generic type information is correctly resolved and matched across all component [producer types]({{< relref "/producers" >}}):

- **Constructor-based** — generics are resolved from the class hierarchy
- **Factory methods** (`@Factory`) — generics are taken from the method's generic return type
- **Builders** (`@Builder`) — generics are resolved through the builder method's parameterized return type and the `build()` method's return type
- **Pre-built instances** (`system.add()`) — generics are resolved from the runtime class

### Mixed Scenarios

Type arguments can come from multiple sources and are correctly stitched together — some from the producer declaration, others from the class hierarchy.

For example, a factory returning `BooleanHandler<String>` where `BooleanHandler<I> implements RequestHandler<I, Boolean>` will correctly resolve to `RequestHandler<String, Boolean>`.

## Collections

Generic matching also applies to [collection injection]({{< relref "/references/collections" >}}). When the `List` element type has generic type arguments, only components with matching type arguments are collected.
