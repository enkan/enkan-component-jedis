package enkan.component.jedis;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import enkan.system.EnkanSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.RedisClient;
import redis.clients.jedis.exceptions.JedisConnectionException;

import static enkan.util.BeanBuilder.builder;
import static org.assertj.core.api.Assertions.assertThat;

public class JedisStoreTest {
    private static DockerClient docker;
    private String redisContainerId;
    private EnkanSystem system;

    @BeforeAll
    public static void setupDockerClient() throws InterruptedException {
        DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();
        docker = DockerClientImpl.getInstance(config, httpClient);
        docker.pullImageCmd("redis:alpine").exec(new PullImageResultCallback()).awaitCompletion();
    }

    @BeforeEach
    public void startRedis() throws InterruptedException {
        CreateContainerResponse containerResponse = docker.createContainerCmd("redis:alpine")
                .exec();
        redisContainerId = containerResponse.getId();
        docker.startContainerCmd(redisContainerId).exec();
        String ipAddress = docker.inspectContainerCmd(redisContainerId).exec()
                .getNetworkSettings()
                .getNetworks()
                .get("bridge")
                .getIpAddress();

        waitForRedis(ipAddress, 6379);

        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setJmxEnabled(true);
        system = EnkanSystem.of("jedis", builder(new JedisProvider())
                .set(JedisProvider::setPoolConfig, poolConfig)
                .set(JedisProvider::setHost, ipAddress)
                .build());
        system.start();
    }

    @Test
    public void write_and_read() {
        JedisStore<Prefecture> store = jedisProvider().createStore("redis", Prefecture.class);

        Prefecture tokyo = new Prefecture("13", "Tokyo");
        store.write("13", tokyo);

        assertThat(store.read("13")).isEqualTo(tokyo);
    }

    @Test
    public void delete_removes_entry() {
        JedisStore<Prefecture> store = jedisProvider().createStore("redis", Prefecture.class);

        store.write("13", new Prefecture("13", "Tokyo"));
        store.delete("13");

        assertThat(store.read("13")).isNull();
    }

    @Test
    public void read_returns_null_for_missing_key() {
        JedisStore<Prefecture> store = jedisProvider().createStore("redis", Prefecture.class);

        assertThat(store.read("nonexistent")).isNull();
    }

    @Test
    public void expiry_evicts_entry() throws InterruptedException {
        JedisStore<Prefecture> store = jedisProvider().createStore("redis", Prefecture.class, 1L);

        store.write("13", new Prefecture("13", "Tokyo"));
        // Do not call read() here — it would reset the sliding TTL
        Thread.sleep(2000);

        assertThat(store.read("13")).isNull();
    }

    @AfterEach
    public void stopRedis() {
        if (system != null) system.stop();
        if (redisContainerId != null) {
            docker.stopContainerCmd(redisContainerId).exec();
            docker.removeContainerCmd(redisContainerId).exec();
            redisContainerId = null;
        }
    }

    private static void waitForRedis(String host, int port) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            try (RedisClient client = RedisClient.create(host, port)) {
                client.ping();
                return;
            } catch (JedisConnectionException e) {
                Thread.sleep(500);
            }
        }
        throw new IllegalStateException("Redis did not become ready in time");
    }

    private JedisProvider jedisProvider() {
        return system.getComponent("jedis");
    }
}
