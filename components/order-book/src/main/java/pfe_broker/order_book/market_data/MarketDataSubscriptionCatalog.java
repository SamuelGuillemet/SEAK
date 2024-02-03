package pfe_broker.order_book.market_data;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import pfe_broker.avro.MarketDataRequest;

@Singleton
public class MarketDataSubscriptionCatalog {

  /*
   * Map of symbol to map of username to market data request
   */
  private final Map<
    String,
    Map<String, MarketDataRequest>
  > marketDataSubscription;

  public MarketDataSubscriptionCatalog(MeterRegistry meterRegistry) {
    this.marketDataSubscription = Collections.synchronizedMap(new HashMap<>());

    meterRegistry.gauge(
      "order_book_market_data_subscription",
      marketDataSubscription,
      k -> countMarketDataRequests()
    );
  }

  private long countMarketDataRequests() {
    long total = 0;
    for (Map<
      String,
      MarketDataRequest
    > requests : marketDataSubscription.values()) {
      total += requests.size();
    }
    return total;
  }

  public void subscribe(MarketDataRequest marketDataRequest)
    throws DuplicateEntryException {
    for (CharSequence symbol : marketDataRequest.getSymbols()) {
      Map<String, MarketDataRequest> requests =
        marketDataSubscription.computeIfAbsent(
          String.valueOf(symbol),
          key -> new HashMap<>()
        );
      String username = marketDataRequest.getUsername().toString();
      if (
        requests.containsKey(username) &&
        requests
          .get(username)
          .getRequestId()
          .equals(marketDataRequest.getRequestId())
      ) {
        throw new DuplicateEntryException("Market data request already exists");
      }
      requests.put(
        marketDataRequest.getUsername().toString(),
        marketDataRequest
      );
    }
  }

  public void unsubscribe(MarketDataRequest marketDataRequest) {
    for (CharSequence symbol : marketDataRequest.getSymbols()) {
      Map<String, MarketDataRequest> requests = marketDataSubscription.get(
        String.valueOf(symbol)
      );
      if (requests == null) {
        continue;
      }
      requests.remove(marketDataRequest.getUsername().toString());
    }
  }

  public void unsubscribeAll(MarketDataRequest marketDataRequest) {
    String username = marketDataRequest.getUsername().toString();
    for (Map<
      String,
      MarketDataRequest
    > requests : marketDataSubscription.values()) {
      requests.remove(username);
    }
  }

  public List<MarketDataRequest> getMarketDataRequests(String symbol) {
    return marketDataSubscription
      .getOrDefault(symbol, new HashMap<>())
      .values()
      .stream()
      .toList();
  }

  public void clear() {
    marketDataSubscription.clear();
  }
}
