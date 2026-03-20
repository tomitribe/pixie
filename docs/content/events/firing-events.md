---
title: "Firing Events"
weight: 1
description: "Use @Event to fire events from components"
---

**Purpose:** The `@Event` annotation injects a `Consumer<T>` into a component so it can fire events.

## Example

```java
public final class OrderService {
    private final Consumer<OrderPlaced> orderPlacedEvent;

    public OrderService(@Event final Consumer<OrderPlaced> orderPlacedEvent) {
        this.orderPlacedEvent = orderPlacedEvent;
    }

    public void placeOrder(final String orderId) {
        orderPlacedEvent.accept(new OrderPlaced(orderId));
    }
}
```

Pixie will inject a `Consumer<OrderPlaced>` that calls `System.fire(event)`, which will invoke all observer methods in all components in the `System`.

The event object can be any Java type — there are no interfaces to implement or base classes to extend.
