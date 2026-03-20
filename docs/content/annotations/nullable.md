---
title: "@Nullable"
weight: 4
description: "Mark parameters as optional"
---

**Purpose:** Marks a constructor parameter as optional — it can be `null` if not configured.

## Example

```java
public final class Notification {
    private final String message;
    private final String footer;

    public Notification(@Param("message") final String message,
                        @Nullable @Param("footer") final String footer) {
        this.message = message;
        this.footer = footer;
    }
}
```

If `footer` is missing from the config, it will be `null` instead of throwing an error.

Without `@Nullable`, a missing property would cause a `ConstructionFailedException` at startup.
