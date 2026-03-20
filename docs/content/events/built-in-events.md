---
title: "Built-in Events"
weight: 4
description: "Lifecycle and diagnostic events fired by the Pixie System"
---

Pixie fires several built-in events during the lifecycle of the `System`. All event classes are in the `org.tomitribe.pixie.event` package.

## Lifecycle Events

| Event | Fired When |
|-------|-----------|
| `PixieLoad` | After `system.load(properties)` completes. Contains the loaded `Properties`. |
| `PixieClose` | When `system.close()` is called. |

```java
public class StartupListener {
    public void onLoad(@Observes final PixieLoad event) {
        log.info("System loaded with " + event.getProperties().size() + " properties");
    }

    public void onClose(@Observes final PixieClose event) {
        log.info("System shutting down");
    }
}
```

`System` implements `Closeable`, so it works with try-with-resources:

```java
try (final System system = new System(properties)) {
    // PixieLoad fires after construction
    // ...
}   // PixieClose fires here
```

## Component Events

| Event | Fired When |
|-------|-----------|
| `ComponentAdded<T>` | A component is added to the System. Contains the type and instance. |
| `ComponentRemoved<T>` | A component is removed from the System. Contains the type and instance. |

```java
public class ComponentTracker {
    public void onAdded(@Observes final ComponentAdded<?> event) {
        log.info("Added: " + event.getType().getSimpleName());
    }
}
```

## Observer Events

| Event | Fired When |
|-------|-----------|
| `ObserverAdded` | An observer is registered with the System. Contains the observer instance. |
| `ObserverRemoved` | An observer is unregistered. Contains the observer instance. |

## Dispatch Events

| Event | Fired When |
|-------|-----------|
| `BeforeEvent<T>` | Before an event of type `T` is dispatched. See [Before & After]({{< relref "/events/before-and-after" >}}). |
| `AfterEvent<T>` | After an event of type `T` has been dispatched. See [Before & After]({{< relref "/events/before-and-after" >}}). |

## Error Events

| Event | Fired When |
|-------|-----------|
| `ObserverFailed` | An observer method threw an exception. Contains the observer, method, original event, and throwable. |
| `ObserverNotFound` | No observers exist for a fired event. Contains the original event. |

```java
public class ErrorHandler {
    public void onFailure(@Observes final ObserverFailed event) {
        log.error("Observer " + event.getMethod().getName() + " failed: "
                + event.getThrowable().getMessage());
    }

    public void onMissing(@Observes final ObserverNotFound event) {
        log.warn("No observers for: " + event.getEvent().getClass().getSimpleName());
    }
}
```

Observer exceptions are caught by Pixie — they do not propagate to the event producer and do not prevent other observers from being called.
