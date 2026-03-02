package enkan.component.jedis;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPool;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JedisStoreTypeCheckTest {
    @Test
    public void write_rejects_wrong_type() {
        JedisStore<Prefecture> store = new JedisStore<>("test", unusablePool(), Prefecture.class);

        assertThatThrownBy(() -> store.write("key", new OtherSerializable()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prefecture");
    }

    /** Returns a JedisPool stub that throws if getResource() is called. */
    private static JedisPool unusablePool() {
        return new JedisPool() {
            @Override
            public redis.clients.jedis.Jedis getResource() {
                throw new AssertionError("should not reach Redis");
            }
        };
    }

    static class OtherSerializable implements Serializable {}
}
