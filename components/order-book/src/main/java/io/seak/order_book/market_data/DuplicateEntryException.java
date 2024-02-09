package io.seak.order_book.market_data;

public class DuplicateEntryException extends Exception {

  public DuplicateEntryException(String message) {
    super(message);
  }
}
