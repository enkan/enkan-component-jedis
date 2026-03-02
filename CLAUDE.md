# enkan-component-jedis

## Overview

A Redis session store component for the enkan framework.
Stores session data in Redis using Jedis 6.x, serialized with MessagePack.

## Build & Test

```bash
mvn test       # Run tests (requires Docker)
mvn package    # Build jar
```

Tests spin up a Redis container via Docker. The Docker daemon must be running.

## Key classes

| Class | Responsibility |
| ----- | -------------- |
| `JedisProvider` | enkan component. Manages `JedisPool` lifecycle and creates `JedisStore` instances |
| `JedisStore<T>` | `KeyValueStore` implementation. Handles read/write/delete and TTL against Redis |

## API

```java
// Create a store bound to a specific type (no TTL)
JedisStore<T> createStore(String type, Class<T> clazz)

// With TTL in seconds (sliding TTL)
JedisStore<T> createStore(String type, Class<T> clazz, long expiry)
```

## Design decisions

### Type-safe deserialization (security)

`JedisStore` deserializes only to the `Class<T>` received at construction time.
Storing class names in Redis has been removed to eliminate the risk of arbitrary class execution if Redis is compromised.

### Redis key structure

Keys follow the format `type:key` (e.g. `session:user:42`). No `:class` key is used.

### TTL

- `expiry = -1` (default) means no TTL
- When `expiry >= 0`, the TTL is reset on every `read` call (sliding TTL)

### Serialization

MessagePack via `jackson-dataformat-msgpack`. Jackson annotations are supported.

## Dependency version notes

| Dependency | Version | Notes |
| ---------- | ------- | ----- |
| jedis | 6.0.0 | Managed via `jedis.version` property in `pom.xml` |
| jackson-dataformat-msgpack | 0.9.9 | Managed via `jackson.msgpack.version` property |
| docker-java-core / transport-httpclient5 | 3.5.1 | Test scope only. Do not use Jersey transport (avoids JAX-RS dependency) |
| hibernate-validator | 9.0.1.Final | Test scope only. Requires `expressly` (Jakarta EL) due to Jakarta namespace |
