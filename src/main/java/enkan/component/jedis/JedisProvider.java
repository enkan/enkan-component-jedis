package enkan.component.jedis;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.builders.StandaloneClientBuilder;

import java.io.Serializable;

import static enkan.util.BeanBuilder.builder;

/**
 * An enkan component that manages a {@link RedisClient} and creates
 * type-safe {@link JedisStore} instances.
 *
 * <p>The client is created when the component is started and closed when it is
 * stopped. A custom {@link ConnectionPoolConfig} can be supplied via
 * {@link #setPoolConfig(ConnectionPoolConfig)} before the component is started.
 *
 * @author kawasima
 */
public class JedisProvider extends SystemComponent<JedisProvider> {
    private RedisClient jedis;

    private String host = "localhost";
    private int port = 6379;

    private ConnectionPoolConfig poolConfig;

    @Override
    protected ComponentLifecycle<JedisProvider> lifecycle() {
        return new ComponentLifecycle<JedisProvider>() {
            @Override
            public void start(JedisProvider c) {
                StandaloneClientBuilder<RedisClient> builder = RedisClient.builder()
                        .hostAndPort(c.host, c.port);
                if (c.poolConfig != null) {
                    builder.poolConfig(c.poolConfig);
                }
                c.jedis = builder.build();
            }

            @Override
            public void stop(JedisProvider c) {
                if (c.jedis != null) {
                    c.jedis.close();
                }
                c.jedis = null;
            }
        };
    }

    /**
     * Creates a new {@link JedisStore} with no TTL.
     *
     * @param <T>   the value type
     * @param type  the key prefix used to namespace entries in Redis
     * @param clazz the class to deserialize values into
     * @return a new store instance
     * @throws IllegalStateException if this component has not been started
     */
    public <T extends Serializable> JedisStore<T> createStore(String type, Class<T> clazz) {
        if (jedis == null) throw new IllegalStateException("JedisProvider is not started");
        return new JedisStore<>(type, jedis, clazz);
    }

    /**
     * Creates a new {@link JedisStore} with a sliding TTL.
     *
     * @param <T>    the value type
     * @param type   the key prefix used to namespace entries in Redis
     * @param clazz  the class to deserialize values into
     * @param expiry the TTL in seconds; reset on every read (sliding expiration)
     * @return a new store instance
     * @throws IllegalStateException if this component has not been started
     */
    public <T extends Serializable> JedisStore<T> createStore(String type, Class<T> clazz, long expiry) {
        if (jedis == null) throw new IllegalStateException("JedisProvider is not started");
        return builder(new JedisStore<>(type, jedis, clazz))
                .set(JedisStore::setExpiry, expiry)
                .build();
    }

    /**
     * Sets the Redis host. Defaults to {@code "localhost"}.
     *
     * @param host the Redis server hostname or IP address
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Sets the Redis port. Defaults to {@code 6379}.
     *
     * @param port the Redis server port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Sets a custom pool configuration. If not set, a default
     * {@link ConnectionPoolConfig} is used.
     *
     * @param poolConfig the pool configuration
     */
    public void setPoolConfig(ConnectionPoolConfig poolConfig) {
        this.poolConfig = poolConfig;
    }
}
