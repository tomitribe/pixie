---
title: "Constructor"
weight: 1
description: "Create components by calling an annotated constructor"
---

The most common way to create components in Pixie. Annotate a public constructor's parameters and Pixie will call it directly.

## Example

```java
public class Person {

    private final String name;
    private final Integer age;
    private final Address address;

    public Person(@Name final String name,
                  @Param("age") final Integer age,
                  @Param("address") @Component final Address address) {
        this.name = name;
        this.age = age;
        this.address = address;
    }
}
```

Configure via properties:

```properties
jane = new://org.example.Person
jane.age = 37
jane.address = @home
```

Or via the Builder API:

```java
final System system = System.builder()
        .definition(Person.class, "jane")
        .param("age", 37)
        .comp("address", "home")
        // ...
        .build();
```

## Rules

- The constructor must be **public**
- Every parameter must be annotated with `@Param`, `@Component`, `@Name`, or `@Event`
- You cannot mix annotated and unannotated parameters in the same constructor
- If a class has multiple constructors, Pixie uses the one that is fully annotated
