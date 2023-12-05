package pfe_broker.common;

import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;

@Singleton
public class SymbolReader {

  @Property(name = "kafka.bootstrap.servers")
  private String bootstrapServers;

  @Property(name = "kafka.common.symbol-topic-prefix")
  private String symbolTopicPrefix;

  public List<String> getSymbols() {
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
    } catch (InterruptedException | ExecutionException e) {
      throw new RuntimeException(e);
    }
  }

  public boolean isKafkaRunning() {
    return UtilsRunning.isKafkaRunning(bootstrapServers);
  }

  public String getBootstrapServers() {
    return bootstrapServers;
  }
}
