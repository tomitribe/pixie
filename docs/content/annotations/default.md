---
title: "@Default"
weight: 2
description: "Provide fallback values for missing properties"
---

**Purpose:** Specifies a default value for a constructor parameter if it is not set in the configuration.

## Example

```java
public final class Address {
    private final String country;

    public Address(@Param("country") @Default("USA") final String country) {
        this.country = country;
    }
}
```

If `country` is missing from the config, `"USA"` is used.

## With @Component

`@Default` also applies to `@Component` parameters. When used on `@Component`, it implies the name of the component that should be injected:

```java
public final class OrderService {
    private final PaymentProcessor processor;

    public OrderService(@Param("processor") @Component
                        @Default("stripe") final PaymentProcessor processor) {
        this.processor = processor;
    }
}
```

If `processor` is not specified in the config, Pixie will look for a component named `stripe`.
