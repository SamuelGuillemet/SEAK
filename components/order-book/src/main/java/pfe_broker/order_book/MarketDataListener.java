package pfe_broker.order_book;

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.avro.MarketData;
import pfe_broker.avro.MarketDataRequest;
import pfe_broker.avro.MarketDataResponse;
import pfe_broker.avro.Trade;
import pfe_broker.order_book.market_data.MarketDataSubscriptionCatalog;
import pfe_broker.order_book.order_book.LimitOrderBook;
import pfe_broker.order_book.order_book.OrderBookCatalog;

@Singleton
public class MarketDataListener {

  private static final Logger LOG = LoggerFactory.getLogger(
    MarketDataListener.class
  );

  @Property(name = "kafka.common.symbol-topic-prefix")
  private String symbolTopicPrefix;

  private final OrderBookCatalog orderBooks;
  private final MarketDataSubscriptionCatalog marketDataSubscriptionCatalog;
  private final MessageProducer tradeProducer;

  public MarketDataListener(
    OrderBookCatalog orderBooks,
    MarketDataSubscriptionCatalog marketDataSubscriptionCatalog,
    MessageProducer tradeProducer
  ) {
    this.orderBooks = orderBooks;
    this.marketDataSubscriptionCatalog = marketDataSubscriptionCatalog;
    this.tradeProducer = tradeProducer;
  }

  @KafkaListener(
    groupId = "order-book-market-data",
    batch = true,
    threadsValue = "${kafka.common.market-data-thread-pool-size}"
  )
  @Topic(patterns = "${kafka.common.symbol-topic-prefix}[A-Z]+")
  public void receiveMarketData(
    List<ConsumerRecord<String, MarketData>> records
  ) {
    records.forEach(item -> {
      MarketData marketData = item.value();
      String symbol = item.topic().substring(symbolTopicPrefix.length());

      LimitOrderBook orderBook = orderBooks.getOrderBook(symbol);
      if (orderBook != null) {
        Map<String, Trade> trades = orderBook.matchOrdersToTrade(marketData);
        if (!trades.isEmpty()) {
          LOG.debug("Sending {} trades to Kafka", trades.size());
          trades.forEach(tradeProducer::sendTrade);
        }
      }

      List<MarketDataRequest> marketDataRequests =
        marketDataSubscriptionCatalog.getMarketDataRequests(symbol);
      for (MarketDataRequest marketDataRequest : marketDataRequests) {
        MarketDataResponse marketDataResponse = new MarketDataResponse(
          marketDataRequest.getUsername(),
          symbol,
          List.of(marketData),
          marketDataRequest.getRequestId(),
          marketDataRequest.getMarketDataEntries()
        );
        String key = String.format(
          "%s:%s:update",
          marketDataRequest.getUsername(),
          marketDataRequest.getRequestId()
        );
        tradeProducer.sendMarketDataResponse(key, marketDataResponse);
      }
    });
  }
}
