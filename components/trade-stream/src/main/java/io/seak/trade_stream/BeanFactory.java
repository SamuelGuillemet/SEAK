package io.seak.trade_stream;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.seak.avro.RejectedOrder;
import io.seak.avro.Trade;
import io.seak.avro.utils.SchemaRecord;
import org.apache.avro.Schema;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;

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
    NewTopic tradesTopic(
      @Property(name = "kafka.topics.trades") String topicName
    ) {
      return createTopic(topicName);
    }

    @Bean
    NewTopic acceptedTradesTopic(
      @Property(name = "kafka.topics.accepted-trades") String topicName
    ) {
      return createTopic(topicName);
    }

    @Bean
    NewTopic rejectedOrdersTopic(
      @Property(name = "kafka.topics.rejected-orders") String topicName
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
    SchemaRecord tradesSchema(
      @Property(name = "kafka.topics.trades") String topicName
    ) {
      return createSchemaRecord(Trade.getClassSchema(), topicName);
    }

    @Bean
    SchemaRecord acceptedTradesSchema(
      @Property(name = "kafka.topics.accepted-trades") String topicName
    ) {
      return createSchemaRecord(Trade.getClassSchema(), topicName);
    }

    @Bean
    SchemaRecord rejectedOrdersSchema(
      @Property(name = "kafka.topics.rejected-orders") String topicName
    ) {
      return createSchemaRecord(RejectedOrder.getClassSchema(), topicName);
    }
  }
}
