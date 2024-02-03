package pfe_broker.avro.utils;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import quickfix.IncorrectTagValue;

public class Converters {

  private Converters() {
  }

  public static class Side {

    private static final Map<Character, pfe_broker.avro.Side> avroSideMap = new HashMap<>();
    private static final Map<pfe_broker.avro.Side, Character> quickfixSideMap = new EnumMap<>(pfe_broker.avro.Side.class);

    private Side() {
    }

    static {
      avroSideMap.put(quickfix.field.Side.BUY, pfe_broker.avro.Side.BUY);
      avroSideMap.put(quickfix.field.Side.SELL, pfe_broker.avro.Side.SELL);

      avroSideMap.entrySet().forEach(entry -> quickfixSideMap.put(entry.getValue(), entry.getKey()));
    }

    public static char charFromAvro(pfe_broker.avro.Side side) {
      if (!quickfixSideMap.containsKey(side)) {
        throw new IllegalArgumentException(side.toString());
      }
      return quickfixSideMap.get(side);
    }

    public static quickfix.field.Side fromAvro(pfe_broker.avro.Side side)  {
      return new quickfix.field.Side(charFromAvro(side));
    }
    public static pfe_broker.avro.Side toAvro(char side) throws IncorrectTagValue  {
      if (!avroSideMap.containsKey(side)) {
        throw new IncorrectTagValue(quickfix.field.Side.FIELD, String.valueOf(side));
      }
      return avroSideMap.get(side);
    }

    public static pfe_broker.avro.Side toAvro(quickfix.field.Side side) throws IncorrectTagValue  {
      return toAvro(side.getValue());
    }
  }

  public static class OrderRejectReason {

    private OrderRejectReason() {
    }

    private static final Map<Integer, pfe_broker.avro.OrderRejectReason> avroReasonMap = new HashMap<>();
    private static final Map<pfe_broker.avro.OrderRejectReason, Integer> quickfixReasonMap = new EnumMap<>(pfe_broker.avro.OrderRejectReason.class);

    static {
      avroReasonMap.put(quickfix.field.OrdRejReason.BROKER_EXCHANGE_OPTION,
          pfe_broker.avro.OrderRejectReason.BROKER_EXCHANGE_OPTION);
      avroReasonMap.put(quickfix.field.OrdRejReason.UNKNOWN_SYMBOL, pfe_broker.avro.OrderRejectReason.UNKNOWN_SYMBOL);
      avroReasonMap.put(quickfix.field.OrdRejReason.EXCHANGE_CLOSED, pfe_broker.avro.OrderRejectReason.EXCHANGE_CLOSED);
      avroReasonMap.put(quickfix.field.OrdRejReason.ORDER_EXCEEDS_LIMIT,
          pfe_broker.avro.OrderRejectReason.ORDER_EXCEEDS_LIMIT);
      avroReasonMap.put(quickfix.field.OrdRejReason.TOO_LATE_TO_ENTER,
          pfe_broker.avro.OrderRejectReason.TOO_LATE_TO_ENTER);
      avroReasonMap.put(quickfix.field.OrdRejReason.UNKNOWN_ORDER, pfe_broker.avro.OrderRejectReason.UNKNOWN_ORDER);
      avroReasonMap.put(quickfix.field.OrdRejReason.DUPLICATE_ORDER, pfe_broker.avro.OrderRejectReason.DUPLICATE_ORDER);
      avroReasonMap.put(quickfix.field.OrdRejReason.STALE_ORDER, pfe_broker.avro.OrderRejectReason.STALE_ORDER);
      avroReasonMap.put(quickfix.field.OrdRejReason.INCORRECT_QUANTITY,
          pfe_broker.avro.OrderRejectReason.INCORRECT_QUANTITY);
      avroReasonMap.put(quickfix.field.OrdRejReason.UNKNOWN_ACCOUNT, pfe_broker.avro.OrderRejectReason.UNKNOWN_ACCOUNT);
      avroReasonMap.put(quickfix.field.OrdRejReason.PRICE_EXCEEDS_CURRENT_PRICE_BAND,
          pfe_broker.avro.OrderRejectReason.PRICE_EXCEEDS_CURRENT_PRICE_BAND);

      avroReasonMap.entrySet().forEach(entry -> quickfixReasonMap.put(entry.getValue(), entry.getKey()));
    }

    public static int intFromAvro(pfe_broker.avro.OrderRejectReason reason)  {
      return quickfixReasonMap.getOrDefault(reason, quickfix.field.OrdRejReason.OTHER);
    }

    public static quickfix.field.OrdRejReason fromAvro(pfe_broker.avro.OrderRejectReason reason)  {
      return new quickfix.field.OrdRejReason(intFromAvro(reason));
    }

    public static pfe_broker.avro.OrderRejectReason toAvro(int reason)  {
      return avroReasonMap.getOrDefault(reason, pfe_broker.avro.OrderRejectReason.OTHER);
    }

    public static pfe_broker.avro.OrderRejectReason toAvro(quickfix.field.OrdRejReason reason)  {
      return toAvro(reason.getValue());
    }
  }

  public static class Type {

    private Type() {
    }

    private static final Map<Character, pfe_broker.avro.Type> avroTypeMap = new HashMap<>();
    private static final Map<pfe_broker.avro.Type, Character> quickfixTypeMap = new EnumMap<>(
        pfe_broker.avro.Type.class);

    static {
      avroTypeMap.put(quickfix.field.OrdType.MARKET, pfe_broker.avro.Type.MARKET);
      avroTypeMap.put(quickfix.field.OrdType.LIMIT, pfe_broker.avro.Type.LIMIT);

      avroTypeMap.entrySet().forEach(entry -> quickfixTypeMap.put(entry.getValue(), entry.getKey()));
    }

    public static char charFromAvro(pfe_broker.avro.Type type)  {
      if (!quickfixTypeMap.containsKey(type)) {
        throw new IllegalArgumentException(type.toString());
      }
      return quickfixTypeMap.get(type);
    }

    public static quickfix.field.OrdType fromAvro(pfe_broker.avro.Type type)  {
      return new quickfix.field.OrdType(charFromAvro(type));
    }

    public static pfe_broker.avro.Type toAvro(char type) throws IncorrectTagValue  {
      if (!avroTypeMap.containsKey(type)) {
        throw new IncorrectTagValue(quickfix.field.OrdType.FIELD, String.valueOf(type));
      }
      return avroTypeMap.get(type);
    }

    public static pfe_broker.avro.Type toAvro(quickfix.field.OrdType type) throws IncorrectTagValue  {
      return toAvro(type.getValue());
    }
  }

  public static class MarketDataEntry {

    private MarketDataEntry() {
    }

    private static final Map<Character, pfe_broker.avro.MarketDataEntry> avroMarketDataEntryMap = new HashMap<>();
    private static final Map<pfe_broker.avro.MarketDataEntry, Character> quickfixMarketDataEntryMap = new EnumMap<>(
        pfe_broker.avro.MarketDataEntry.class);

    static {
      avroMarketDataEntryMap.put(quickfix.field.MDEntryType.OPENING_PRICE, pfe_broker.avro.MarketDataEntry.OPEN);
      avroMarketDataEntryMap.put(quickfix.field.MDEntryType.CLOSING_PRICE, pfe_broker.avro.MarketDataEntry.CLOSE);
      avroMarketDataEntryMap.put(quickfix.field.MDEntryType.TRADING_SESSION_HIGH_PRICE,
          pfe_broker.avro.MarketDataEntry.HIGH);
      avroMarketDataEntryMap.put(quickfix.field.MDEntryType.TRADING_SESSION_LOW_PRICE,
          pfe_broker.avro.MarketDataEntry.LOW);

      avroMarketDataEntryMap.entrySet()
          .forEach(entry -> quickfixMarketDataEntryMap.put(entry.getValue(), entry.getKey()));
    }

    public static char charFromAvro(pfe_broker.avro.MarketDataEntry marketDataEntry) {
      if (!quickfixMarketDataEntryMap.containsKey(marketDataEntry)) {
        throw new IllegalArgumentException(marketDataEntry.toString());
      }
      return quickfixMarketDataEntryMap.get(marketDataEntry);
    }

    public static quickfix.field.MDEntryType fromAvro(pfe_broker.avro.MarketDataEntry marketDataEntry) {
      return new quickfix.field.MDEntryType(charFromAvro(marketDataEntry));
    }

    public static pfe_broker.avro.MarketDataEntry toAvro(char marketDataEntry) throws IncorrectTagValue {
      if (!avroMarketDataEntryMap.containsKey(marketDataEntry)) {
        throw new IncorrectTagValue(quickfix.field.MDEntryType.FIELD, String.valueOf(marketDataEntry));
      }
      return avroMarketDataEntryMap.get(marketDataEntry);
    }

    public static pfe_broker.avro.MarketDataEntry toAvro(quickfix.field.MDEntryType marketDataEntry) throws IncorrectTagValue {
      return toAvro(marketDataEntry.getValue());
    }
  }

  public static class MarketDataSubscriptionRequest {

    private MarketDataSubscriptionRequest() {
    }

    private static final Map<Character, pfe_broker.avro.MarketDataSubscriptionRequest> avroMarketDataRequestMap = new HashMap<>();
    private static final Map<pfe_broker.avro.MarketDataSubscriptionRequest, Character> quickfixMarketDataRequestMap = new EnumMap<>(
        pfe_broker.avro.MarketDataSubscriptionRequest.class);

    static {
      avroMarketDataRequestMap.put(quickfix.field.SubscriptionRequestType.SNAPSHOT,
          pfe_broker.avro.MarketDataSubscriptionRequest.SNAPSHOT);
      avroMarketDataRequestMap.put(quickfix.field.SubscriptionRequestType.SNAPSHOT_UPDATES,
          pfe_broker.avro.MarketDataSubscriptionRequest.SUBSCRIBE);
      avroMarketDataRequestMap.put(quickfix.field.SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_UPDATE_REQUEST,
          pfe_broker.avro.MarketDataSubscriptionRequest.UNSUBSCRIBE);

      avroMarketDataRequestMap.entrySet()
          .forEach(entry -> quickfixMarketDataRequestMap.put(entry.getValue(), entry.getKey()));
    }

    public static char charFromAvro(pfe_broker.avro.MarketDataSubscriptionRequest marketDataRequest) {
      if (!quickfixMarketDataRequestMap.containsKey(marketDataRequest)) {
        throw new IllegalArgumentException(marketDataRequest.toString());
      }
      return quickfixMarketDataRequestMap.get(marketDataRequest);
    }

    public static quickfix.field.SubscriptionRequestType fromAvro(
        pfe_broker.avro.MarketDataSubscriptionRequest marketDataRequest) {
      return new quickfix.field.SubscriptionRequestType(charFromAvro(marketDataRequest));
    }

    public static pfe_broker.avro.MarketDataSubscriptionRequest toAvro(char marketDataRequest)
        throws IncorrectTagValue {
      if (!avroMarketDataRequestMap.containsKey(marketDataRequest)) {
        throw new IncorrectTagValue(quickfix.field.SubscriptionRequestType.FIELD, String.valueOf(marketDataRequest));
      }
      return avroMarketDataRequestMap.get(marketDataRequest);
    }

    public static pfe_broker.avro.MarketDataSubscriptionRequest toAvro(
        quickfix.field.SubscriptionRequestType marketDataRequest) throws IncorrectTagValue {
      return toAvro(marketDataRequest.getValue());
    }
  }

  public static class MarketDataRejectedReason {

    private MarketDataRejectedReason() {
    }

    private static final Map<Character, pfe_broker.avro.MarketDataRejectedReason> avroMarketDataRejectedReasonMap = new HashMap<>();
    private static final Map<pfe_broker.avro.MarketDataRejectedReason, Character> quickfixMarketDataRejectedReasonMap = new EnumMap<>(
        pfe_broker.avro.MarketDataRejectedReason.class);

    static {
      avroMarketDataRejectedReasonMap.put(quickfix.field.MDReqRejReason.UNKNOWN_SYMBOL,
          pfe_broker.avro.MarketDataRejectedReason.UNKNOWN_SYMBOL);
      avroMarketDataRejectedReasonMap.put(quickfix.field.MDReqRejReason.DUPLICATE_MDREQID,
          pfe_broker.avro.MarketDataRejectedReason.DUPLICATE_MD_REQ_ID);
      avroMarketDataRejectedReasonMap.put(quickfix.field.MDReqRejReason.UNSUPPORTED_SUBSCRIPTIONREQUESTTYPE,
          pfe_broker.avro.MarketDataRejectedReason.UNSUPPORTED_SUBSCRIPTION_REQUEST_TYPE);
      avroMarketDataRejectedReasonMap.put(quickfix.field.MDReqRejReason.UNSUPPORTED_MARKETDEPTH,
          pfe_broker.avro.MarketDataRejectedReason.UNSUPPORTED_MARKET_DEPTH);
      avroMarketDataRejectedReasonMap.put(quickfix.field.MDReqRejReason.UNSUPPORTED_MDUPDATETYPE,
          pfe_broker.avro.MarketDataRejectedReason.UNSUPPORTED_MD_UPDATE_TYPE);
      avroMarketDataRejectedReasonMap.put(quickfix.field.MDReqRejReason.UNSUPPORTED_MDENTRYTYPE,
          pfe_broker.avro.MarketDataRejectedReason.UNSUPPORTED_MD_ENTRY_TYPE);

      avroMarketDataRejectedReasonMap.entrySet()
          .forEach(entry -> quickfixMarketDataRejectedReasonMap.put(entry.getValue(), entry.getKey()));
    }

    public static char charFromAvro(pfe_broker.avro.MarketDataRejectedReason marketDataRejectedReason) {
      if (!quickfixMarketDataRejectedReasonMap.containsKey(marketDataRejectedReason)) {
        throw new IllegalArgumentException(marketDataRejectedReason.toString());
      }
      return quickfixMarketDataRejectedReasonMap.get(marketDataRejectedReason);
    }

    public static quickfix.field.MDReqRejReason fromAvro(
        pfe_broker.avro.MarketDataRejectedReason marketDataRejectedReason) {
      return new quickfix.field.MDReqRejReason(charFromAvro(marketDataRejectedReason));
    }

    public static pfe_broker.avro.MarketDataRejectedReason toAvro(char marketDataRejectedReason)
        throws IncorrectTagValue {
      if (!avroMarketDataRejectedReasonMap.containsKey(marketDataRejectedReason)) {
        throw new IncorrectTagValue(quickfix.field.MDReqRejReason.FIELD, String.valueOf(marketDataRejectedReason));
      }
      return avroMarketDataRejectedReasonMap.get(marketDataRejectedReason);
    }

    public static pfe_broker.avro.MarketDataRejectedReason toAvro(
        quickfix.field.MDReqRejReason marketDataRejectedReason) throws IncorrectTagValue {
      return toAvro(marketDataRejectedReason.getValue());
    }
  }
}
