package pfe_broker.order_book;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import org.apache.avro.Schema;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import pfe_broker.avro.MarketDataRejected;
import pfe_broker.avro.MarketDataRequest;
import pfe_broker.avro.MarketDataResponse;
import pfe_broker.avro.OrderBookRequest;
import pfe_broker.avro.utils.SchemaRecord;

@Requires(bean = AdminClient.class)
@Factory
public class BeanFactory {

  private static final int DEFAULT_PARTITIONS = 2;
  private static final short DEFAULT_REPLICATION_FACTOR = 1;
  private static final int CREATE_TOPICS_TIMEOUT_MS = 5000;
  private static final boolean CREATE_TOPICS_VALIDATE_ONLY = false;
  private static final boolean CREATE_TOPICS_RETRY_ON_QUOTA_VIOLATION = false;

  @Bean
  CreateTopicsOptions options() {
    return new CreateTopicsOptions()
      .timeoutMs(CREATE_TOPICS_TIMEOUT_MS)
      .validateOnly(CREATE_TOPICS_VALIDATE_ONLY)
      .retryOnQuotaViolation(CREATE_TOPICS_RETRY_ON_QUOTA_VIOLATION);
  }

  @Factory
  static class Topics {

    private NewTopic createTopic(String topicName) {
      return new NewTopic(
        topicName,
        DEFAULT_PARTITIONS,
        DEFAULT_REPLICATION_FACTOR
      );
    }

    @Bean
    NewTopic orderBookResponseTopic(
      @Property(name = "kafka.topics.order-book-response") String topicName
    ) {
      return createTopic(topicName);
    }

    @Bean
    NewTopic orderBookRejectedTopic(
      @Property(name = "kafka.topics.order-book-rejected") String topicName
    ) {
      return createTopic(topicName);
    }

    @Bean
    NewTopic marketDataRequestTopic(
      @Property(name = "kafka.topics.market-data-request") String topicName
    ) {
      return createTopic(topicName);
    }

    @Bean
    NewTopic marketDataResponseTopic(
      @Property(name = "kafka.topics.market-data-response") String topicName
    ) {
      return createTopic(topicName);
    }

    @Bean
    NewTopic marketDataRejectedTopic(
      @Property(name = "kafka.topics.market-data-rejected") String topicName
    ) {
      return createTopic(topicName);
    }
  }

  @Factory
  static class SchemaRegistry {

    private SchemaRecord createSchemaRecord(Schema schema, String topicName) {
      return new SchemaRecord(schema, topicName);
    }

    @Bean
    SchemaRecord orderBookResponseSchema(
      @Property(name = "kafka.topics.order-book-response") String topicName
    ) {
      return createSchemaRecord(OrderBookRequest.getClassSchema(), topicName);
    }

    @Bean
    SchemaRecord orderBookRejectedSchema(
      @Property(name = "kafka.topics.order-book-rejected") String topicName
    ) {
      return createSchemaRecord(OrderBookRequest.getClassSchema(), topicName);
    }

    @Bean
    SchemaRecord marketDataRequestSchema(
      @Property(name = "kafka.topics.market-data-request") String topicName
    ) {
      return createSchemaRecord(MarketDataRequest.getClassSchema(), topicName);
    }

    @Bean
    SchemaRecord marketDataResponseSchema(
      @Property(name = "kafka.topics.market-data-response") String topicName
    ) {
      return createSchemaRecord(MarketDataResponse.getClassSchema(), topicName);
    }

    @Bean
    SchemaRecord marketDataRejectedSchema(
      @Property(name = "kafka.topics.market-data-rejected") String topicName
    ) {
      return createSchemaRecord(MarketDataRejected.getClassSchema(), topicName);
    }
  }
}
