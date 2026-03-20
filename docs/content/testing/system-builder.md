---
title: "System.builder()"
weight: 2
description: "Integration tests using the Pixie System"
---

For integration-style tests that exercise the full Pixie wiring — component resolution, event dispatch, and the complete object graph — use `System.builder()`.

## Basic Wiring Test

```java
@Test
public void testFullSystem() {
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

    final Person jane = system.get(Person.class);

    assertEquals("jane", jane.getName());
    assertEquals(37, jane.getAge());
    assertEquals("River Falls", jane.getAddress().getCity());
}
```

## Testing Event Wiring

`System.builder()` wires `@Event` consumers and `@Observes` methods together automatically. This lets you verify that firing an event in one component reaches observers in another:

```java
@Test
public void testEventFlow() {
    final System system = System.builder()
            .definition(ShoppingCart.class)
            .definition(EmailReceipt.class)
            .definition(LogOrder.class)
            .build();

    final ShoppingCart cart = system.get(ShoppingCart.class);
    final EmailReceipt email = system.get(EmailReceipt.class);
    final LogOrder log = system.get(LogOrder.class);

    cart.order("order-101");

    assertEquals(1, email.getOrdersProcessed().size());
    assertEquals("order-101", email.getOrdersProcessed().get(0).getId());

    assertEquals(1, log.getOrdersProcessed().size());
    assertEquals("order-101", log.getOrdersProcessed().get(0).getId());
}
```

## Injecting Test Doubles

Use `system.add()` to substitute pre-built instances — mocks, stubs, or test-specific implementations — into the object graph:

```java
@Test
public void testWithMockProcessor() {
    final List<String> charged = new ArrayList<>();

    final System system = System.builder()
            .add("stripe", (PaymentProcessor) charged::add)

            .definition(ShoppingCart.class, "cart")

            .build();

    final ShoppingCart cart = system.get(ShoppingCart.class);
    cart.order("order-101");

    assertEquals(1, charged.size());
}
```

The `@Component` reference to `stripe` resolves to the lambda you provided. The component under test doesn't know or care that it's a test double.

## Mixing Approaches

Plain Java tests and `System.builder()` tests serve different purposes:

| Approach | Best For |
|----------|---------|
| **Plain Java** | Unit testing a single class in isolation. Fast, no framework overhead. |
| **System.builder()** | Integration testing the wired object graph. Verifies resolution, events, and component interaction. |

Use both. Plain Java tests verify your logic. `System.builder()` tests verify your wiring.
