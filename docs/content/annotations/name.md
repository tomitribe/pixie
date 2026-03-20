---
title: "@Name"
weight: 5
description: "Inject the component's name from configuration"
---

**Purpose:** Injects the component's name from the configuration.

## Example

```java
public final class Service {
    private final String serviceName;

    public Service(@Name final String serviceName) {
        this.serviceName = serviceName;
    }
}
```

If configured as:

```properties
myService = new://com.foo.Service
```

The constructor will receive `"myService"` as the value of `serviceName`.

This is useful when a component needs to know its own identity — for example, to use its name in logging, metrics, or self-registration.
