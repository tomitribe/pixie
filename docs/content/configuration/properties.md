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

## Declaration Order

The order in which components are declared in the properties file is preserved. This matters for [by-type resolution]({{< relref "/references/by-type" >}}) — when multiple components match a `@Component` parameter by type, the first declared match is selected.

This lets you establish a preferred default simply by listing it first:

```properties
# StripeProcessor is the default PaymentProcessor
stripe = new://org.example.StripeProcessor
stripe.apiKey = sk_live_abc123

paypal = new://org.example.PaypalProcessor
paypal.clientId = AaBb123

# Gets StripeProcessor automatically (first match by type)
orderCart = new://org.example.ShoppingCart

# Explicitly overrides to PaypalProcessor
donationCart = new://org.example.ShoppingCart
donationCart.processor = @paypal
```

## Case Insensitivity

All property keys are **case insensitive** — component names, param names, and `@` references are all matched without regard to case.

For `@Param` string values, these are equivalent:

```properties
jane.age = 37
Jane.Age = 37
JANE.AGE = 37
```

For `@Param` `@Component` references, the same applies to both the property key and the `@` reference:

```properties
jane.address = @home
Jane.Address = @Home
JANE.ADDRESS = @HOME
```

This prevents failures due to inconsistent capitalization between your properties file and your annotation values.

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
