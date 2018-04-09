package enkan.component.jedis;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisProvider extends SystemComponent<JedisProvider> {
    private JedisPool pool;

    private String redisServerAddress;

    private JedisPoolConfig poolConfig;

    public Jedis getClient() {
        return pool.getResource();
    }

    @Override
    protected ComponentLifecycle<JedisProvider> lifecycle() {
        return new ComponentLifecycle<JedisProvider>() {
            @Override
            public void start(JedisProvider c) {
                if (poolConfig == null) {
                    poolConfig = new JedisPoolConfig();
                }
                c.pool = new JedisPool(poolConfig, redisServerAddress);
            }

            @Override
            public void stop(JedisProvider c) {
                if (c.pool != null && !c.pool.isClosed()) {
                    c.pool.close();
                }
            }
        };
    }

    public void setRedisServerAddress(String redisServerAddress) {
        this.redisServerAddress = redisServerAddress;
    }

    public void setPoolConfig(JedisPoolConfig poolConfig) {
        this.poolConfig = poolConfig;
    }
}
