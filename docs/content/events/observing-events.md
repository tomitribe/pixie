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

## Listening by Supertype or Interface

Observation is polymorphic — an observer matches any event that is **assignable** to its parameter type. This follows the same rules as Java's `instanceof`, and the parameter may be a class **or an interface**:

```java
// Receives OrderProcessed and any subclass (e.g., StripeOrderProcessed)
public void onOrder(@Observes final OrderProcessed event) { ... }

// Receives any event whose type implements Auditable
public void onAuditable(@Observes final Auditable event) { ... }

// Receives every event in the system
public void onAny(@Observes final Object event) { ... }
```

When more than one observer method on the same component matches, the **most specific** one is called — exactly as Java resolves an overloaded method call. "Most specific" follows the subtype relation across the full type graph, classes and interfaces alike. If you observe `Integer` and `Number`, firing an `Integer` calls only the `Integer` observer:

```java
public class Listener {
    // Called for Integer events
    public void onInteger(@Observes final Integer event) { ... }

    // Called for Long, Double, etc. — but NOT Integer
    public void onNumber(@Observes final Number event) { ... }
}
```

Pixie deliberately mirrors Java overload resolution and invokes the best-matching `@Observes` method per JVM rules. The more general matches are not called — in the example above, an `Integer` event never reaches `onNumber`. This applies per component: every component with a matching observer is notified, and each contributes exactly one call — its best match.

## Ambiguous Matches

Most-specific selection needs a *single* most-specific match. If a component observes two unrelated interfaces and an event implements **both**, with no common subtype to break the tie, there is no most-specific match. Rather than guess, Pixie throws `AmbiguousObserverException` when the event is fired — the same stance the Java compiler takes on an ambiguous overloaded call:

```java
public interface Audited {}
public interface Billable {}
public class Invoice implements Audited, Billable {}

public class Handler {
    public void onAudited(@Observes final Audited event)  { ... }
    public void onBillable(@Observes final Billable event) { ... } // tie!
}
// Firing an Invoice throws AmbiguousObserverException
```

To resolve it, give the resolver a unique winner — observe a type the event shares as a common subtype (e.g. `Invoice` itself, or an interface that extends both), or remove one of the observers.

## One Observer Per Type

Within a single component you may declare at most one `@Observes` method for a given event type. Two methods observing the same type is a configuration error, and Pixie rejects the component when it is registered — rather than silently dropping one:

```java
public class Bad {
    public void onOrder(@Observes final OrderProcessed event)  { ... }
    public void logOrder(@Observes final OrderProcessed event) { ... } // rejected
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
