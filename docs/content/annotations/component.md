---
title: "@Component"
weight: 3
description: "Inject dependent objects from the Pixie System"
---

**Purpose:** Indicates that a constructor parameter should be injected as a component dependency.

## Example

```java
public final class ShoppingCart {
    private final PaymentProcessor paymentProcessor;

    public ShoppingCart(@Param("processor") @Component
                        final PaymentProcessor paymentProcessor) {
        this.paymentProcessor = paymentProcessor;
    }
}
```

Pixie can resolve this reference by name or by type.

## Resolution by Name

```properties
cart = new://org.example.ShoppingCart
cart.processor = @stripe
```

With the above configuration, Pixie will look in the `System` for an object with the name `stripe` and inject it as the value of `processor` when constructing the `ShoppingCart`.

A `ConstructionFailedException` will be thrown if any of the following occurs:

- No `@Param` was provided
- No object with that name is found
- The object found is of the wrong type

## Resolution by Type

```properties
cart = new://org.example.ShoppingCart
```

When the `processor` name has not been specified, Pixie will look in the `System` for any object of type `PaymentProcessor` and inject it automatically.

If there are **multiple instances** of `PaymentProcessor`, they will be sorted in descending order by name and the first will be picked.

A `ConstructionFailedException` will be thrown if no objects of that type are found.

## Adding Custom Components

The `@Component` annotation can resolve components that are added directly to the Pixie `System`:

```properties
jane = new://org.example.Person
jane.age = 37
jane.address = @home
```

In the above properties, the `Person` object has a `@Component` reference to `Address` called `home` which is not defined in the properties. The `home` instance can be added directly:

```java
final Properties properties = // ...

final System system = new System();
system.add("home", new Address("820 Roosevelt Street",
        "River Falls", State.WI, 54022, "USA"));
system.load(properties);

final Person person = system.get(Person.class);
assertNotNull(person.getAddress());
```
