package enkan.component.jedis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import enkan.middleware.session.KeyValueStore;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.List;

import static enkan.util.ReflectionUtils.tryReflection;
import static java.nio.charset.StandardCharsets.UTF_8;

public class JedisStore implements KeyValueStore {
    private final JedisProvider jedisProvider;
    private final ObjectMapper mapper;

    protected JedisStore(JedisProvider jedisProvider) {
        this.jedisProvider = jedisProvider;
        this.mapper = new ObjectMapper(new MessagePackFactory());
    }

    @Override
    public Serializable read(String key) {
        try(Jedis jedis = jedisProvider.getClient()) {
            List<byte[]> objs = jedis.mget(
                    (key + ":class").getBytes(UTF_8),
                    key.getBytes(UTF_8));
            if (objs == null || objs.get(0) == null) return null;

            Class<?> cls = tryReflection(() -> Class.forName(new String(objs.get(0), UTF_8)));
            return (Serializable) mapper.readValue(objs.get(1), cls);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String write(String key, Serializable value) {
        try(Jedis jedis = jedisProvider.getClient()) {
            jedis.mset(
                    (key + ":class").getBytes(UTF_8), value.getClass().getName().getBytes(UTF_8),
                    key.getBytes(UTF_8), mapper.writeValueAsBytes(value));
            return key;
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String delete(String key) {
        try(Jedis jedis = jedisProvider.getClient()) {
            jedis.del((key + ":class").getBytes(UTF_8),
                    key.getBytes(UTF_8));
            return key;
        }
    }
}
