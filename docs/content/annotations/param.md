---
title: "@Param"
weight: 1
description: "Bind constructor parameters to configuration properties"
---

**Purpose:** Binds a constructor parameter to a configuration property.

Pixie will automatically inject values from a properties file or the builder API.

## Example

```java
public final class User {
    private final String username;
    private final int age;

    public User(@Param("username") final String username,
                @Param("age") final int age) {
        this.username = username;
        this.age = age;
    }
}
```

This maps to a properties file entry:

```properties
user = new://org.example.User
user.username = alice
user.age = 30
```

## Supported Types

Any Java type that can be created from a `String` is supported. Pixie will inspect the Java class and look for one of the following:

- A public constructor with a single parameter of type `String`
- A public static method with a single parameter of type `String` returning an instance of the type

This means enums, `Integer`, `Long`, `Boolean`, `URI`, `File`, `Duration`, and many other standard Java types work out of the box.
