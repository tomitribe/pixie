---
title: "Collection Injection"
weight: 3
description: "Inject collections of components filtered by generic type"
---

Generic matching also applies to collection injection points. Only components with matching type arguments are collected.

## Example

```java
// Collects only RequestHandler<String, Integer> implementations
// — not all RequestHandler implementations
@Param("handlers") @Component List<RequestHandler<String, Integer>> handlers
```

This is useful when you have multiple implementations of a generic interface and want to inject only the subset that matches specific type arguments.
