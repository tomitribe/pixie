---
title: "Pre-built Instances"
weight: 4
description: "Add existing objects directly to the Pixie System"
---

You can add objects you've already created directly to the Pixie `System` using `system.add()`. This is useful for third-party objects, test doubles, or values computed at runtime that can't be expressed in a properties file.

## Adding by Name

```java
final System system = new System();

system.add("home", new Address("820 Roosevelt Street",
        "River Falls", State.WI, 54022, "USA"));

system.load(properties);
```

The added instance can be referenced by name from other components:

```properties
jane = new://org.example.Person
jane.age = 37
jane.address = @home
```

Here `@home` resolves to the `Address` instance added via `system.add("home", ...)`.

## Adding without a Name

```java
system.add(new Address("820 Roosevelt Street",
        "River Falls", State.WI, 54022, "USA"));
```

When added without a name, the instance can still be resolved by type. If a `@Component` parameter matches the type and no name is specified, Pixie will find it automatically.

## Mixing with Properties

Pre-built instances and properties-defined components work together seamlessly. You can add some objects manually and define others in properties:

```java
final System system = new System();

// Add a pre-built component
system.add("stripe", new StripeProcessor(apiKey));

// Load the rest from properties
system.load(properties);

// The ShoppingCart defined in properties can reference @stripe
final ShoppingCart cart = system.get(ShoppingCart.class);
```

## When to Use

- **Third-party libraries** — objects whose constructors you can't annotate
- **Runtime values** — API keys, database connections, or config computed at startup
- **Testing** — inject mocks or stubs without changing production code
- **Bridge code** — integrate Pixie with other frameworks by adding their managed objects
