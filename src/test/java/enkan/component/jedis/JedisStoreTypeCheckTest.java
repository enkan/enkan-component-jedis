package enkan.component.jedis;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.params.SetParams;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JedisStoreTypeCheckTest {
    /**
     * UnifiedJedis subclass that fails the test if any command method is invoked.
     * Ensures the type check in {@link JedisStore#write} rejects wrong-typed values
     * before making any Redis call.
     */
    static class NoCallJedis extends UnifiedJedis {
        NoCallJedis() {
            super();
        }

        @Override
        public String set(byte[] key, byte[] value) {
            throw new AssertionError("Redis should not be called when type check fails");
        }

        @Override
        public String set(byte[] key, byte[] value, SetParams params) {
            throw new AssertionError("Redis should not be called when type check fails");
        }
    }

    @Test
    public void write_rejects_wrong_type() {
        UnifiedJedis noCall = new NoCallJedis();
        JedisStore<Prefecture> store = new JedisStore<>("test", noCall, Prefecture.class);

        assertThatThrownBy(() -> store.write("key", new OtherSerializable()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Prefecture");
    }

    static class OtherSerializable implements Serializable {}
}
