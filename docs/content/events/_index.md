---
title: "Events"
weight: 5
description: "Fire and observe events across components"
---

Pixie includes a lightweight event system that lets components communicate without direct dependencies. A component fires an event, and any component with a matching `@Observes` method receives it — no wiring, no registration, no interfaces required.

The event object can be any Java type. There are no interfaces to implement or base classes to extend.
