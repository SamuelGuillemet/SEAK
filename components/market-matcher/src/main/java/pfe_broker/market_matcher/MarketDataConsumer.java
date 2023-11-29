package pfe_broker.market_matcher;

import static pfe_broker.log.Log.LOG;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.env.Environment;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import pfe_broker.avro.MarketData;

@Singleton
public class MarketDataConsumer {

  private final Environment environment;
  private final KafkaConsumer<String, MarketData> consumer;

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
    return props;
  }

  public MarketDataConsumer(Environment environment) {
    this.environment = environment;
    this.consumer = new KafkaConsumer<>(this.buildProperties());
    LOG.info("MarketDataConsumer created");
  }

  public MarketData readLastStockData(String symbol) {
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
      consumer.seek(partition, offset);
    }

    ConsumerRecords<String, MarketData> records = consumer.poll(
      Duration.ofMillis(100)
    );

    ConsumerRecord<String, MarketData> stockData = null;
    for (ConsumerRecord<String, MarketData> record : records) {
      if (stockData == null || record.timestamp() > stockData.timestamp()) {
        stockData = record;
      }
    }

    return stockData == null ? null : stockData.value();
  }
}
