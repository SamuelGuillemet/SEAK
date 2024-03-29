package io.seak.order_stream;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.micronaut.configuration.kafka.streams.ConfiguredStreamBuilder;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.seak.avro.Order;
import io.seak.avro.OrderBookRequest;
import io.seak.avro.OrderBookRequestType;
import io.seak.avro.RejectedOrder;
import io.seak.avro.Type;
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

@Factory
public class OrderStream {

  private final OrderIntegrityCheckService integrityCheckService;

  @Property(name = "kafka.schema.registry.url")
  private String schemaRegistryUrl;

  @Property(name = "kafka.topics.orders")
  private String ordersTopic;

  @Property(name = "kafka.topics.accepted-orders")
  private String acceptedOrdersTopic;

  @Property(name = "kafka.topics.rejected-orders")
  private String rejectedOrdersTopic;

  @Property(name = "kafka.topics.order-book-request")
  private String orderBookRequestTopic;

  private final Serdes.StringSerde keySerde = new Serdes.StringSerde();

  public OrderStream(OrderIntegrityCheckService integrityCheckService) {
    this.integrityCheckService = integrityCheckService;
  }

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

    KStream<String, OrderBookRequest> acceptedOrdersLimit =
      integrityCheckedOrdersStream
        .filter((key, value) ->
          value.orderRejectReason() == null &&
          value.order().getType() == Type.LIMIT
        )
        .mapValues(value ->
          new OrderBookRequest(OrderBookRequestType.NEW, value.order(), null)
        );

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
      orderBookRequestTopic,
      Produced.with(keySerde, this.orderBookRequestAvroSerde())
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

  private SpecificAvroSerde<OrderBookRequest> orderBookRequestAvroSerde() {
    SpecificAvroSerde<OrderBookRequest> orderBookRequestAvroSerde =
      new SpecificAvroSerde<>();

    Map<String, String> serdeConfig = new HashMap<>();
    serdeConfig.put(
      AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
      schemaRegistryUrl
    );
    orderBookRequestAvroSerde.configure(serdeConfig, false);
    return orderBookRequestAvroSerde;
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
