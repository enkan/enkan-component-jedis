# enkan-component-jedis

## Overview

A Redis session store component for the enkan framework.
Stores session data in Redis using Jedis 7.x, serialized with CBOR (Jackson 3.x).

## Build & Test

```bash
mvn test       # Run tests (requires Docker)
mvn package    # Build jar
```

Tests spin up a Redis container via Docker. The Docker daemon must be running.

## Key classes

| Class | Responsibility |
| ----- | -------------- |
| `JedisProvider` | enkan component. Manages `RedisClient` (Jedis 7.x `UnifiedJedis`) lifecycle and creates `JedisStore` instances |
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

- Default `expiry = -1` means no TTL
- Any non-positive value (`<= 0`) is treated as "no TTL" (Redis rejects `EX 0`, so `0` is not forwarded)
- When `expiry > 0`, the TTL is reset on every `read` call (sliding TTL) using Redis `GETEX`
- Writes with a positive expiry use atomic `SET ... EX` so SET and TTL are applied together

### Serialization

CBOR (RFC 7049) via `tools.jackson.dataformat:jackson-dataformat-cbor` (Jackson 3.x).
Jackson annotations are supported. **Note:** this is a breaking wire-format change from earlier
releases that used MessagePack — existing Redis data from pre-CBOR versions cannot be read.

## Dependency version notes

| Dependency | Version | Notes |
| ---------- | ------- | ----- |
| enkan-parent | 0.15.0 | Parent pom |
| jedis | 7.4.1 | Managed via `jedis.version` property in `pom.xml` |
| jackson-dataformat-cbor | Inherited from parent (`${jackson.version}`) | Jackson 3.x, package `tools.jackson.*` |
| docker-java-core / transport-httpclient5 | 3.7.1 | Test scope only. Do not use Jersey transport (avoids JAX-RS dependency) |
| hibernate-validator | 9.1.0.Final | Test scope only. Requires `expressly` (Jakarta EL) due to Jakarta namespace |
