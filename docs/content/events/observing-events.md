---
title: "Observing Events"
weight: 2
description: "Use @Observes to listen for events"
---

The `@Observes` annotation marks a method as an event listener. When a matching event is fired, Pixie calls the method automatically.

## Example

```java
public class EmailReceipt {
    public void onOrderProcessed(@Observes final OrderProcessed event) {
        sendEmail(event.getId());
    }
}

public class LogOrder {
    public void onOrderProcessed(@Observes final OrderProcessed event) {
        log.info("Order processed: " + event.getId());
    }
}
```

When an `OrderProcessed` event is fired, **both** methods are called — every matching observer in the system is notified.

## Multiple Observers on One Component

A single component can observe multiple event types:

```java
public class AuditLog {
    public void onOrder(@Observes final OrderProcessed event) {
        log("Order: " + event.getId());
    }

    public void onRefund(@Observes final RefundIssued event) {
        log("Refund: " + event.getId());
    }
}
```

## Listening by Supertype

Observation is polymorphic — an observer matches any event that is **assignable** to its parameter type. This follows the same rules as Java's `instanceof`:

```java
// Receives OrderProcessed and any subclass (e.g., StripeOrderProcessed)
public void onOrder(@Observes final OrderProcessed event) { ... }

// Receives every event in the system
public void onAny(@Observes final Object event) { ... }
```

When both a supertype and a subtype observer exist, the **most specific** match is called. If you observe `Integer` and `Number`, firing an `Integer` event calls only the `Integer` observer — not both:

```java
public class Listener {
    // Called for Integer events
    public void onInteger(@Observes final Integer event) { ... }

    // Called for Long, Double, etc. — but NOT Integer
    public void onNumber(@Observes final Number event) { ... }
}
```

## Exception Handling

Observer exceptions are caught and do not propagate to the event producer. Instead, Pixie fires an `ObserverFailed` event that you can observe to handle errors:

```java
public class ErrorHandler {
    public void onFailure(@Observes final ObserverFailed event) {
        log.error("Observer " + event.getMethod() + " failed", event.getThrowable());
    }
}
```

This ensures that one failing observer does not prevent other observers from receiving the event.
