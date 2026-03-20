---
title: "Overview"
weight: 1
description: "What is Pixie and why use it"
---

Pixie is a tiny ~100KB jar that handles configuration, dependency injection, and events. It can handle any scenario where you would use reflection to instantiate a Java object.

## Key Features

- **Configuration** — Map properties files directly to constructor parameters
- **Dependency Injection** — Constructor-based injection with automatic type resolution
- **Events** — Fire and observe events across components
- **Type Safety** — Full generic type matching for component resolution
- **Zero Dependencies** — Only depends on `tomitribe-util`
- **Validation** — Strict property validation catches typos and misconfigurations at startup

## How It Works

Pixie uses constructor annotations to map configuration values to Java objects. You annotate your constructor parameters with `@Param`, `@Component`, `@Event`, or `@Name`, then Pixie builds your objects from either a properties file or a fluent builder API.

```java
public class Person {
    private final String name;
    private final Integer age;
    private final Address address;

    public Person(@Name final String name,
                  @Param("age") final Integer age,
                  @Param("address") @Component final Address address) {
        this.name = name;
        this.age = age;
        this.address = address;
    }
}
```

Pixie reads the annotations and wires everything together — no XML, no classpath scanning, no magic.
