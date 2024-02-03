package pfe_broker.market_matcher;

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.context.annotation.Property;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import pfe_broker.avro.MarketData;
import pfe_broker.common.MarketDataSeeker;

@Singleton
public class MarketDataConsumer {

  private final Map<String, MarketData> marketDataMap;
  private final MarketDataSeeker marketDataSeeker;

  @Property(name = "kafka.common.symbol-topic-prefix")
  private String symbolTopicPrefix;

  public MarketDataConsumer(MarketDataSeeker marketDataConsumer) {
    this.marketDataSeeker = marketDataConsumer;
    this.marketDataMap = Collections.synchronizedMap(new HashMap<>());
  }

  @KafkaListener(
    groupId = "market-matcher-market-data",
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
      marketDataMap.put(symbol, marketData);
    });
  }

  public MarketData readLastStockData(String symbol) {
    return marketDataMap.computeIfAbsent(symbol, this::readIndividualData);
  }

  private MarketData readIndividualData(String symbol) {
    List<MarketData> marketDataList = marketDataSeeker.readLastStockData(
      symbol,
      1
    );
    if (marketDataList.isEmpty()) {
      return null;
    }
    return marketDataList.get(0);
  }
}
