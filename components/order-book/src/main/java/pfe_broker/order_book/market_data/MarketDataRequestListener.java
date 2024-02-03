package pfe_broker.order_book.market_data;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.OffsetReset;
import io.micronaut.configuration.kafka.annotation.Topic;
import jakarta.inject.Singleton;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pfe_broker.avro.MarketData;
import pfe_broker.avro.MarketDataRejected;
import pfe_broker.avro.MarketDataRejectedReason;
import pfe_broker.avro.MarketDataRequest;
import pfe_broker.avro.MarketDataResponse;
import pfe_broker.common.MarketDataSeeker;
import pfe_broker.common.SymbolReader;
import pfe_broker.order_book.MessageProducer;

@Singleton
public class MarketDataRequestListener {

  private static final Logger LOG = LoggerFactory.getLogger(
    MarketDataRequestListener.class
  );

  private final SymbolReader symbolReader;
  private final MessageProducer messageProducer;
  private final MarketDataSeeker marketDataSeeker;
  private final MarketDataSubscriptionCatalog marketDataSubscriptionCatalog;
  private final MeterRegistry meterRegistry;

  public MarketDataRequestListener(
    MessageProducer messageProducer,
    MarketDataSeeker marketDataSeeker,
    MarketDataSubscriptionCatalog marketDataSubscriptionCatalog,
    SymbolReader symbolReader,
    MeterRegistry meterRegistry
  ) {
    this.messageProducer = messageProducer;
    this.marketDataSeeker = marketDataSeeker;
    this.marketDataSubscriptionCatalog = marketDataSubscriptionCatalog;
    this.symbolReader = symbolReader;
    this.meterRegistry = meterRegistry;
  }

  @KafkaListener(
    groupId = "order-book-market-data-request",
    batch = true,
    offsetReset = OffsetReset.EARLIEST
  )
  @Topic(patterns = "${kafka.topics.market-data-request}")
  public void receiveMarketDataRequest(
    List<ConsumerRecord<String, MarketDataRequest>> records
  ) {
    records.forEach(item -> {
      Timer marketDataRequestTimer = meterRegistry.timer(
        "order_book_market_data_request",
        "subscriptionRequest",
        item.value().getMarketDataSubscriptionRequest().toString()
      );
      Timer.Sample sample = Timer.start();
      handleMarketDataRequest(item.key(), item.value());
      sample.stop(marketDataRequestTimer);
    });
  }

  private void handleMarketDataRequest(
    String key,
    MarketDataRequest marketDataRequest
  ) {
    LOG.debug(
      "Received market data request of type {}",
      marketDataRequest.getMarketDataSubscriptionRequest()
    );

    // Discard if any symbol is not valid
    if (
      marketDataRequest
        .getSymbols()
        .stream()
        .anyMatch(symbol ->
          !symbolReader.getSymbolsCached().contains(symbol.toString())
        )
    ) {
      rejectRequest(
        key,
        marketDataRequest,
        MarketDataRejectedReason.UNKNOWN_SYMBOL
      );
      return;
    }

    if (marketDataRequest.getDepth() < 0 || marketDataRequest.getDepth() > 10) {
      rejectRequest(
        key,
        marketDataRequest,
        MarketDataRejectedReason.UNSUPPORTED_MARKET_DEPTH
      );
      return;
    }

    switch (marketDataRequest.getMarketDataSubscriptionRequest()) {
      case SUBSCRIBE:
        handleSubscribe(key, marketDataRequest);
        break;
      case UNSUBSCRIBE:
        handleUnsubscribe(marketDataRequest);
        break;
      case SNAPSHOT:
        handleSnapshot(key, marketDataRequest);
        break;
      default:
        rejectRequest(
          key,
          marketDataRequest,
          MarketDataRejectedReason.UNSUPPORTED_SUBSCRIPTION_REQUEST_TYPE
        );
    }
  }

  private void handleSubscribe(
    String key,
    MarketDataRequest marketDataRequest
  ) {
    try {
      marketDataSubscriptionCatalog.subscribe(marketDataRequest);
    } catch (DuplicateEntryException e) {
      rejectRequest(
        key,
        marketDataRequest,
        MarketDataRejectedReason.DUPLICATE_MD_REQ_ID
      );
    }
  }

  private void handleUnsubscribe(MarketDataRequest marketDataRequest) {
    String reqId = marketDataRequest.getRequestId().toString();
    if (reqId.equals("logout")) {
      marketDataSubscriptionCatalog.unsubscribeAll(marketDataRequest);
    } else {
      marketDataSubscriptionCatalog.unsubscribe(marketDataRequest);
    }
  }

  private void handleSnapshot(String key, MarketDataRequest marketDataRequest) {
    if (marketDataRequest.getDepth() == 0) {
      rejectRequest(
        key,
        marketDataRequest,
        MarketDataRejectedReason.UNSUPPORTED_MARKET_DEPTH
      );
    }

    for (CharSequence symbol : marketDataRequest.getSymbols()) {
      List<MarketData> marketData = marketDataSeeker.readLastStockData(
        String.valueOf(symbol),
        marketDataRequest.getDepth()
      );

      messageProducer.sendMarketDataResponse(
        key,
        new MarketDataResponse(
          marketDataRequest.getUsername(),
          symbol,
          marketData,
          marketDataRequest.getRequestId(),
          marketDataRequest.getMarketDataEntries()
        )
      );
    }
  }

  private void rejectRequest(
    String key,
    MarketDataRequest marketDataRequest,
    MarketDataRejectedReason reason
  ) {
    LOG.warn(
      "Rejecting market data request {} with reason {}",
      marketDataRequest,
      reason
    );
    this.messageProducer.sendMarketDataRejected(
        key,
        new MarketDataRejected(
          marketDataRequest.getUsername(),
          marketDataRequest.getRequestId(),
          reason
        )
      );
  }
}
