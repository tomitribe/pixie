---
title: "Builder API"
weight: 2
description: "Define components fluently in code"
---

The Builder API lets you define your component system in code without properties files.

## Example

```java
final System system = System.builder()

        .definition(Person.class, "jane")
        .param("age", 37)
        .comp("address", "home")

        .definition(Address.class, "home")
        .param("street", "820 Roosevelt Street")
        .param("city", "River Falls")
        .param("state", "WI")
        .param("zipcode", "54022")

        .build();

final Person person = system.get(Person.class);
```

## API

- `definition(Class, name)` — Declare a new component with the given class and name
- `param(name, value)` — Set a `@Param` value on the current component
- `comp(name, reference)` — Set a `@Component` reference on the current component
- `build()` — Build the `System` and construct all components
