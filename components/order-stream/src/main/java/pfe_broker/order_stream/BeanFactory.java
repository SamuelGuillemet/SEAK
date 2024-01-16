package pfe_broker.order_stream;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import org.apache.avro.Schema;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import pfe_broker.avro.Order;
import pfe_broker.avro.OrderBookRequest;
import pfe_broker.avro.RejectedOrder;
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
    NewTopic ordersTopic(
      @Property(name = "kafka.topics.orders") String topicName
    ) {
      return createTopic(topicName);
    }

    @Bean
    NewTopic acceptedOrdersTopic(
      @Property(name = "kafka.topics.accepted-orders") String topicName
    ) {
      return createTopic(topicName);
    }

    @Bean
    NewTopic acceptedOrdersOrderBookTopic(
      @Property(name = "kafka.topics.order-book-request") String topicName
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
    SchemaRecord ordersSchema(
      @Property(name = "kafka.topics.orders") String topicName
    ) {
      return createSchemaRecord(Order.getClassSchema(), topicName);
    }

    @Bean
    SchemaRecord acceptedOrdersSchema(
      @Property(name = "kafka.topics.accepted-orders") String topicName
    ) {
      return createSchemaRecord(Order.getClassSchema(), topicName);
    }

    @Bean
    SchemaRecord acceptedOrdersOrderBookSchema(
      @Property(name = "kafka.topics.order-book-request") String topicName
    ) {
      return createSchemaRecord(OrderBookRequest.getClassSchema(), topicName);
    }

    @Bean
    SchemaRecord rejectedOrdersSchema(
      @Property(name = "kafka.topics.rejected-orders") String topicName
    ) {
      return createSchemaRecord(RejectedOrder.getClassSchema(), topicName);
    }
  }
}
