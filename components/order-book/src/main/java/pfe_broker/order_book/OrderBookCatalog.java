package pfe_broker.order_book;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class OrderBookCatalog {

  private final Map<String, LimitOrderBook> orderBooks;
  private final MeterRegistry meterRegistry;

  public OrderBookCatalog(MeterRegistry meterRegistry) {
    this.orderBooks = Collections.synchronizedMap(new HashMap<>());
    this.meterRegistry = meterRegistry;
  }

  public void addOrderBook(String symbol) {
    if (orderBooks.containsKey(symbol)) {
      return;
    }
    orderBooks.put(symbol, new LimitOrderBook(symbol, meterRegistry));
  }

  public LimitOrderBook getOrderBook(String symbol) {
    return orderBooks.get(symbol);
  }

  public void clear() {
    orderBooks.clear();
  }
}
