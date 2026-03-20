---
title: "References"
weight: 3
description: "How Pixie resolves @Component dependencies"
---

When a constructor parameter is annotated with `@Component`, Pixie resolves it by finding a matching component in the `System`. There are several ways this resolution can work:

| Resolution | How It Works | Properties Syntax |
|-----------|-------------|-------------------|
| **By Type** | Automatically find a component by type | *(omit the property)* |
| **By Name** | Reference a specific component by name | `cart.processor = @stripe` |
| **Collections** | Inject multiple components as a `List` | `helios.worlds = @earth @mars` or omit for all |
| **Generics** | Narrow matching using generic type arguments | *(automatic)* |

All resolution modes work with components from any [producer type]({{< relref "/producers" >}}) — constructors, `@Factory` methods, `@Builder` patterns, and pre-built instances via `system.add()`.
