---
title: "Producer Types"
weight: 4
description: "Generic resolution across all component producer types"
---

Generic type information is correctly resolved and matched across all component producer types:

- **Constructor-based** — generics are resolved from the class hierarchy
- **Factory methods** (`@Factory`) — generics are taken from the method's generic return type
- **Builders** (`@Builder`) — generics are resolved through the builder method's parameterized return type and the `build()` method's return type
- **Pre-built instances** (`system.add()`) — generics are resolved from the runtime class

## Mixed Scenarios

Mixed scenarios where some type arguments come from the producer declaration and others from the class hierarchy are fully supported.

For example, a factory returning `BooleanHandler<String>` where `BooleanHandler<I> implements RequestHandler<I, Boolean>` will correctly resolve to `RequestHandler<String, Boolean>`.
