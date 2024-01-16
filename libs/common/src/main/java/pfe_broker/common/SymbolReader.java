package pfe_broker.common;

import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class SymbolReader {

  private static final Logger LOG = LoggerFactory.getLogger(SymbolReader.class);

  @Property(name = "kafka.bootstrap.servers")
  private String bootstrapServers;

  @Property(name = "kafka.common.symbol-topic-prefix")
  private String symbolTopicPrefix;

  public List<String> getSymbols() throws InterruptedException {
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

  public boolean isKafkaRunning() {
    return UtilsRunning.isKafkaRunning(bootstrapServers);
  }
}
