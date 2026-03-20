---
title: "Type Matching"
weight: 1
description: "How Pixie resolves generic type arguments"
---

When a `@Component` parameter uses generic type arguments, Pixie will narrow matching to only those components whose resolved generic types are compatible.

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

This works for interfaces, superclasses, and any depth of inheritance.

If the parameter is a raw type (no generics), all implementations are eligible as before — this is fully backwards compatible.
