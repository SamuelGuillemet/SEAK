package pfe_broker.common;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Requires(property = "kafka.bootstrap.servers")
public class SymbolReader {

  private static final Logger LOG = LoggerFactory.getLogger(SymbolReader.class);

  private final List<String> symbols;

  @Property(name = "kafka.bootstrap.servers")
  private String bootstrapServers;

  @Property(name = "kafka.common.symbol-topic-prefix")
  private String symbolTopicPrefix;

  public SymbolReader() throws InterruptedException {
    this.symbols = new ArrayList<>();
  }

  @PostConstruct
  void init() {
    retrieveSymbols();
  }

  private List<String> getSymbols() throws InterruptedException {
    Properties props = new Properties();
    props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    try (Admin admin = Admin.create(props)) {
      return admin
        .listTopics()
        .names()
        .get()
        .stream()
        .filter(topic -> topic.startsWith(symbolTopicPrefix))
        .map(topic -> topic.substring(symbolTopicPrefix.length()))
        .toList();
    } catch (ExecutionException e) {
      LOG.error("Error while getting symbols", e);
      return List.of();
    }
  }

  private boolean isKafkaRunning() {
    return UtilsRunning.isKafkaRunning(bootstrapServers);
  }

  public List<String> getSymbolsCached() {
    return symbols;
  }

  public List<String> retrieveSymbols() {
    if (isKafkaRunning()) {
      this.symbols.clear();
      try {
        this.symbols.addAll(getSymbols());
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.error("Error while getting symbols", e);
      }
    } else {
      LOG.error("Kafka is not running");
    }
    return this.symbols;
  }
}
