package pfe_broker.market_matcher;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.env.Environment;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.avro.MarketData;

@Singleton
public class MarketDataConsumer {

  private static final Logger LOG = LoggerFactory.getLogger(
    MarketDataConsumer.class
  );

  private final Environment environment;
  private final KafkaConsumer<String, MarketData> consumer;

  private Map<String, MarketData> marketDataMap;

  @Property(name = "kafka.common.symbol-topic-prefix")
  private String symbolTopicPrefix;

  protected Properties buildProperties() {
    Properties props = new Properties();
    props.put(
      ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
      environment.get("kafka.bootstrap.servers", String.class).get()
    );
    props.put(
      KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG,
      environment.get("kafka.schema.registry.url", String.class).get()
    );
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "market-matcher-market-data");
    props.put(
      ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
      StringDeserializer.class
    );
    props.put(
      ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
      KafkaAvroDeserializer.class
    );
    props.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true);
    props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 10);
    return props;
  }

  public MarketDataConsumer(Environment environment) {
    this.environment = environment;
    this.consumer = new KafkaConsumer<>(this.buildProperties());
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
    records.forEach(record -> {
      MarketData marketData = record.value();
      String symbol = record.topic().substring(symbolTopicPrefix.length());
      marketDataMap.put(symbol, marketData);
    });
  }

  public MarketData readLastStockData(String symbol) {
    if (!marketDataMap.containsKey(symbol)) {
      readLastStockDataBis(symbol);
    }
    return marketDataMap.get(symbol);
  }

  private void readLastStockDataBis(String symbol) {
    LOG.debug("Reading last stock data for {}", symbol);

    List<TopicPartition> partitions = consumer
      .partitionsFor(symbolTopicPrefix + symbol)
      .stream()
      .map(partitionInfo ->
        new TopicPartition(partitionInfo.topic(), partitionInfo.partition())
      )
      .toList();

    consumer.assign(partitions);
    consumer.seekToEnd(partitions);

    for (TopicPartition partition : partitions) {
      long offset = consumer.position(partition) - 1;
      if (offset < 0) {
        return;
      }
      consumer.seek(partition, offset);
    }

    ConsumerRecords<String, MarketData> records = consumer.poll(
      Duration.ofMillis(10)
    );

    ConsumerRecord<String, MarketData> stockData = null;
    for (ConsumerRecord<String, MarketData> record : records) {
      if (stockData == null || record.timestamp() > stockData.timestamp()) {
        stockData = record;
      }
    }

    MarketData marketData = stockData == null ? null : stockData.value();
    marketDataMap.put(symbol, marketData);
  }
}
