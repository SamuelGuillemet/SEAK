package pfe_broker.order_book.market_data;

public class DuplicateEntryException extends Exception {

  public DuplicateEntryException(String message) {
    super(message);
  }
}
