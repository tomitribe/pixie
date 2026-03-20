---
title: "Plain Java"
weight: 1
description: "Test components without Pixie using plain constructors"
---

Every Pixie component can be instantiated with `new` — no framework required. This makes unit testing straightforward.

## Simple Components

```java
@Test
public void testAddress() {
    final Address address = new Address(
            "820 Roosevelt Street",
            "River Falls",
            State.WI,
            54022,
            "USA"
    );

    assertEquals("River Falls", address.getCity());
    assertEquals(State.WI, address.getState());
}
```

## Components with Dependencies

Pass dependencies directly through the constructor, just as Pixie would:

```java
@Test
public void testPerson() {
    final Address home = new Address("820 Roosevelt Street",
            "River Falls", State.WI, 54022, "USA");

    final Person person = new Person("jane", 37, home);

    assertEquals("jane", person.getName());
    assertEquals(37, person.getAge());
    assertEquals("River Falls", person.getAddress().getCity());
}
```

## Testing Events with a Consumer

Components that fire events via `@Event` take a `Consumer<T>` in their constructor. In a test, pass your own `Consumer` to capture and verify the events:

```java
@Test
public void testOrderFiresEvent() {
    final List<OrderProcessed> firedEvents = new ArrayList<>();

    // Pass a lambda as the event consumer
    final ShoppingCart cart = new ShoppingCart(firedEvents::add);

    cart.order("order-123");

    assertEquals(1, firedEvents.size());
    assertEquals("order-123", firedEvents.get(0).getId());
}
```

No mock framework needed — a `Consumer` is a standard functional interface. An `ArrayList::add` captures everything.

This works because `@Event Consumer<OrderProcessed>` is just a constructor parameter. Pixie injects one that dispatches to the system's observers, but in a test you supply whatever you want:

```java
// Verify the event is fired
final ShoppingCart cart = new ShoppingCart(firedEvents::add);

// Verify nothing blows up if the consumer does nothing
final ShoppingCart cart = new ShoppingCart(event -> {});

// Verify behavior on consumer failure
final ShoppingCart cart = new ShoppingCart(event -> {
    throw new RuntimeException("simulated failure");
});
```

## Testing Observers

Observer methods are just regular methods. Call them directly:

```java
@Test
public void testEmailReceipt() {
    final EmailReceipt receipt = new EmailReceipt();

    receipt.onOrderProcessed(new OrderProcessed("order-456"));

    assertEquals(1, receipt.getOrdersProcessed().size());
    assertEquals("order-456", receipt.getOrdersProcessed().get(0).getId());
}
```

No need to fire events through a `System` — the `@Observes` annotation is only used by Pixie for discovery. The method itself is plain Java.
