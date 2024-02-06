package pfe_broker.common;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
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
@Requires(property = "kafka.bootstrap.servers")
public class MarketDataSeeker {

  private static final Logger LOG = LoggerFactory.getLogger(
    MarketDataSeeker.class
  );

  private KafkaConsumer<String, MarketData> consumer;

  @Property(name = "kafka.common.symbol-topic-prefix")
  private String symbolTopicPrefix;

  @Property(name = "kafka.bootstrap.servers")
  private String bootstrapServers;

  @Property(name = "kafka.schema.registry.url")
  private String schemaRegistryUrl;

  protected Properties buildProperties() {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(
      AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
      schemaRegistryUrl
    );
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "market-data-seeker");
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

  @PostConstruct
  void init() {
    this.consumer = new KafkaConsumer<>(this.buildProperties());
  }

  public List<MarketData> readLastStockData(String symbol, long depth) {
    LOG.trace("Reading last {} stock data for {}", depth, symbol);

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
      long offset = consumer.position(partition) - depth;
      if (offset < 0) {
        offset = 0;
      }
      consumer.seek(partition, offset);
    }

    ConsumerRecords<String, MarketData> records = consumer.poll(
      Duration.ofMillis(10)
    );

    SortedMap<Long, MarketData> stockData = new TreeMap<>();
    for (ConsumerRecord<String, MarketData> item : records) {
      stockData.put(item.timestamp(), item.value());
    }

    long skipped = stockData.size() - depth;
    if (skipped < 0) {
      skipped = 0;
    }

    return stockData.values().stream().skip(skipped).toList();
  }
}
