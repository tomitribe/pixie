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

## Type Conversion

All `@Param` values originate as strings — from properties files or the Builder API. Pixie uses the `Converter` from [tomitribe-util](https://github.com/tomitribe/tomitribe-util) to convert these strings into Java types. This is the same conversion system used by [Crest](https://github.com/tomitribe/crest) for CLI argument parsing.

### Conversion Chain

The converter tries the following strategies in order, using the first one that succeeds:

1. **Registered `PropertyEditor`** — if a `java.beans.PropertyEditor` is registered for the type
2. **Enum** — `Enum.valueOf()` with case-insensitive fallback (exact match, then uppercase, then lowercase)
3. **`Constructor(String)`** — any public constructor taking a single `String` parameter
4. **`Constructor(CharSequence)`** — any public constructor taking `CharSequence`
5. **Public static factory method** — any public static method taking `String` and returning the target type. The method name does not matter — `valueOf`, `of`, `parse`, `from`, or any other name will work.

### Built-in Types

The following types work out of the box with no additional configuration:

| Category | Types |
|----------|-------|
| **Primitives & wrappers** | `byte`, `short`, `int`, `long`, `float`, `double`, `boolean`, `char` and their boxed equivalents |
| **Strings** | `String`, `CharSequence` |
| **Enums** | Any enum type (case-insensitive matching) |
| **Files & paths** | `java.io.File` |
| **Network** | `java.net.URI`, `java.net.URL` |
| **Time** | `java.util.concurrent.TimeUnit` |
| **tomitribe-util** | `org.tomitribe.util.Duration` (e.g., `"30 seconds"`, `"5m"`), `org.tomitribe.util.Size` (e.g., `"10mb"`, `"2.5 gb"`) |

### Example with Multiple Types

```java
public class DataTypes {

    public DataTypes(@Param("aString") final String aString,
                     @Param("abyte") final byte abyte,
                     @Param("aint") final int aint,
                     @Param("along") final long along,
                     @Param("afloat") final float afloat,
                     @Param("adouble") final double adouble,
                     @Param("aboolean") final boolean aboolean,
                     @Param("achar") final char achar,
                     @Param("aTimeUnit") final TimeUnit aTimeUnit,
                     @Param("aURI") final URI aURI,
                     @Param("aFile") final File aFile) {
        // ...
    }
}
```

```properties
joe = new://org.example.DataTypes
joe.aString = Hello
joe.abyte = 123
joe.aint = 1234567890
joe.along = 1234567890123456789
joe.afloat = 1.234
joe.adouble = 0.1234567890
joe.aboolean = true
joe.achar = z
joe.aTimeUnit = SECONDS
joe.aURI = https://example.com
joe.aFile = /tmp/data.txt
```

### Enums

Enums are matched case-insensitively. All of the following are equivalent:

```properties
server.state = RUNNING
server.state = running
server.state = Running
```

### Duration and Size

Pixie includes [tomitribe-util](https://github.com/tomitribe/tomitribe-util) which provides human-readable `Duration` and `Size` types:

```java
public class CacheConfig {
    public CacheConfig(@Param("ttl") final Duration ttl,
                       @Param("maxSize") final Size maxSize) {
        // ...
    }
}
```

```properties
cache = new://org.example.CacheConfig
cache.ttl = 30 seconds
cache.maxSize = 64 mb
```

`Duration` accepts formats like `10s`, `30 seconds`, `1 day and 5 hours`, `500ms`.

`Size` accepts formats like `10kb`, `2.5 mb`, `1 gigabyte`, `512 bytes`.

### Custom Types

Any class you write that has a `public Constructor(String)` automatically works as a `@Param` type:

```java
public class EmailAddress {
    private final String value;

    public EmailAddress(final String value) {
        if (!value.contains("@")) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
        this.value = value;
    }

    public String get() { return value; }
}
```

```java
public class NotificationService {
    public NotificationService(@Param("admin") final EmailAddress admin) {
        // ...
    }
}
```

```properties
notifications = new://org.example.NotificationService
notifications.admin = admin@example.com
```

Classes with any public static method that takes a `String` and returns an instance of the class also work automatically. Common conventions include `valueOf(String)`, `of(String)`, `parse(String)`, and `from(String)`, but the method name is not restricted — any name will be discovered.

### Custom PropertyEditor

For full control over conversion, register a `java.beans.PropertyEditor`:

```java
public class HostPortEditor extends PropertyEditorSupport {
    @Override
    public void setAsText(final String text) {
        final String[] parts = text.split(":");
        setValue(new HostPort(parts[0], Integer.parseInt(parts[1])));
    }
}
```

Register it with Java's `PropertyEditorManager`:

```java
PropertyEditorManager.registerEditor(HostPort.class, HostPortEditor.class);
```

The `PropertyEditor` strategy has the highest priority in the conversion chain, so it will take precedence over constructors and static factory methods.
