package enkan.component.jedis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import enkan.middleware.session.KeyValueStore;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class JedisStore<T extends Serializable> implements KeyValueStore {
    private final byte[] keyPrefix;
    private final JedisPool pool;
    private final Class<T> clazz;
    private final ObjectMapper mapper;
    private long expiry = -1;

    protected JedisStore(String type, JedisPool pool, Class<T> clazz) {
        this.keyPrefix = (type + ":").getBytes(UTF_8);
        this.pool = pool;
        this.clazz = clazz;
        this.mapper = new ObjectMapper(new MessagePackFactory());
    }

    @Override
    public T read(String key) {
        try (Jedis jedis = pool.getResource()) {
            byte[] k = objectKey(key);
            byte[] data = jedis.get(k);
            if (data == null) return null;

            if (expiry >= 0) {
                jedis.expire(k, expiry);
            }

            return mapper.readValue(data, clazz);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String write(String key, Serializable value) {
        if (!clazz.isInstance(value)) {
            throw new IllegalArgumentException(
                    "Expected " + clazz.getName() + " but got " + value.getClass().getName());
        }
        try (Jedis jedis = pool.getResource()) {
            byte[] k = objectKey(key);
            jedis.set(k, mapper.writeValueAsBytes(value));
            if (expiry >= 0) {
                jedis.expire(k, expiry);
            }
            return key;
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String delete(String key) {
        try (Jedis jedis = pool.getResource()) {
            jedis.del(objectKey(key));
            return key;
        }
    }

    private byte[] objectKey(String keyFragment) {
        byte[] suffix = keyFragment.getBytes(UTF_8);
        byte[] key = new byte[keyPrefix.length + suffix.length];
        System.arraycopy(keyPrefix, 0, key, 0, keyPrefix.length);
        System.arraycopy(suffix, 0, key, keyPrefix.length, suffix.length);
        return key;
    }

    public void setExpiry(long expiry) {
        this.expiry = expiry;
    }
}
