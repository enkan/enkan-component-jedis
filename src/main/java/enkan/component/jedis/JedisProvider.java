package enkan.component.jedis;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.Serializable;

import static enkan.util.BeanBuilder.builder;

public class JedisProvider extends SystemComponent<JedisProvider> {
    private JedisPool pool;

    private String host = "localhost";
    private int port = 6379;

    private JedisPoolConfig poolConfig;

    @Override
    protected ComponentLifecycle<JedisProvider> lifecycle() {
        return new ComponentLifecycle<JedisProvider>() {
            @Override
            public void start(JedisProvider c) {
                if (c.poolConfig == null) {
                    c.poolConfig = new JedisPoolConfig();
                }
                c.pool = new JedisPool(c.poolConfig, c.host, c.port);
            }

            @Override
            public void stop(JedisProvider c) {
                if (c.pool != null && !c.pool.isClosed()) {
                    c.pool.close();
                }
                c.pool = null;
            }
        };
    }

    public <T extends Serializable> JedisStore<T> createStore(String type, Class<T> clazz) {
        if (pool == null) throw new IllegalStateException("JedisProvider is not started");
        return new JedisStore<>(type, pool, clazz);
    }

    public <T extends Serializable> JedisStore<T> createStore(String type, Class<T> clazz, long expiry) {
        if (pool == null) throw new IllegalStateException("JedisProvider is not started");
        return builder(new JedisStore<>(type, pool, clazz))
                .set(JedisStore::setExpiry, expiry)
                .build();
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPoolConfig(JedisPoolConfig poolConfig) {
        this.poolConfig = poolConfig;
    }
}
