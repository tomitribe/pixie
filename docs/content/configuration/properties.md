---
title: "Properties Format"
weight: 1
description: "Define components using properties files"
---

Pixie can build an entire object graph from a `java.util.Properties` instance. The file name and location can be anything — it's your job to read the file into a `Properties` object.

## Format

Each component is defined with a `new://` declaration, followed by its properties:

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

- `new://fully.qualified.ClassName` declares a new component
- `componentName.propertyName = value` sets a `@Param` value
- `@referenceName` references another component (for `@Component` parameters)

## Loading Properties

```java
final Properties properties = new Properties();
properties.load(...); // read the properties file

final System system = new System(properties);

final Person person = system.get(Person.class);
```

## Adding Pre-built Instances

You can mix properties-defined components with manually added instances:

```java
final System system = new System();
system.add("home", new Address("820 Roosevelt Street",
        "River Falls", State.WI, 54022, "USA"));
system.load(properties);

final Person person = system.get(Person.class);
```
