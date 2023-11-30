package pfe_broker.market_matcher;

import static pfe_broker.log.Log.LOG;

import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.OffsetStrategy;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.messaging.annotation.SendTo;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.common.IsolationLevel;
import pfe_broker.avro.MarketData;
import pfe_broker.avro.Order;
import pfe_broker.avro.Trade;
import pfe_broker.common.SymbolReader;

@Singleton
public class MarketMatcher {

  private final MarketDataConsumer marketDataProducer;
  private final SymbolReader symbolReader;
  public List<String> symbols = new ArrayList<>();

  MarketMatcher(
    MarketDataConsumer marketDataProducer,
    SymbolReader symbolReader
  ) {
    this.marketDataProducer = marketDataProducer;
    this.symbolReader = symbolReader;
    LOG.debug("OrderConsumer created");
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
    offsetReset = OffsetReset.EARLIEST,
    producerClientId = "trades-producer",
    offsetStrategy = OffsetStrategy.SYNC_PER_RECORD,
    isolation = IsolationLevel.READ_COMMITTED
  )
  @Topic("${kafka.topics.accepted-orders}")
  @SendTo("${kafka.topics.trades}")
  Trade receiveAcceptedOrder(@KafkaKey String key, Order order) {
    String symbol = order.getSymbol().toString();

    if (!symbols.contains(symbol)) {
      LOG.error("Ignoring order " + order + " for unknown symbol " + symbol);
      return null;
    }

    MarketData marketData = marketDataProducer.readLastStockData(symbol);

    if (marketData == null) {
      LOG.error("Ignoring order " + order + " for unknown symbol " + symbol);
      return null;
    }

    LOG.info(
      "Matching order " +
      order +
      " with market data " +
      marketData +
      "at instant " +
      java.time.Instant.now()
    );

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
      LOG.error("Error while retreiving symbols: " + e.getMessage());
    }
  }
}
