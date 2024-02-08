package io.seak.order_stream.mocks;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.seak.avro.Order;
import io.seak.avro.OrderBookRequest;
import io.seak.avro.RejectedOrder;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class MockOrderListener {

  public List<Order> acceptedOrders = new ArrayList<>();
  public List<RejectedOrder> rejectedOrders = new ArrayList<>();
  public List<OrderBookRequest> orderBookRequests = new ArrayList<>();

  @KafkaListener(
    groupId = "mock-orders-consumer",
    offsetReset = OffsetReset.EARLIEST
  )
  @Topic("${kafka.topics.accepted-orders}")
  void receiveAcceptedOrder(@KafkaKey String key, Order order) {
    acceptedOrders.add(order);
  }

  @KafkaListener(
    groupId = "mock-rejected-orders-consumer",
    offsetReset = OffsetReset.EARLIEST
  )
  @Topic("${kafka.topics.rejected-orders}")
  void receiveRejectedOrder(@KafkaKey String key, RejectedOrder order) {
    rejectedOrders.add(order);
  }

  @KafkaListener(
    groupId = "mock-order-book-request-consumer",
    offsetReset = OffsetReset.EARLIEST
  )
  @Topic("${kafka.topics.order-book-request}")
  void receiveOrderBookRequest(@KafkaKey String key, OrderBookRequest order) {
    orderBookRequests.add(order);
  }
}
