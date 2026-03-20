---
title: "Generics"
weight: 5
description: "Generic type matching for precise component resolution"
---

When a `@Component` parameter uses generic type arguments, Pixie narrows matching to only those components whose resolved generic types are compatible. This works for interfaces, superclasses, and any depth of inheritance.

If the parameter is a raw type (no generics), all implementations are eligible — this is fully backwards compatible.
