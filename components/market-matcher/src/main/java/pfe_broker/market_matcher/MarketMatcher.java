package pfe_broker.market_matcher;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.messaging.annotation.SendTo;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.avro.MarketData;
import pfe_broker.avro.Order;
import pfe_broker.avro.OrderRejectReason;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.Trade;
import pfe_broker.common.SymbolReader;

@Singleton
public class MarketMatcher {

  private static final Logger LOG = LoggerFactory.getLogger(
    MarketMatcher.class
  );

  private final MarketDataConsumer marketDataConsumer;
  private final SymbolReader symbolReader;
  private final MeterRegistry meterRegistry;
  private final RejectedOrderProducer rejectedOrderProducer;

  MarketMatcher(
    MarketDataConsumer marketDataConsumer,
    SymbolReader symbolReader,
    MeterRegistry meterRegistry,
    RejectedOrderProducer rejectedOrderProducer
  ) {
    this.marketDataConsumer = marketDataConsumer;
    this.symbolReader = symbolReader;
    this.meterRegistry = meterRegistry;
    this.rejectedOrderProducer = rejectedOrderProducer;
  }

  @KafkaListener(
    groupId = "market-matcher-orders",
    producerClientId = "market-matcher-trades-producer",
    offsetReset = OffsetReset.EARLIEST,
    pollTimeout = "0ms",
    batch = true
  )
  @Topic("${kafka.topics.accepted-orders}")
  @SendTo("${kafka.topics.trades}")
  List<Trade> receiveAcceptedOrder(
    List<ConsumerRecord<String, Order>> records
  ) {
    return records
      .stream()
      .map(item -> processOrder(item.key(), item.value()))
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  private Trade processOrder(String key, Order order) {
    Timer processOrderTimer = meterRegistry.timer(
      "market_matcher_process_order",
      "symbol",
      order.getSymbol().toString(),
      "side",
      order.getSide().toString()
    );
    Timer.Sample sample = Timer.start();

    String symbol = order.getSymbol().toString();

    if (!symbolReader.getSymbolsCached().contains(symbol)) {
      rejectOrder(key, order);
      return null;
    }

    MarketData marketData = marketDataConsumer.readLastStockData(symbol);

    if (marketData == null) {
      rejectOrder(key, order);
      return null;
    }

    LOG.debug("Matching order {} with market data {}", order, marketData);

    sample.stop(processOrderTimer);

    return Trade
      .newBuilder()
      .setOrder(order)
      .setPrice(marketData.getClose())
      .setSymbol(symbol)
      .setQuantity(order.getQuantity())
      .build();
  }

  private void rejectOrder(String key, Order order) {
    LOG.warn(
      "Ignoring order {} for unknown symbol {}",
      order,
      order.getSymbol()
    );
    meterRegistry
      .counter(
        "market_matcher_rejected_order",
        "symbol",
        order.getSymbol().toString(),
        "side",
        order.getSide().toString()
      )
      .increment();
    rejectedOrderProducer.sendRejectedOrder(
      key,
      RejectedOrder
        .newBuilder()
        .setOrder(order)
        .setReason(OrderRejectReason.UNKNOWN_SYMBOL)
        .build()
    );
  }

  /**
   * Expose this public method to be able to call it from the test
   */
  public void retreiveSymbols() throws InterruptedException {
    this.symbolReader.retrieveSymbols();
  }

  public List<String> getSymbols() {
    return this.symbolReader.getSymbolsCached();
  }
}
