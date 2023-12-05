package pfe_broker.common.utils;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class RedisTestContainer extends GenericContainer<RedisTestContainer> {

  private static final String REDIS_IMAGE_VERSION = "7.2.3";
  private final int REDIS_PORT = 6379;

  public RedisTestContainer() {
    this(REDIS_IMAGE_VERSION);
  }

  public RedisTestContainer(String version) {
    super(DockerImageName.parse("redis").withTag(version));
    waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\n", 1));
    withExposedPorts(REDIS_PORT);
    withStartupTimeout(Duration.ofMinutes(2));
  }

  public String getRedisUrl() {
    return String.format("redis://%s:%d", getHost(), getMappedPort(REDIS_PORT));
  }
}
