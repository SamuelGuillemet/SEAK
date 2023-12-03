package pfe_broker.common.utils;

import java.time.Duration;
import java.util.Properties;
import java.util.stream.Stream;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.shaded.com.trilead.ssh2.log.Logger;
import org.testcontainers.utility.DockerImageName;

public class KafkaTestContainer extends GenericContainer<KafkaTestContainer> {

  Logger LOG = Logger.getLogger(KafkaTestContainer.class);

  private final Network KAFKA_NETWORK = Network.newNetwork();
  private final String CONFLUENT_PLATFORM_VERSION = "7.4.1";
  private final DockerImageName KAFKA_IMAGE = DockerImageName
    .parse("confluentinc/cp-kafka")
    .withTag(CONFLUENT_PLATFORM_VERSION);
  private final KafkaContainer KAFKA = new KafkaContainer(KAFKA_IMAGE)
    .withNetwork(KAFKA_NETWORK)
    .withKraft()
    .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
    .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1");

  private final SchemaRegistryContainer SCHEMA_REGISTRY =
    new SchemaRegistryContainer(CONFLUENT_PLATFORM_VERSION)
      .withStartupTimeout(Duration.ofMinutes(2));

  public KafkaTestContainer() {
    // This is useless but required by testcontainers
    super(DockerImageName.parse("confluentinc/cp-kafka"));
  }

  @Override
  public void start() {
    KAFKA.start();
    LOG.log(3, "Kafka started");
    SCHEMA_REGISTRY.withKafka(KAFKA).start();
    LOG.log(3, "Schema registry started");
    // Should be set after container is started
    SCHEMA_REGISTRY.withEnv(
      "SCHEMA_REGISTRY_LISTENERS",
      SCHEMA_REGISTRY.getSchemaUrl()
    );
  }

  public boolean isRunning() {
    return KAFKA.isRunning() && SCHEMA_REGISTRY.isRunning();
  }

  public void registerTopics(String... topics) {
    Properties properties = new Properties();
    properties.put("bootstrap.servers", KAFKA.getBootstrapServers());

    try (Admin admin = Admin.create(properties)) {
      CreateTopicsResult result = admin.createTopics(
        Stream
          .of(topics)
          .map(topic -> new NewTopic(topic, 2, (short) 1))
          .toList()
      );
      result.all().get();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void stop() {
    KAFKA.stop();
    SCHEMA_REGISTRY.stop();
  }

  public String getBootstrapServers() {
    return KAFKA.getBootstrapServers();
  }

  public String getSchemaRegistryUrl() {
    return SCHEMA_REGISTRY.getSchemaUrl();
  }

  private class SchemaRegistryContainer
    extends GenericContainer<SchemaRegistryContainer> {

    public static final String SCHEMA_REGISTRY_IMAGE =
      "confluentinc/cp-schema-registry";
    public final int SCHEMA_REGISTRY_PORT = 8081;

    @SuppressWarnings("unused")
    public SchemaRegistryContainer() {
      this(CONFLUENT_PLATFORM_VERSION);
    }

    public SchemaRegistryContainer(String version) {
      super(
        DockerImageName
          .parse(SCHEMA_REGISTRY_IMAGE)
          .withTag(CONFLUENT_PLATFORM_VERSION)
      );
      waitingFor(Wait.forHttp("/subjects").forStatusCode(200));
      withExposedPorts(SCHEMA_REGISTRY_PORT);
    }

    public SchemaRegistryContainer withKafka(KafkaContainer kafka) {
      return withKafka(
        kafka.getNetwork(),
        kafka.getNetworkAliases().get(0) + ":9092"
      );
    }

    public SchemaRegistryContainer withKafka(
      Network network,
      String bootstrapServers
    ) {
      withNetwork(network);
      withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry");
      withEnv(
        "SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS",
        "PLAINTEXT://" + bootstrapServers
      );
      return self();
    }

    public String getSchemaUrl() {
      return String.format(
        "http://%s:%d",
        getHost(),
        getMappedPort(SCHEMA_REGISTRY_PORT)
      );
    }
  }
}
