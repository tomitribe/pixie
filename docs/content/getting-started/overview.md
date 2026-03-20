---
title: "Overview"
weight: 1
description: "What is Pixie and why use it"
---

Pixie is a tiny ~100KB jar that handles configuration, dependency injection, and events. It can handle any scenario where you would use reflection to instantiate a Java object.

## Key Features

- **Configuration** — Map properties files directly to constructor parameters
- **Dependency Injection** — Constructor-based injection with automatic type resolution
- **Events** — Fire and observe events across components
- **Type Safety** — Full generic type matching for component resolution
- **Zero Dependencies** — Only depends on `tomitribe-util`
- **Validation** — Strict property validation catches typos and misconfigurations at startup

## How It Works

Pixie uses constructor annotations to map configuration values to Java objects. You annotate your constructor parameters with `@Param`, `@Component`, `@Event`, or `@Name`, then Pixie builds your objects from either a properties file or a fluent builder API.

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

Pixie reads the annotations and wires everything together — no XML, no classpath scanning, no magic.

## No Proxies

Unlike CDI, Pixie does not generate proxies or subclasses around your objects. When Pixie constructs a `Person`, you get an actual `Person` — not a `Person$$Proxy` wrapping the real instance behind an interceptor chain.

This has real benefits:

- **Performance** — No proxy indirection on every method call. Your objects run at full JVM speed with no extra layers. The JIT compiler can inline methods normally.
- **Debugging** — Stack traces show your classes and methods, not proxy infrastructure. When you step through code in a debugger, you step through *your* code.
- **Predictability** — `getClass()` returns what you expect. `instanceof` works naturally. There are no surprises with `final` methods, `private` methods, or self-calls bypassing the proxy.
- **GraalVM / Native Image** — No dynamic proxy generation means fewer reflection headaches and simpler native image configuration.

## No Field Injection

Pixie deliberately does not support field injection. Every dependency must come through a constructor, a `@Factory` method, or a `@Builder` setter — never by reflectively setting fields on an already-constructed object.

This means every class you write with Pixie can also be built in plain Java with `new`. There is no hidden wiring that only works when the framework is present:

```java
// Built by Pixie from a properties file
final System system = new System(properties);
final Person person = system.get(Person.class);

// Built by hand in a test — same class, no framework needed
final Person person = new Person("jane", 37, new Address(...));
```

With field injection, this is not possible. Fields marked `@Inject` are set reflectively after construction, so the class cannot be fully instantiated without the injection framework. The framework becomes load-bearing infrastructure that cannot be removed without rewriting every class that uses it.

Constructor injection avoids this problem entirely. Your classes express their dependencies as normal constructor parameters — the same mechanism Java has always had. Pixie is simply a convenient way to call those constructors. If you ever want to stop using Pixie, you replace `system.get(...)` calls with `new` and everything still compiles.

This makes Pixie safe to adopt on large teams. There is no need to review code to ensure developers aren't using framework-specific features that create hidden coupling. If the constructor compiles, the class works with or without Pixie.
