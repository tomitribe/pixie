---
title: "Quick Example"
weight: 3
description: "Build your first Pixie system in minutes"
---

Let's walk through a complete example using Pixie to configure and build two classes: `Person` and `Address`.

## Define Your Classes

Annotate the constructor parameters with Pixie annotations. Pixie maps configuration to constructors based on the names you use.

```java
import org.tomitribe.pixie.Component;
import org.tomitribe.pixie.Default;
import org.tomitribe.pixie.Name;
import org.tomitribe.pixie.Param;

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

    // getters...
}

public class Address {
    private final String street;
    private final String city;
    private final State state;
    private final int zipcode;
    private final String country;

    public Address(@Param("street") final String street,
                   @Param("city") final String city,
                   @Param("state") final State state,
                   @Param("zipcode") final int zipcode,
                   @Param("country") @Default("USA") final String country) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipcode = zipcode;
        this.country = country;
    }
    // getters...
}

public enum State {
    WI, MN, CA;
}
```

## Configure with Properties

Create a properties file to define your objects and their relationships:

```properties
jon = new://org.example.Person
jon.age = 46
jon.address = @home

home = new://org.example.Address
home.street = 823 Roosevelt Street
home.city = River Falls
home.state = WI
home.zipcode = 54022
home.country = USA
```

Then build a Pixie `System` and look up your objects:

```java
import java.util.Properties;
import org.example.Person;
import org.tomitribe.pixie.System;

public static void main(String[] args) {

    final Properties properties = new Properties();
    properties.load(...); // read the properties file

    final System system = new System(properties);

    final Person person = system.get(Person.class);
    assertEquals("jon", person.getName());

    final Address address = person.getAddress();
    assertNotNull(address);
    assertEquals("823 Roosevelt Street", address.getStreet());
}
```

## Configure with the Builder API

Alternatively, skip the properties file and build your `System` in code:

```java
final System system = System.builder()

        .definition(Person.class, "jane")
        .param("age", 37)
        .comp("address", "home")

        .definition(Address.class, "home")
        .param("street", "820 Roosevelt Street")
        .param("city", "River Falls")
        .param("state", "WI")
        .param("zipcode", "54022")

        .build();

final Person person = system.get(Person.class);
```

Both approaches produce the same result — fully configured, wired object graphs.
