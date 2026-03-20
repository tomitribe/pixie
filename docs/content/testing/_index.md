---
title: "Testing"
weight: 6
description: "Test Pixie components with or without the framework"
---

Because Pixie uses constructor injection with no proxies and no field injection, every component you write can be tested in plain Java. You can also use `System.builder()` for integration-style tests that exercise the full wiring. Both approaches are valid and complement each other.
