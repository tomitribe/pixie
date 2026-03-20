---
title: "Firing Events"
weight: 1
description: "Use @Event to fire events from components"
---

The `@Event` annotation injects a `Consumer<T>` into a component so it can fire events.

## Example

```java
public class ShoppingCart {
    private final Consumer<OrderProcessed> orderProcessedEvent;

    public ShoppingCart(@Event final Consumer<OrderProcessed> orderProcessedEvent) {
        this.orderProcessedEvent = orderProcessedEvent;
    }

    public void order(final String orderId) {
        // Fire the event — all observers are notified
        orderProcessedEvent.accept(new OrderProcessed(orderId));
    }
}
```

Pixie injects a `Consumer<OrderProcessed>` that, when called, dispatches the event to all `@Observes` methods in the system that match the event type.

## Event Types

The event object can be any Java type — a class you define, a standard library class, a primitive wrapper, or anything else. There are no marker interfaces or base classes required:

```java
public class OrderProcessed {
    private final String id;

    public OrderProcessed(final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
```

## Multiple Event Types

A component can inject multiple event consumers for different event types:

```java
public class OrderService {
    private final Consumer<OrderPlaced> orderPlaced;
    private final Consumer<OrderShipped> orderShipped;

    public OrderService(@Event final Consumer<OrderPlaced> orderPlaced,
                        @Event final Consumer<OrderShipped> orderShipped) {
        this.orderPlaced = orderPlaced;
        this.orderShipped = orderShipped;
    }
}
```

## Firing from Outside Components

You can also fire events directly on the `System` without using `@Event`:

```java
system.fireEvent(new OrderProcessed("order123"));
```

This is useful when integrating with external systems or firing events from test code.
