/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package io.seak.avro;
@org.apache.avro.specific.AvroGenerated
public enum OrderRejectReason implements org.apache.avro.generic.GenericEnumSymbol<OrderRejectReason> {
  BROKER_EXCHANGE_OPTION, UNKNOWN_SYMBOL, EXCHANGE_CLOSED, ORDER_EXCEEDS_LIMIT, TOO_LATE_TO_ENTER, UNKNOWN_ORDER, DUPLICATE_ORDER, STALE_ORDER, INCORRECT_QUANTITY, UNKNOWN_ACCOUNT, PRICE_EXCEEDS_CURRENT_PRICE_BAND, OTHER  ;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"enum\",\"name\":\"OrderRejectReason\",\"namespace\":\"io.seak.avro\",\"symbols\":[\"BROKER_EXCHANGE_OPTION\",\"UNKNOWN_SYMBOL\",\"EXCHANGE_CLOSED\",\"ORDER_EXCEEDS_LIMIT\",\"TOO_LATE_TO_ENTER\",\"UNKNOWN_ORDER\",\"DUPLICATE_ORDER\",\"STALE_ORDER\",\"INCORRECT_QUANTITY\",\"UNKNOWN_ACCOUNT\",\"PRICE_EXCEEDS_CURRENT_PRICE_BAND\",\"OTHER\"]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }

  @Override
  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
}
