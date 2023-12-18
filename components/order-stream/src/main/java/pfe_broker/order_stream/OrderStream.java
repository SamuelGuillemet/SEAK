package pfe_broker.order_stream;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.micronaut.configuration.kafka.streams.ConfiguredStreamBuilder;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import pfe_broker.avro.Order;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.Type;

@Factory
public class OrderStream {

  @Inject
  private OrderIntegrityCheckService integrityCheckService;

  @Property(name = "kafka.schema.registry.url")
  private String schemaRegistryUrl;

  @Property(name = "kafka.topics.orders")
  private String ordersTopic;

  @Property(name = "kafka.topics.accepted-orders")
  private String acceptedOrdersTopic;

  @Property(name = "kafka.topics.rejected-orders")
  private String rejectedOrdersTopic;

  @Property(name = "kafka.topics.accepted-orders-order-book")
  private String acceptedOrdersOrderBookTopic;

  private final Serdes.StringSerde keySerde = new Serdes.StringSerde();

  @Singleton
  @Named("order-stream-integrity")
  KStream<String, Order> orderStreamIntegrity(ConfiguredStreamBuilder builder) {
    Properties props = builder.getConfiguration();
    props.put(
      StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
      Serdes.String().getClass().getName()
    );
    props.put(
      StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
      Serdes.String().getClass().getName()
    );

    KStream<String, Order> orderStream = builder.stream(
      ordersTopic,
      Consumed.with(this.keySerde, this.orderAvroSerde())
    );

    KStream<String, OrderIntegrityCheckRecord> integrityCheckedOrderStream =
      orderStream.mapValues(order ->
        new OrderIntegrityCheckRecord(
          order,
          integrityCheckService.checkIntegrity(order)
        )
      );

    processAcceptedAndRejectedOrders(integrityCheckedOrderStream);

    return orderStream;
  }

  private void processAcceptedAndRejectedOrders(
    KStream<String, OrderIntegrityCheckRecord> integrityCheckedOrdersStream
  ) {
    KStream<String, Order> acceptedOrdersMarket = integrityCheckedOrdersStream
      .filter((key, value) ->
        value.orderRejectReason() == null &&
        value.order().getType() == Type.MARKET
      )
      .mapValues(OrderIntegrityCheckRecord::order);

    KStream<String, Order> acceptedOrdersLimit = integrityCheckedOrdersStream
      .filter((key, value) ->
        value.orderRejectReason() == null &&
        value.order().getType() == Type.LIMIT
      )
      .mapValues(OrderIntegrityCheckRecord::order);

    KStream<String, RejectedOrder> rejectedOrders = integrityCheckedOrdersStream
      .filter((key, value) -> value.orderRejectReason() != null)
      .mapValues(value ->
        new RejectedOrder(value.order(), value.orderRejectReason())
      );

    acceptedOrdersMarket.to(
      acceptedOrdersTopic,
      Produced.with(keySerde, this.orderAvroSerde())
    );
    acceptedOrdersLimit.to(
      acceptedOrdersOrderBookTopic,
      Produced.with(keySerde, this.orderAvroSerde())
    );
    rejectedOrders.to(
      rejectedOrdersTopic,
      Produced.with(keySerde, this.rejectedOrderAvroSerde())
    );
  }

  private SpecificAvroSerde<Order> orderAvroSerde() {
    SpecificAvroSerde<Order> orderAvroSerde = new SpecificAvroSerde<>();

    Map<String, String> serdeConfig = new HashMap<>();
    serdeConfig.put(
      AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
      schemaRegistryUrl
    );
    orderAvroSerde.configure(serdeConfig, false);
    return orderAvroSerde;
  }

  private SpecificAvroSerde<RejectedOrder> rejectedOrderAvroSerde() {
    SpecificAvroSerde<RejectedOrder> rejectedOrderAvroSerde =
      new SpecificAvroSerde<>();

    Map<String, String> serdeConfig = new HashMap<>();
    serdeConfig.put(
      AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
      schemaRegistryUrl
    );
    rejectedOrderAvroSerde.configure(serdeConfig, false);
    return rejectedOrderAvroSerde;
  }
}
