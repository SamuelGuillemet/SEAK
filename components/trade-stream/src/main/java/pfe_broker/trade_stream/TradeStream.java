package pfe_broker.trade_stream;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import io.micronaut.configuration.kafka.streams.ConfiguredStreamBuilder;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import pfe_broker.avro.RejectedOrder;
import pfe_broker.avro.Trade;

@Factory
public class TradeStream {

  @Property(name = "kafka.schema.registry.url")
  private String schemaRegistryUrl;

  @Property(name = "kafka.topics.trades")
  private String tradesTopic;

  @Property(name = "kafka.topics.accepted-trades")
  private String acceptedTradesTopic;

  @Property(name = "kafka.topics.rejected-orders")
  private String rejectedOrdersTopic;

  @Inject
  private TradeIntegrityCheckService integrityCheckService;

  private final Serdes.StringSerde keySerde = new Serdes.StringSerde();

  @Singleton
  @Named("trade-stream-integrity")
  KStream<String, Trade> tradeStreamIntegrity(ConfiguredStreamBuilder builder) {
    Properties props = builder.getConfiguration();
    props.put(
      StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG,
      Serdes.String().getClass().getName()
    );
    props.put(
      StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG,
      Serdes.String().getClass().getName()
    );

    KStream<String, Trade> tradeStream = builder.stream(
      tradesTopic,
      Consumed.with(this.keySerde, this.tradeAvroSerde())
    );

    KStream<String, TradeIntegrityCheckRecord> integrityCheckedTradeStream =
      tradeStream.mapValues(trade ->
        new TradeIntegrityCheckRecord(
          trade,
          integrityCheckService.checkIntegrity(trade)
        )
      );

    processAcceptedAndRejectedTrades(integrityCheckedTradeStream);

    return tradeStream;
  }

  private void processAcceptedAndRejectedTrades(
    KStream<String, TradeIntegrityCheckRecord> integrityCheckedTradesStream
  ) {
    KStream<String, Trade> acceptedTrades = integrityCheckedTradesStream
      .filter((key, value) -> value.orderRejectReason() == null)
      .mapValues(TradeIntegrityCheckRecord::trade);

    KStream<String, RejectedOrder> rejectedOrders = integrityCheckedTradesStream
      .filter((key, value) -> value.orderRejectReason() != null)
      .mapValues(value ->
        new RejectedOrder(value.trade().getOrder(), value.orderRejectReason())
      );

    acceptedTrades.to(
      acceptedTradesTopic,
      Produced.with(keySerde, this.tradeAvroSerde())
    );

    rejectedOrders.to(
      rejectedOrdersTopic,
      Produced.with(keySerde, this.rejectedOrderAvroSerde())
    );
  }

  private SpecificAvroSerde<RejectedOrder> rejectedOrderAvroSerde() {
    SpecificAvroSerde<RejectedOrder> rejectedOrderAvroSerde =
      new SpecificAvroSerde<>();

    Map<String, String> serdeConfig = new HashMap<>();
    serdeConfig.put(
      AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
      schemaRegistryUrl
    );
    rejectedOrderAvroSerde.configure(serdeConfig, false);
    return rejectedOrderAvroSerde;
  }

  private SpecificAvroSerde<Trade> tradeAvroSerde() {
    SpecificAvroSerde<Trade> tradeAvroSerde = new SpecificAvroSerde<>();

    Map<String, String> serdeConfig = new HashMap<>();
    serdeConfig.put(
      AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG,
      schemaRegistryUrl
    );
    tradeAvroSerde.configure(serdeConfig, false);
    return tradeAvroSerde;
  }
}
