---
title: "Annotations"
weight: 2
description: "Constructor annotations for configuration and dependency injection"
---

Pixie supports constructor injection. Each parameter of the constructor Pixie will use must be annotated with one of the following annotations.

All annotations are in the `org.tomitribe.pixie` package.

| Annotation   | Purpose | Example |
|-------------|---------|---------|
| `@Param`     | Maps a constructor parameter to a config property | `@Param("username") String username` |
| `@Default`   | Provides a default value if the property is missing | `@Param("country") @Default("USA") String country` |
| `@Component` | Injects a dependent object built by or given to the Pixie `System` | `@Component PaymentProcessor processor` |
| `@Nullable`  | Allows a property to be `null` if missing | `@Nullable @Param("footer") String footer` |
| `@Name`      | Injects the component's name from the config | `@Name String serviceName` |
| `@Event`     | Injects a `Consumer<T>` to fire events | `@Event Consumer<OrderPlaced> event` |
| `@Observes`  | Marks a method as an event listener | `void onEvent(@Observes OrderPlaced event)` |
