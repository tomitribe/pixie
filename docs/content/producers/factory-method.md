---
title: "Factory Method"
weight: 2
description: "Create components via a @Factory static method"
---

A `@Factory` method is a public static method that Pixie calls to create the component instance. Factory method parameters are annotated with the same Pixie annotations as constructors — `@Param`, `@Component`, `@Name`, `@Event`, etc.

This is useful when the constructor is private, when you need validation logic before construction, or when you're producing instances of a class you don't own.

## Factory in the Same Class

The `@Factory` method can live on the class it produces. The constructor can be private since Pixie never calls it directly:

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

    @Factory
    public static Person create(@Name final String name,
                                @Param("age") @Nullable final Integer age,
                                @Param("address") @Component final Address address) {
        return new Person(name, age, address);
    }
}
```

Configuration is the same as constructor-based components:

```properties
jane = new://org.example.Person
jane.age = 37
jane.address = @home
```

## Factory in a Separate Class

The `@Factory` method can also live in a completely different class. In this case, the definition references the factory class:

```java
public class PersonFactory {

    @Factory
    public static Person create(@Name final String name,
                                @Param("age") @Nullable final Integer age,
                                @Param("address") @Component final Address address) {
        return new Person(name, age, address);
    }
}
```

The properties reference the factory class instead:

```properties
jane = new://org.example.PersonFactory
jane.age = 37
jane.address = @home
```

And with the Builder API:

```java
final System system = System.builder()
        .definition(PersonFactory.class, "jane")
        .param("age", 37)
        .comp("address", "home")
        // ...
        .build();
```

Pixie will call the `@Factory` method and register the returned `Person` instance. The component is looked up by the return type of the factory method, not the factory class itself:

```java
final Person jane = system.get(Person.class);
```

## Rules

- The method must be **public** and **static**
- The method must be annotated with `@Factory`
- All parameters must be annotated with Pixie annotations
- The method name does not matter
- If a class has both a `@Factory` method and a constructor, the `@Factory` method takes priority
