package enkan.component.jedis;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.PullImageResultCallback;
import enkan.system.EnkanSystem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static enkan.util.BeanBuilder.builder;
import static org.assertj.core.api.Assertions.assertThat;

public class JedisStoreTest {
    private static DockerClient docker;
    private String redisContainerId;
    private EnkanSystem system;

    @BeforeAll
    public static void setupDockerClient() {
        docker = DockerClientBuilder.getInstance().build();
        docker.pullImageCmd("redis:alpine").exec(new PullImageResultCallback()).awaitSuccess();
    }

    @BeforeEach
    public void startRedis() {
        CreateContainerResponse containerResponse = docker.createContainerCmd("redis:alpine")
                .exec();
        redisContainerId = containerResponse.getId();
        docker.startContainerCmd(redisContainerId).exec();
        String ipAddress = docker.inspectContainerCmd(redisContainerId).exec()
                .getNetworkSettings()
                .getNetworks()
                .get("bridge")
                .getIpAddress();
        system = EnkanSystem.of("jedis", builder(new JedisProvider())
                .set(JedisProvider::setRedisServerAddress, ipAddress)
                .build());
        system.start();
    }

    @Test
    public void setAndGetAndDelete() {
        JedisProvider jedisProvider = system.getComponent("jedis");
        JedisStore store = new JedisStore(jedisProvider);

        Prefecture tokyo = new Prefecture("13", "Tokyo");
        store.write("13", tokyo);

        assertThat(store.read("13")).isNotNull()
                .isEqualToComparingFieldByField(tokyo);

        store.delete("13");

        assertThat(store.read("13")).isNull();
    }

    @AfterEach
    public void stopRedis() {
        system.stop();
        docker.stopContainerCmd(redisContainerId).exec();
        docker.removeContainerCmd(redisContainerId).exec();
        redisContainerId = null;
    }
}
