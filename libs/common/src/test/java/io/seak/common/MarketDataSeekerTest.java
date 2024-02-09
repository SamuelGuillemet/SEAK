package io.seak.common;

import static org.assertj.core.api.Assertions.assertThat;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.seak.avro.MarketData;
import io.seak.common.utils.KafkaTestContainer;
import io.seak.common.utils.mocks.MockMarketDataProducer;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@MicronautTest(transactional = false)
@Testcontainers(disabledWithoutDocker = true)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MarketDataSeekerTest implements TestPropertyProvider {

  @Container
  static final KafkaTestContainer kafka = new KafkaTestContainer();

  @Inject
  MarketDataSeeker marketDataSeeker;

  @Override
  public @NonNull Map<String, String> getProperties() {
    if (!kafka.isRunning()) {
      kafka.start();
    }
    kafka.registerTopics("market-data.AAPL");
    return Map.of(
      "kafka.bootstrap.servers",
      kafka.getBootstrapServers(),
      "kafka.schema.registry.url",
      kafka.getSchemaRegistryUrl()
    );
  }

  @BeforeAll
  void setup(MockMarketDataProducer mockMarketDataProducer) {
    Instant now = Instant.now();
    for (int i = 0; i < 100; i++) {
      mockMarketDataProducer.sendMarketData(
        now.toString(),
        MarketData
          .newBuilder()
          .setClose(100.0f)
          .setHigh(110.0f)
          .setLow(90.0f)
          .setOpen(95.0f)
          .setVolume(10 + i)
          .build()
      );
      now = now.plus(Duration.ofSeconds(1));
    }
  }

  @Test
  void testReadLastStockData() {
    List<MarketData> marketDataList = marketDataSeeker.readLastStockData(
      "AAPL",
      5
    );

    assertThat(marketDataList).hasSize(5);
    assertThat(marketDataList.get(0).getVolume()).isEqualTo(105);
    assertThat(marketDataList.get(1).getVolume()).isEqualTo(106);
    assertThat(marketDataList.get(2).getVolume()).isEqualTo(107);
    assertThat(marketDataList.get(3).getVolume()).isEqualTo(108);
    assertThat(marketDataList.get(4).getVolume()).isEqualTo(109);
  }
}
