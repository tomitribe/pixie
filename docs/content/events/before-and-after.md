---
title: "Before & After"
weight: 3
description: "Observe events before or after they are dispatched"
---

Pixie wraps every event dispatch in `BeforeEvent<T>` and `AfterEvent<T>` wrappers, allowing you to hook into the event lifecycle.

## BeforeEvent

`BeforeEvent<T>` is fired **before** the actual event is dispatched to its observers:

```java
public class SecurityCheck {
    public void beforeOrder(@Observes final BeforeEvent<OrderProcessed> event) {
        // Runs before any @Observes OrderProcessed methods
        validatePermissions(event.getEvent());
    }
}
```

## AfterEvent

`AfterEvent<T>` is fired **after** all observers of the actual event have been called:

```java
public class Metrics {
    public void afterOrder(@Observes final AfterEvent<OrderProcessed> event) {
        // Runs after all @Observes OrderProcessed methods
        recordMetric("order.processed");
    }
}
```

## Execution Order

For a single event, the order is:

1. `BeforeEvent<T>` observers
2. `T` observers (the event itself)
3. `AfterEvent<T>` observers

## Type Matching

`BeforeEvent` and `AfterEvent` use the generic type argument for matching, so there are no false positives:

```java
// Only fires before Integer events — not before Date events
public void beforeInt(@Observes final BeforeEvent<Integer> event) { ... }

// Only fires after Date events — not after Integer events
public void afterDate(@Observes final AfterEvent<Date> event) { ... }
```

Supertype matching works too — `BeforeEvent<Number>` will fire before any `Integer`, `Long`, or `Double` event. `AfterEvent<Object>` fires after every event.
