package enkan.component.jedis;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

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
                if (poolConfig == null) {
                    poolConfig = new JedisPoolConfig();
                }
                c.pool = new JedisPool(poolConfig, host, port);
            }

            @Override
            public void stop(JedisProvider c) {
                if (c.pool != null && !c.pool.isClosed()) {
                    c.pool.close();
                }
            }
        };
    }

    public JedisStore createStore(String type) {
        return new JedisStore(type, pool);
    }

    public JedisStore createStore(String type, int expiry) {
        return builder(new JedisStore(type, pool))
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
