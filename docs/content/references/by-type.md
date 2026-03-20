---
title: "By Type"
weight: 1
description: "Automatically resolve a component by type"
---

When a `@Component` property is not specified in the configuration, Pixie resolves it automatically by finding a component that matches the parameter type:

```properties
cart = new://org.example.ShoppingCart
```

```java
public class ShoppingCart {
    public ShoppingCart(@Param("processor") @Component
                        final PaymentProcessor processor) {
        // Pixie finds a PaymentProcessor in the System automatically
    }
}
```

Pixie searches the `System` for any component assignable to `PaymentProcessor` and injects it. No `@` reference is needed in the properties — Pixie handles the wiring for you.

## What Matches

A component is eligible if it is **assignable** to the parameter type — the same rule as Java's `instanceof`. This means the parameter type does not need to be the concrete class. Pixie will match:

- **The exact class** — a `StripeProcessor` parameter matches a `StripeProcessor` component
- **An interface** — a `PaymentProcessor` parameter matches any component that implements `PaymentProcessor`
- **A superclass** — an `AbstractProcessor` parameter matches any component that extends `AbstractProcessor`
- **`Object`** — matches any component (though rarely useful for single injection)

```java
public interface PaymentProcessor { ... }
public class StripeProcessor implements PaymentProcessor { ... }
public class PaypalProcessor implements PaymentProcessor { ... }
```

```java
// Matches StripeProcessor, PaypalProcessor, or any other PaymentProcessor
@Param("processor") @Component final PaymentProcessor processor
```

This makes it easy to program against interfaces. Define your parameter as an interface type, and Pixie will find whichever implementation is configured in the system — without your code needing to know the concrete class.

## Multiple Matches

If there are **multiple** components assignable to the parameter type, Pixie selects the first match based on the order components were declared in the properties file. This order is preserved internally, so you can control which implementation is preferred by listing it first:

```properties
# StripeProcessor will be the default PaymentProcessor
# because it appears first
stripe = new://org.example.StripeProcessor
stripe.apiKey = sk_live_abc123

paypal = new://org.example.PaypalProcessor
paypal.clientId = AaBb123
```

Any component that resolves by type will get `StripeProcessor`. You only need a [named reference]({{< relref "/references/by-name" >}}) for the cases where you want a different one:

```properties
# Uses StripeProcessor by default (first declared)
orderCart = new://org.example.ShoppingCart

# Explicitly overrides to PaypalProcessor
donationCart = new://org.example.ShoppingCart
donationCart.processor = @paypal
```

This "preferred first, names by exception" pattern keeps your configuration concise — declare your default implementation at the top and only use `@` references where you need to diverge.

If you need **all** matching components rather than just one, use a [collection]({{< relref "/references/collections" >}}) parameter.

## No Matches

A `ConstructionFailedException` is thrown if no component of the matching type is found.

## Generic Type Narrowing

When the parameter includes generic type arguments, Pixie uses them to further narrow the match. A `RequestHandler<String, Integer>` parameter will only match `RequestHandler` implementations with those exact type arguments. See [Generics]({{< relref "/references/generics" >}}) for details.
