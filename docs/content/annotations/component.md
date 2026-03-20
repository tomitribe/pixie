---
title: "@Component"
weight: 3
description: "Inject dependent objects from the Pixie System"
---

**Purpose:** Indicates that a constructor parameter should be injected as a component dependency.

## Example

```java
public class ShoppingCart {
    public ShoppingCart(@Param("processor") @Component
                        final PaymentProcessor processor) {
        // ...
    }
}
```

Pixie resolves `@Component` references in several ways:

- [**By Name**]({{< relref "/references/by-name" >}}) — `cart.processor = @stripe` references a specific component
- [**By Type**]({{< relref "/references/by-type" >}}) — omit the property and Pixie finds a match automatically
- [**Collections**]({{< relref "/references/collections" >}}) — use `List<T>` to inject multiple components
- [**Generics**]({{< relref "/references/generics" >}}) — generic type arguments narrow which components match

See the [References]({{< relref "/references" >}}) section for full details on each resolution mode.
