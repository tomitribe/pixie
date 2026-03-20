---
title: "By Name"
weight: 2
description: "Reference a specific component by name"
---

The most explicit way to resolve a `@Component` is by name. Set the property value to `@` followed by the component name:

```properties
cart = new://org.example.ShoppingCart
cart.processor = @stripe

stripe = new://org.example.StripeProcessor
stripe.apiKey = sk_live_abc123
```

```java
public class ShoppingCart {
    public ShoppingCart(@Param("processor") @Component
                        final PaymentProcessor processor) {
        // Pixie injects the component named "stripe"
    }
}
```

Pixie looks in the `System` for a component with the name `stripe` and injects it.

## When to Use Named References

[By-type resolution]({{< relref "/references/by-type" >}}) is convenient when there's only one component of a given type. Named references become necessary when the system has multiple candidates and you need to pick a specific one.

### Multiple Implementations

When several components implement the same interface, a named reference selects the one you want:

```properties
orderCart = new://org.example.ShoppingCart
orderCart.processor = @stripe

donationCart = new://org.example.ShoppingCart
donationCart.processor = @paypal

stripe = new://org.example.StripeProcessor
stripe.apiKey = sk_live_abc123

paypal = new://org.example.PaypalProcessor
paypal.clientId = AaBb123
```

Without named references, both carts would get the same processor via by-type resolution. Named references let each cart use a different one.

### Same Type, Different Configuration

Components don't need to be different types to warrant names. You might have multiple instances of the same class, each configured differently:

```properties
app = new://org.example.App
app.primary = @usEast
app.fallback = @euWest

usEast = new://org.example.DataSource
usEast.url = jdbc:postgresql://us-east-1.example.com/prod
usEast.maxConnections = 50

euWest = new://org.example.DataSource
euWest.url = jdbc:postgresql://eu-west-1.example.com/prod
euWest.maxConnections = 20
```

```java
public class App {
    public App(@Param("primary") @Component final DataSource primary,
               @Param("fallback") @Component final DataSource fallback) {
        // two DataSource instances, each with different config
    }
}
```

Here by-type resolution can't work — there are two `DataSource` instances and Pixie wouldn't know which goes where. Named references make the wiring explicit.

## Pre-built Instances

Named references also resolve components added directly via `system.add()`:

```java
final System system = new System();
system.add("home", new Address("820 Roosevelt Street",
        "River Falls", State.WI, 54022, "USA"));
system.load(properties);
```

```properties
jane = new://org.example.Person
jane.address = @home
```

The `@home` reference resolves to the `Address` instance added via `system.add("home", ...)`.

## Case Insensitivity

Component names and `@` references are case insensitive. The following are all equivalent:

```properties
jane.address = @home
jane.address = @Home
jane.address = @HOME
```

This applies to component names, property keys, and `@Param` names throughout Pixie.

## Error Conditions

A `ConstructionFailedException` is thrown if:

- No component with that name exists
- The named component is not assignable to the parameter type
