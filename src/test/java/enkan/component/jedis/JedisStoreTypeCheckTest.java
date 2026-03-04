package enkan.component.jedis;

import org.junit.jupiter.api.Test;
import redis.clients.jedis.RedisClient;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JedisStoreTypeCheckTest {
    @Test
    public void write_rejects_wrong_type() {
        // Type check occurs before any Redis call, so an unconnected client is fine
        try (RedisClient client = RedisClient.create("localhost", 0)) {
            JedisStore<Prefecture> store = new JedisStore<>("test", client, Prefecture.class);

            assertThatThrownBy(() -> store.write("key", new OtherSerializable()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Prefecture");
        }
    }

    static class OtherSerializable implements Serializable {}
}
