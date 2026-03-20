---
title: "Wildcards"
weight: 2
description: "Wildcard support in component type matching"
---

Wildcards in `@Component` parameters are supported and follow standard Java assignability rules.

## Examples

```java
// Matches any RequestHandler whose first type argument extends Number
@Component RequestHandler<? extends Number, ?> handler

// Matches any RequestHandler whose first type argument is a supertype of Integer
@Component RequestHandler<? super Integer, ?> handler

// Matches any RequestHandler regardless of type arguments
@Component RequestHandler<?, ?> handler
```

Wildcard matching follows the same rules as Java's type system — `? extends Number` matches `Integer`, `Long`, `Double`, etc., while `? super Integer` matches `Integer`, `Number`, `Object`, etc.
