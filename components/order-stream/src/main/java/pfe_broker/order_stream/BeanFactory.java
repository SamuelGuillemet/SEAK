package pfe_broker.order_stream;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.NewTopic;
import pfe_broker.avro.Order;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.utils.SchemaRecord;

@Requires(bean = AdminClient.class)
@Factory
public class BeanFactory {

  @Bean
  CreateTopicsOptions options() {
    return new CreateTopicsOptions()
      .timeoutMs(5000)
      .validateOnly(false)
      .retryOnQuotaViolation(false);
  }

  @Bean
  NewTopic ordersTopic(
    @Property(name = "kafka.topics.orders") String topicName
  ) {
    return new NewTopic(topicName, 2, (short) 1);
  }

  @Bean
  NewTopic acceptedOrdersTopic(
    @Property(name = "kafka.topics.accepted-orders") String topicName
  ) {
    return new NewTopic(topicName, 2, (short) 1);
  }

  @Bean
  NewTopic acceptedOrdersOrderBookTopic(
    @Property(name = "kafka.topics.accepted-orders-order-book") String topicName
  ) {
    return new NewTopic(topicName, 2, (short) 1);
  }

  @Bean
  NewTopic rejectedOrdersTopic(
    @Property(name = "kafka.topics.rejected-orders") String topicName
  ) {
    return new NewTopic(topicName, 2, (short) 1);
  }

  @Bean
  public SchemaRecord ordersSchema(
    @Property(name = "kafka.topics.orders") String topicName
  ) {
    return new SchemaRecord(Order.getClassSchema(), topicName);
  }

  @Bean
  public SchemaRecord acceptedOrdersSchema(
    @Property(name = "kafka.topics.accepted-orders") String topicName
  ) {
    return new SchemaRecord(Order.getClassSchema(), topicName);
  }

  @Bean
  public SchemaRecord acceptedOrdersOrderBookSchema(
    @Property(name = "kafka.topics.accepted-orders-order-book") String topicName
  ) {
    return new SchemaRecord(Order.getClassSchema(), topicName);
  }

  @Bean
  public SchemaRecord rejectedOrdersSchema(
    @Property(name = "kafka.topics.rejected-orders") String topicName
  ) {
    return new SchemaRecord(RejectedOrder.getClassSchema(), topicName);
  }
}
