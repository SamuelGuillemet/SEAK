package io.seak.avro.utils;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import quickfix.IncorrectTagValue;

public class Converters {

  private Converters() {}

  public static class Side {

    private static final Map<Character, io.seak.avro.Side> avroSideMap =
      new HashMap<>();
    private static final Map<io.seak.avro.Side, Character> quickfixSideMap =
      new EnumMap<>(io.seak.avro.Side.class);

    private Side() {}

    static {
      avroSideMap.put(quickfix.field.Side.BUY, io.seak.avro.Side.BUY);
      avroSideMap.put(quickfix.field.Side.SELL, io.seak.avro.Side.SELL);

      avroSideMap
        .entrySet()
        .forEach(entry -> quickfixSideMap.put(entry.getValue(), entry.getKey())
        );
    }

    public static char charFromAvro(io.seak.avro.Side side) {
      if (!quickfixSideMap.containsKey(side)) {
        throw new IllegalArgumentException(side.toString());
      }
      return quickfixSideMap.get(side);
    }

    public static quickfix.field.Side fromAvro(io.seak.avro.Side side) {
      return new quickfix.field.Side(charFromAvro(side));
    }

    public static io.seak.avro.Side toAvro(char side) throws IncorrectTagValue {
      if (!avroSideMap.containsKey(side)) {
        throw new IncorrectTagValue(
          quickfix.field.Side.FIELD,
          String.valueOf(side)
        );
      }
      return avroSideMap.get(side);
    }

    public static io.seak.avro.Side toAvro(quickfix.field.Side side)
      throws IncorrectTagValue {
      return toAvro(side.getValue());
    }
  }

  public static class OrderRejectReason {

    private OrderRejectReason() {}

    private static final Map<
      Integer,
      io.seak.avro.OrderRejectReason
    > avroReasonMap = new HashMap<>();
    private static final Map<
      io.seak.avro.OrderRejectReason,
      Integer
    > quickfixReasonMap = new EnumMap<>(io.seak.avro.OrderRejectReason.class);

    static {
      avroReasonMap.put(
        quickfix.field.OrdRejReason.BROKER_EXCHANGE_OPTION,
        io.seak.avro.OrderRejectReason.BROKER_EXCHANGE_OPTION
      );
      avroReasonMap.put(
        quickfix.field.OrdRejReason.UNKNOWN_SYMBOL,
        io.seak.avro.OrderRejectReason.UNKNOWN_SYMBOL
      );
      avroReasonMap.put(
        quickfix.field.OrdRejReason.EXCHANGE_CLOSED,
        io.seak.avro.OrderRejectReason.EXCHANGE_CLOSED
      );
      avroReasonMap.put(
        quickfix.field.OrdRejReason.ORDER_EXCEEDS_LIMIT,
        io.seak.avro.OrderRejectReason.ORDER_EXCEEDS_LIMIT
      );
      avroReasonMap.put(
        quickfix.field.OrdRejReason.TOO_LATE_TO_ENTER,
        io.seak.avro.OrderRejectReason.TOO_LATE_TO_ENTER
      );
      avroReasonMap.put(
        quickfix.field.OrdRejReason.UNKNOWN_ORDER,
        io.seak.avro.OrderRejectReason.UNKNOWN_ORDER
      );
      avroReasonMap.put(
        quickfix.field.OrdRejReason.DUPLICATE_ORDER,
        io.seak.avro.OrderRejectReason.DUPLICATE_ORDER
      );
      avroReasonMap.put(
        quickfix.field.OrdRejReason.STALE_ORDER,
        io.seak.avro.OrderRejectReason.STALE_ORDER
      );
      avroReasonMap.put(
        quickfix.field.OrdRejReason.INCORRECT_QUANTITY,
        io.seak.avro.OrderRejectReason.INCORRECT_QUANTITY
      );
      avroReasonMap.put(
        quickfix.field.OrdRejReason.UNKNOWN_ACCOUNT,
        io.seak.avro.OrderRejectReason.UNKNOWN_ACCOUNT
      );
      avroReasonMap.put(
        quickfix.field.OrdRejReason.PRICE_EXCEEDS_CURRENT_PRICE_BAND,
        io.seak.avro.OrderRejectReason.PRICE_EXCEEDS_CURRENT_PRICE_BAND
      );

      avroReasonMap
        .entrySet()
        .forEach(entry ->
          quickfixReasonMap.put(entry.getValue(), entry.getKey())
        );
    }

    public static int intFromAvro(io.seak.avro.OrderRejectReason reason) {
      return quickfixReasonMap.getOrDefault(
        reason,
        quickfix.field.OrdRejReason.OTHER
      );
    }

    public static quickfix.field.OrdRejReason fromAvro(
      io.seak.avro.OrderRejectReason reason
    ) {
      return new quickfix.field.OrdRejReason(intFromAvro(reason));
    }

    public static io.seak.avro.OrderRejectReason toAvro(int reason) {
      return avroReasonMap.getOrDefault(
        reason,
        io.seak.avro.OrderRejectReason.OTHER
      );
    }

    public static io.seak.avro.OrderRejectReason toAvro(
      quickfix.field.OrdRejReason reason
    ) {
      return toAvro(reason.getValue());
    }
  }

  public static class Type {

    private Type() {}

    private static final Map<Character, io.seak.avro.Type> avroTypeMap =
      new HashMap<>();
    private static final Map<io.seak.avro.Type, Character> quickfixTypeMap =
      new EnumMap<>(io.seak.avro.Type.class);

    static {
      avroTypeMap.put(quickfix.field.OrdType.MARKET, io.seak.avro.Type.MARKET);
      avroTypeMap.put(quickfix.field.OrdType.LIMIT, io.seak.avro.Type.LIMIT);

      avroTypeMap
        .entrySet()
        .forEach(entry -> quickfixTypeMap.put(entry.getValue(), entry.getKey())
        );
    }

    public static char charFromAvro(io.seak.avro.Type type) {
      if (!quickfixTypeMap.containsKey(type)) {
        throw new IllegalArgumentException(type.toString());
      }
      return quickfixTypeMap.get(type);
    }

    public static quickfix.field.OrdType fromAvro(io.seak.avro.Type type) {
      return new quickfix.field.OrdType(charFromAvro(type));
    }

    public static io.seak.avro.Type toAvro(char type) throws IncorrectTagValue {
      if (!avroTypeMap.containsKey(type)) {
        throw new IncorrectTagValue(
          quickfix.field.OrdType.FIELD,
          String.valueOf(type)
        );
      }
      return avroTypeMap.get(type);
    }

    public static io.seak.avro.Type toAvro(quickfix.field.OrdType type)
      throws IncorrectTagValue {
      return toAvro(type.getValue());
    }
  }

  public static class MarketDataEntry {

    private MarketDataEntry() {}

    private static final Map<
      Character,
      io.seak.avro.MarketDataEntry
    > avroMarketDataEntryMap = new HashMap<>();
    private static final Map<
      io.seak.avro.MarketDataEntry,
      Character
    > quickfixMarketDataEntryMap = new EnumMap<>(
      io.seak.avro.MarketDataEntry.class
    );

    static {
      avroMarketDataEntryMap.put(
        quickfix.field.MDEntryType.OPENING_PRICE,
        io.seak.avro.MarketDataEntry.OPEN
      );
      avroMarketDataEntryMap.put(
        quickfix.field.MDEntryType.CLOSING_PRICE,
        io.seak.avro.MarketDataEntry.CLOSE
      );
      avroMarketDataEntryMap.put(
        quickfix.field.MDEntryType.TRADING_SESSION_HIGH_PRICE,
        io.seak.avro.MarketDataEntry.HIGH
      );
      avroMarketDataEntryMap.put(
        quickfix.field.MDEntryType.TRADING_SESSION_LOW_PRICE,
        io.seak.avro.MarketDataEntry.LOW
      );

      avroMarketDataEntryMap
        .entrySet()
        .forEach(entry ->
          quickfixMarketDataEntryMap.put(entry.getValue(), entry.getKey())
        );
    }

    public static char charFromAvro(
      io.seak.avro.MarketDataEntry marketDataEntry
    ) {
      if (!quickfixMarketDataEntryMap.containsKey(marketDataEntry)) {
        throw new IllegalArgumentException(marketDataEntry.toString());
      }
      return quickfixMarketDataEntryMap.get(marketDataEntry);
    }

    public static quickfix.field.MDEntryType fromAvro(
      io.seak.avro.MarketDataEntry marketDataEntry
    ) {
      return new quickfix.field.MDEntryType(charFromAvro(marketDataEntry));
    }

    public static io.seak.avro.MarketDataEntry toAvro(char marketDataEntry)
      throws IncorrectTagValue {
      if (!avroMarketDataEntryMap.containsKey(marketDataEntry)) {
        throw new IncorrectTagValue(
          quickfix.field.MDEntryType.FIELD,
          String.valueOf(marketDataEntry)
        );
      }
      return avroMarketDataEntryMap.get(marketDataEntry);
    }

    public static io.seak.avro.MarketDataEntry toAvro(
      quickfix.field.MDEntryType marketDataEntry
    ) throws IncorrectTagValue {
      return toAvro(marketDataEntry.getValue());
    }
  }

  public static class MarketDataSubscriptionRequest {

    private MarketDataSubscriptionRequest() {}

    private static final Map<
      Character,
      io.seak.avro.MarketDataSubscriptionRequest
    > avroMarketDataRequestMap = new HashMap<>();
    private static final Map<
      io.seak.avro.MarketDataSubscriptionRequest,
      Character
    > quickfixMarketDataRequestMap = new EnumMap<>(
      io.seak.avro.MarketDataSubscriptionRequest.class
    );

    static {
      avroMarketDataRequestMap.put(
        quickfix.field.SubscriptionRequestType.SNAPSHOT,
        io.seak.avro.MarketDataSubscriptionRequest.SNAPSHOT
      );
      avroMarketDataRequestMap.put(
        quickfix.field.SubscriptionRequestType.SNAPSHOT_UPDATES,
        io.seak.avro.MarketDataSubscriptionRequest.SUBSCRIBE
      );
      avroMarketDataRequestMap.put(
        quickfix.field.SubscriptionRequestType.DISABLE_PREVIOUS_SNAPSHOT_UPDATE_REQUEST,
        io.seak.avro.MarketDataSubscriptionRequest.UNSUBSCRIBE
      );

      avroMarketDataRequestMap
        .entrySet()
        .forEach(entry ->
          quickfixMarketDataRequestMap.put(entry.getValue(), entry.getKey())
        );
    }

    public static char charFromAvro(
      io.seak.avro.MarketDataSubscriptionRequest marketDataRequest
    ) {
      if (!quickfixMarketDataRequestMap.containsKey(marketDataRequest)) {
        throw new IllegalArgumentException(marketDataRequest.toString());
      }
      return quickfixMarketDataRequestMap.get(marketDataRequest);
    }

    public static quickfix.field.SubscriptionRequestType fromAvro(
      io.seak.avro.MarketDataSubscriptionRequest marketDataRequest
    ) {
      return new quickfix.field.SubscriptionRequestType(
        charFromAvro(marketDataRequest)
      );
    }

    public static io.seak.avro.MarketDataSubscriptionRequest toAvro(
      char marketDataRequest
    ) throws IncorrectTagValue {
      if (!avroMarketDataRequestMap.containsKey(marketDataRequest)) {
        throw new IncorrectTagValue(
          quickfix.field.SubscriptionRequestType.FIELD,
          String.valueOf(marketDataRequest)
        );
      }
      return avroMarketDataRequestMap.get(marketDataRequest);
    }

    public static io.seak.avro.MarketDataSubscriptionRequest toAvro(
      quickfix.field.SubscriptionRequestType marketDataRequest
    ) throws IncorrectTagValue {
      return toAvro(marketDataRequest.getValue());
    }
  }

  public static class MarketDataRejectedReason {

    private MarketDataRejectedReason() {}

    private static final Map<
      Character,
      io.seak.avro.MarketDataRejectedReason
    > avroMarketDataRejectedReasonMap = new HashMap<>();
    private static final Map<
      io.seak.avro.MarketDataRejectedReason,
      Character
    > quickfixMarketDataRejectedReasonMap = new EnumMap<>(
      io.seak.avro.MarketDataRejectedReason.class
    );

    static {
      avroMarketDataRejectedReasonMap.put(
        quickfix.field.MDReqRejReason.UNKNOWN_SYMBOL,
        io.seak.avro.MarketDataRejectedReason.UNKNOWN_SYMBOL
      );
      avroMarketDataRejectedReasonMap.put(
        quickfix.field.MDReqRejReason.DUPLICATE_MDREQID,
        io.seak.avro.MarketDataRejectedReason.DUPLICATE_MD_REQ_ID
      );
      avroMarketDataRejectedReasonMap.put(
        quickfix.field.MDReqRejReason.UNSUPPORTED_SUBSCRIPTIONREQUESTTYPE,
        io.seak.avro.MarketDataRejectedReason.UNSUPPORTED_SUBSCRIPTION_REQUEST_TYPE
      );
      avroMarketDataRejectedReasonMap.put(
        quickfix.field.MDReqRejReason.UNSUPPORTED_MARKETDEPTH,
        io.seak.avro.MarketDataRejectedReason.UNSUPPORTED_MARKET_DEPTH
      );
      avroMarketDataRejectedReasonMap.put(
        quickfix.field.MDReqRejReason.UNSUPPORTED_MDUPDATETYPE,
        io.seak.avro.MarketDataRejectedReason.UNSUPPORTED_MD_UPDATE_TYPE
      );
      avroMarketDataRejectedReasonMap.put(
        quickfix.field.MDReqRejReason.UNSUPPORTED_MDENTRYTYPE,
        io.seak.avro.MarketDataRejectedReason.UNSUPPORTED_MD_ENTRY_TYPE
      );

      avroMarketDataRejectedReasonMap
        .entrySet()
        .forEach(entry ->
          quickfixMarketDataRejectedReasonMap.put(
            entry.getValue(),
            entry.getKey()
          )
        );
    }

    public static char charFromAvro(
      io.seak.avro.MarketDataRejectedReason marketDataRejectedReason
    ) {
      if (
        !quickfixMarketDataRejectedReasonMap.containsKey(
          marketDataRejectedReason
        )
      ) {
        throw new IllegalArgumentException(marketDataRejectedReason.toString());
      }
      return quickfixMarketDataRejectedReasonMap.get(marketDataRejectedReason);
    }

    public static quickfix.field.MDReqRejReason fromAvro(
      io.seak.avro.MarketDataRejectedReason marketDataRejectedReason
    ) {
      return new quickfix.field.MDReqRejReason(
        charFromAvro(marketDataRejectedReason)
      );
    }

    public static io.seak.avro.MarketDataRejectedReason toAvro(
      char marketDataRejectedReason
    ) throws IncorrectTagValue {
      if (
        !avroMarketDataRejectedReasonMap.containsKey(marketDataRejectedReason)
      ) {
        throw new IncorrectTagValue(
          quickfix.field.MDReqRejReason.FIELD,
          String.valueOf(marketDataRejectedReason)
        );
      }
      return avroMarketDataRejectedReasonMap.get(marketDataRejectedReason);
    }

    public static io.seak.avro.MarketDataRejectedReason toAvro(
      quickfix.field.MDReqRejReason marketDataRejectedReason
    ) throws IncorrectTagValue {
      return toAvro(marketDataRejectedReason.getValue());
    }
  }
}
