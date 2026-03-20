---
title: "Observing Events"
weight: 2
description: "Use @Observes to listen for events"
---

**Purpose:** The `@Observes` annotation marks a method as an event listener.

## Example

```java
public final class OrderListener {
    public void onOrderPlaced(@Observes final OrderPlaced event) {
        System.out.println("Order placed: " + event.getOrderId());
    }
}
```

When an `OrderPlaced` event is fired, this method will be called automatically.

## Listening by Supertype

It is possible to listen for events by any assignable type, including `java.lang.Object`:

```java
public final class EverythingListener {
    public void onEvent(@Observes final Object event) {
        System.out.println("Event observed: " + event);
    }
}
```

This listener will receive every event fired in the system, regardless of type.
