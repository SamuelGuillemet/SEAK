package pfe_broker.order_book;

import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import pfe_broker.avro.MarketDataRejected;
import pfe_broker.avro.MarketDataResponse;
import pfe_broker.avro.OrderBookRequest;
import pfe_broker.avro.Trade;

@Singleton
public class MessageProducer {

  private static final String SYMBOL_TAG = "symbol";
  private static final String REQUEST_TYPE = "requestType";
  private static final String REASON = "reason";

  private final Producer<String, SpecificRecord> genericProducer;

  private final MeterRegistry meterRegistry;

  @Property(name = "kafka.topics.trades")
  private String tradesTopic;

  @Property(name = "kafka.topics.order-book-response")
  private String orderBookResponseTopic;

  @Property(name = "kafka.topics.order-book-rejected")
  private String orderBookRejectedTopic;

  @Property(name = "kafka.topics.market-data-response")
  private String marketDataResponseTopic;

  @Property(name = "kafka.topics.market-data-rejected")
  private String marketDataRejectedTopic;

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
        orderBookRequest.getOrder().getSymbol().toString(),
        REQUEST_TYPE,
        orderBookRequest.getType().toString()
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
        orderBookRequest.getOrder().getSymbol().toString(),
        REQUEST_TYPE,
        orderBookRequest.getType().toString()
      )
      .increment();
    genericProducer.send(
      new ProducerRecord<>(orderBookRejectedTopic, key, orderBookRequest)
    );
  }

  public void sendMarketDataResponse(
    String key,
    MarketDataResponse marketDataResponse
  ) {
    meterRegistry
      .counter(
        "order_book_market_data_responses",
        SYMBOL_TAG,
        marketDataResponse.getSymbol().toString()
      )
      .increment();
    genericProducer.send(
      new ProducerRecord<>(marketDataResponseTopic, key, marketDataResponse)
    );
  }

  public void sendMarketDataRejected(
    String key,
    MarketDataRejected marketDataRejected
  ) {
    meterRegistry
      .counter(
        "order_book_market_data_rejected",
        SYMBOL_TAG,
        marketDataRejected.getReason().toString(),
        REASON,
        marketDataRejected.getReason().toString()
      )
      .increment();
    genericProducer.send(
      new ProducerRecord<>(marketDataRejectedTopic, key, marketDataRejected)
    );
  }
}
