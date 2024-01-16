package pfe_broker.order_book;

import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import pfe_broker.avro.OrderBookRequest;
import pfe_broker.avro.Trade;

@Singleton
public class MessageProducer {

  private static final String SYMBOL_TAG = "symbol";

  private final Producer<String, SpecificRecord> genericProducer;

  private final MeterRegistry meterRegistry;

  @Property(name = "kafka.topics.trades")
  private String tradesTopic;

  @Property(name = "kafka.topics.order-book-response")
  private String orderBookResponseTopic;

  @Property(name = "kafka.topics.order-book-rejected")
  private String orderBookRejectedTopic;

  public MessageProducer(
    @KafkaClient Producer<String, SpecificRecord> genericProducer,
    MeterRegistry meterRegistry
  ) {
    this.genericProducer = genericProducer;
    this.meterRegistry = meterRegistry;
  }

  public void sendTrade(String key, Trade trade) {
    meterRegistry
      .counter("order_book_trades", SYMBOL_TAG, trade.getSymbol().toString())
      .increment();
    genericProducer.send(new ProducerRecord<>(tradesTopic, key, trade));
  }

  public void sendOrderBookResponse(
    String key,
    OrderBookRequest orderBookRequest
  ) {
    meterRegistry
      .counter(
        "order_book_responses",
        SYMBOL_TAG,
        orderBookRequest.getOrder().getSymbol().toString()
      )
      .increment();
    genericProducer.send(
      new ProducerRecord<>(orderBookResponseTopic, key, orderBookRequest)
    );
  }

  public void sendOrderBookRejected(
    String key,
    OrderBookRequest orderBookRequest
  ) {
    meterRegistry
      .counter(
        "order_book_rejected",
        SYMBOL_TAG,
        orderBookRequest.getOrder().getSymbol().toString()
      )
      .increment();
    genericProducer.send(
      new ProducerRecord<>(orderBookRejectedTopic, key, orderBookRequest)
    );
  }
}
