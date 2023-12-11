package pfe_broker.market_matcher;

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.messaging.annotation.SendTo;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.avro.MarketData;
import pfe_broker.avro.Order;
import pfe_broker.avro.Trade;
import pfe_broker.common.SymbolReader;

@Singleton
public class MarketMatcher {

  private static final Logger LOG = LoggerFactory.getLogger(
    MarketMatcher.class
  );

  private final MarketDataConsumer marketDataConsumer;
  private final SymbolReader symbolReader;
  public List<String> symbols = new ArrayList<>();

  MarketMatcher(
    MarketDataConsumer marketDataProducer,
    SymbolReader symbolReader
  ) {
    this.marketDataConsumer = marketDataProducer;
    this.symbolReader = symbolReader;
  }

  @PostConstruct
  void init() {
    if (this.symbolReader.isKafkaRunning()) {
      this.retreiveSymbols();
    } else {
      LOG.error("Kafka is not running");
    }
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
  List<Trade> receiveAcceptedOrder(List<Order> orders) {
    return orders
      .stream()
      .map(this::processOrder)
      .filter(trade -> trade != null)
      .collect(Collectors.toList());
  }

  private Trade processOrder(Order order) {
    String symbol = order.getSymbol().toString();

    if (!symbols.contains(symbol)) {
      LOG.warn("Ignoring order {} for unknown symbol {}", order, symbol);
      return null;
    }

    MarketData marketData = marketDataConsumer.readLastStockData(symbol);

    if (marketData == null) {
      LOG.warn("Ignoring order {} for unknown symbol {}", order, symbol);
      return null;
    }

    LOG.debug("Matching order {} with market data {}", order, marketData);

    Trade trade = Trade
      .newBuilder()
      .setOrder(order)
      .setPrice(marketData.getClose())
      .setSymbol(symbol)
      .setQuantity(order.getQuantity())
      .build();

    return trade;
  }

  /**
   * Expose this public method to be able to call it from the test
   */
  public void retreiveSymbols() {
    try {
      this.symbols = symbolReader.getSymbols();
    } catch (Exception e) {
      LOG.error("Error while retreiving symbols: {}", e.getMessage());
    }
  }
}
