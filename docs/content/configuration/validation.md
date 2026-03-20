---
title: "Validation"
weight: 3
description: "Strict configuration validation and case insensitivity"
---

Pixie provides strict validation to ensure configuration correctness and prevent common issues with properties files.

## Case Insensitivity

All properties in Pixie are **case insensitive**, meaning users will not encounter failures due to incorrect capitalization. The following entries are treated as equivalent:

```properties
user.name = Alice
User.Name = Alice
USER.NAME = Alice
```

## Strict Property Validation

Pixie enforces strict validation of configuration properties to prevent misconfigurations:

- If a property is **specified in the configuration file but does not exist in the corresponding class**, Pixie will **throw an exception** at startup
- This ensures that typos or removed properties do not lead to silent failures

For example, given the following properties file:

```properties
app.mode = production
app.timeout = 5000
```

If the `app.timeout` property is removed from the Java class but remains in the configuration file, Pixie will **fail fast** with an error, preventing users from relying on "dead" properties.
