# enkan-component-jedis

A Redis session store component for [enkan](https://github.com/enkan/enkan).
Stores session data in Redis via Jedis, serialized with MessagePack.

## Requirements

- Java 11+
- Redis server

## Installation

```xml
<dependency>
    <groupId>net.unit8.enkan</groupId>
    <artifactId>enkan-component-jedis</artifactId>
    <version>0.11.0</version>
</dependency>
```

## Usage

### Registering the component

```java
EnkanSystem system = EnkanSystem.of(
    "jedis", builder(new JedisProvider())
        .set(JedisProvider::setHost, "localhost")
        .set(JedisProvider::setPort, 6379)
        .build()
);
system.start();
```

### Creating a store

Pass the value type explicitly to `createStore`. Redis keys are stored in `type:key` format.

```java
JedisProvider jedis = system.getComponent("jedis");

// Without TTL
JedisStore<UserSession> store = jedis.createStore("session", UserSession.class);

// With TTL (seconds)
JedisStore<UserSession> storeWithTtl = jedis.createStore("session", UserSession.class, 1800L);
```

### Read, write, and delete

```java
// Write
store.write("user:42", new UserSession("alice"));

// Read
UserSession session = store.read("user:42");

// Delete
store.delete("user:42");
```

### Customizing JedisPoolConfig

```java
JedisPoolConfig poolConfig = new JedisPoolConfig();
poolConfig.setMaxTotal(32);
poolConfig.setMaxIdle(8);

EnkanSystem system = EnkanSystem.of(
    "jedis", builder(new JedisProvider())
        .set(JedisProvider::setPoolConfig, poolConfig)
        .build()
);
```

## Design notes

- `JedisStore<T>` deserializes only to the `Class<T>` passed at creation time. Even if Redis data is tampered with, arbitrary class instantiation cannot occur.
- Serialization uses MessagePack via Jackson's `ObjectMapper`, so Jackson annotations are supported.
- When a TTL is set, it is reset on every `read` call (sliding TTL).

## License

Apache License 2.0
