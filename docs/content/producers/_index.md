---
title: "Producers"
weight: 4
description: "Four ways to get objects into the Pixie System"
---

Pixie supports four ways to create and register components in the `System`. Each approach gives you a different level of control over how objects are constructed.

| Producer | How It Works | When To Use |
|----------|-------------|-------------|
| **Constructor** | Pixie calls an annotated constructor directly | Simple classes you own |
| **Factory Method** | A `@Factory` static method creates the instance | Private constructors, validation, or classes you don't own |
| **Builder** | A `@Builder` static method returns a builder with annotated setters | Complex construction with many optional parameters |
| **Pre-built Instance** | You create the object and hand it to `system.add()` | Third-party objects, test doubles, or runtime-computed values |

When a class has both a `@Factory` method and a `@Builder` method, Pixie will prefer the `@Factory`. If neither annotation is present, Pixie falls back to the constructor.
