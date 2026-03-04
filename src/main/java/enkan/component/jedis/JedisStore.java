package enkan.component.jedis;

import enkan.middleware.session.KeyValueStore;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.GetExParams;
import redis.clients.jedis.params.SetParams;
import tools.jackson.dataformat.cbor.CBORMapper;
import tools.jackson.databind.ObjectMapper;

import java.io.Serializable;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A {@link KeyValueStore} implementation backed by Redis via Jedis.
 *
 * <p>Data is serialized with CBOR (RFC 7049) via Jackson. Deserialization is
 * restricted to the {@link Class} specified at construction time to prevent
 * arbitrary class instantiation if the Redis data is tampered with.
 *
 * <p>Redis keys follow the format {@code type:key} where {@code type} is the
 * prefix given at construction time.
 *
 * <p>When a non-negative expiry is configured, a sliding TTL is applied:
 * every {@link #read} resets the TTL so that idle sessions expire while
 * active ones are kept alive.
 *
 * @param <T> the type of values stored in this store
 * @author kawasima
 * @see JedisProvider#createStore(String, Class)
 */
public class JedisStore<T extends Serializable> implements KeyValueStore {
    private final byte[] keyPrefix;
    private final UnifiedJedis jedis;
    private final Class<T> clazz;
    private final ObjectMapper mapper;
    private long expiry = -1;

    /**
     * Creates a new store with no TTL.
     *
     * @param type  the key prefix used to namespace entries in Redis
     * @param jedis the Jedis client to use
     * @param clazz the class to deserialize values into
     */
    protected JedisStore(String type, UnifiedJedis jedis, Class<T> clazz) {
        this.keyPrefix = (type + ":").getBytes(UTF_8);
        this.jedis = jedis;
        this.clazz = clazz;
        this.mapper = new CBORMapper();
    }

    /**
     * {@inheritDoc}
     *
     * <p>If a non-negative expiry is configured, the TTL is reset on every
     * read using the Redis {@code GETEX} command (sliding expiration).
     *
     * @throws tools.jackson.core.JacksonException if CBOR deserialization fails
     */
    @Override
    public T read(String key) {
        byte[] k = objectKey(key);
        byte[] data;
        if (expiry >= 0) {
            data = jedis.getEx(k, GetExParams.getExParams().ex(expiry));
        } else {
            data = jedis.get(k);
        }
        if (data == null) return null;

        return mapper.readValue(data, clazz);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The value must be an instance of the class specified at construction
     * time. If a non-negative expiry is configured, the key is written with
     * {@code SET} with {@code EX} option so that the SET and TTL are applied
     * atomically.
     *
     * @throws IllegalArgumentException            if {@code value} is not an instance of {@code T}
     * @throws tools.jackson.core.JacksonException  if CBOR serialization fails
     */
    @Override
    public String write(String key, Serializable value) {
        if (!clazz.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Expected " + clazz.getName() + " but got " + value.getClass().getName());
        }
        byte[] k = objectKey(key);
        byte[] data = mapper.writeValueAsBytes(value);
        if (expiry >= 0) {
            jedis.set(k, data, SetParams.setParams().ex(expiry));
        } else {
            jedis.set(k, data);
        }
        return key;
    }

    /** {@inheritDoc} */
    @Override
    public String delete(String key) {
        jedis.del(objectKey(key));
        return key;
    }

    private byte[] objectKey(String keyFragment) {
        byte[] suffix = keyFragment.getBytes(UTF_8);
        byte[] key = new byte[keyPrefix.length + suffix.length];
        System.arraycopy(keyPrefix, 0, key, 0, keyPrefix.length);
        System.arraycopy(suffix, 0, key, keyPrefix.length, suffix.length);
        return key;
    }

    /**
     * Sets the TTL in seconds applied to keys on read and write.
     *
     * <p>A value of {@code -1} (the default) disables expiration.
     * A value of {@code 0} or greater enables sliding TTL.
     *
     * @param expiry the TTL in seconds, or {@code -1} to disable
     */
    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }
}
