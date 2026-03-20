---
title: "Builder Pattern"
weight: 3
description: "Create components via a @Builder static method and annotated setters"
---

A `@Builder` method returns a builder object whose setter methods are annotated with Pixie annotations. Pixie calls the `@Builder` method, invokes each annotated setter, then calls `build()` to produce the final instance.

This is ideal for classes with many optional parameters or complex construction logic.

## Builder in the Same Class

```java
public class Person {

    private final String name;
    private final Integer age;
    private final Address address;

    private Person(final String name,
                   final Integer age,
                   final Address address) {
        this.name = name;
        this.age = age;
        this.address = address;
    }

    @Builder
    public static PersonBuilder builder() {
        return new PersonBuilder();
    }

    public static class PersonBuilder {

        private String name;
        private Integer age;
        private Address address;

        public PersonBuilder name(@Name final String name) {
            this.name = name;
            return this;
        }

        public PersonBuilder age(@Param("age") @Nullable final Integer age) {
            this.age = age;
            return this;
        }

        public PersonBuilder address(@Param("address") @Component final Address address) {
            this.address = address;
            return this;
        }

        public Person build() {
            return new Person(name, age, address);
        }
    }
}
```

Configuration is the same as any other producer:

```properties
jane = new://org.example.Person
jane.age = 37
jane.address = @home
```

## Builder in a Separate Class

The builder class and its `@Builder` method can live in a completely different class than the component being built:

```java
public class PersonBuilder {

    private String name;
    private Integer age;
    private Address address;

    public PersonBuilder name(@Name final String name) {
        this.name = name;
        return this;
    }

    public PersonBuilder age(@Param("age") @Nullable final Integer age) {
        this.age = age;
        return this;
    }

    public PersonBuilder address(@Param("address") @Component final Address address) {
        this.address = address;
        return this;
    }

    public Person build() {
        return new Person(name, age, address);
    }

    @Builder
    public static PersonBuilder builder() {
        return new PersonBuilder();
    }
}
```

The definition references the builder class:

```properties
jane = new://org.example.PersonBuilder
jane.age = 37
jane.address = @home
```

Pixie registers the object returned by `build()`. The component is looked up by the return type of `build()`:

```java
final Person jane = system.get(Person.class);
```

## How It Works

1. Pixie calls the static `@Builder` method to get a builder instance
2. For each setter method with an annotated parameter, Pixie resolves the value and calls the setter
3. Pixie calls `build()` on the builder to produce the final component

## Rules

- The `@Builder` method must be **public** and **static**
- The builder class must have a public `build()` method with no parameters
- Each setter method has exactly **one** annotated parameter
- Setter method names do not matter — Pixie matches by annotation, not by name
- If a class has both a `@Factory` and a `@Builder`, the `@Factory` takes priority
