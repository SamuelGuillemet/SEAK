package pfe_broker.avro.utils;

import java.util.HashMap;
import java.util.Map;

public class Converters {
  public static class Side {

    private static final Map<Character, pfe_broker.avro.Side> avroSideMap = new HashMap<>();
    private static final Map<pfe_broker.avro.Side, Character> quickfixSideMap = new HashMap<>();

    static {
      avroSideMap.put(quickfix.field.Side.BUY, pfe_broker.avro.Side.BUY);
      avroSideMap.put(quickfix.field.Side.SELL, pfe_broker.avro.Side.SELL);

      avroSideMap.entrySet().forEach(entry -> quickfixSideMap.put(entry.getValue(), entry.getKey()));
    }

    public static char charFromAvro(pfe_broker.avro.Side side) {
      if (!quickfixSideMap.containsKey(side)) {
        throw new IllegalArgumentException("Unknown side: " + side);
      }
      return quickfixSideMap.get(side);
    }

    public static quickfix.field.Side fromAvro(pfe_broker.avro.Side side) {
      return new quickfix.field.Side(charFromAvro(side));
    }
    public static pfe_broker.avro.Side toAvro(char side) {
      if (!avroSideMap.containsKey(side)) {
        throw new IllegalArgumentException("Unknown side: " + side);
      }
      return avroSideMap.get(side);
    }

    public static pfe_broker.avro.Side toAvro(quickfix.field.Side side) {
      return toAvro(side.getValue());
    }
  }

  public static class OrderRejectReason {

    private static final Map<Integer, pfe_broker.avro.OrderRejectReason> avroReasonMap = new HashMap<>();
    private static final Map<pfe_broker.avro.OrderRejectReason, Integer> quickfixReasonMap = new HashMap<>();

    static {
      avroReasonMap.put(quickfix.field.OrdRejReason.BROKER_EXCHANGE_OPTION, pfe_broker.avro.OrderRejectReason.BROKER_EXCHANGE_OPTION);
      avroReasonMap.put(quickfix.field.OrdRejReason.UNKNOWN_SYMBOL, pfe_broker.avro.OrderRejectReason.UNKNOWN_SYMBOL);
      avroReasonMap.put(quickfix.field.OrdRejReason.EXCHANGE_CLOSED, pfe_broker.avro.OrderRejectReason.EXCHANGE_CLOSED);
      avroReasonMap.put(quickfix.field.OrdRejReason.ORDER_EXCEEDS_LIMIT, pfe_broker.avro.OrderRejectReason.ORDER_EXCEEDS_LIMIT);
      avroReasonMap.put(quickfix.field.OrdRejReason.TOO_LATE_TO_ENTER, pfe_broker.avro.OrderRejectReason.TOO_LATE_TO_ENTER);
      avroReasonMap.put(quickfix.field.OrdRejReason.UNKNOWN_ORDER, pfe_broker.avro.OrderRejectReason.UNKNOWN_ORDER);
      avroReasonMap.put(quickfix.field.OrdRejReason.DUPLICATE_ORDER, pfe_broker.avro.OrderRejectReason.DUPLICATE_ORDER);
      avroReasonMap.put(quickfix.field.OrdRejReason.STALE_ORDER, pfe_broker.avro.OrderRejectReason.STALE_ORDER);
      avroReasonMap.put(quickfix.field.OrdRejReason.INCORRECT_QUANTITY, pfe_broker.avro.OrderRejectReason.INCORRECT_QUANTITY);
      avroReasonMap.put(quickfix.field.OrdRejReason.UNKNOWN_ACCOUNT, pfe_broker.avro.OrderRejectReason.UNKNOWN_ACCOUNT);
      avroReasonMap.put(quickfix.field.OrdRejReason.PRICE_EXCEEDS_CURRENT_PRICE_BAND, pfe_broker.avro.OrderRejectReason.PRICE_EXCEEDS_CURRENT_PRICE_BAND);

      avroReasonMap.entrySet().forEach(entry -> quickfixReasonMap.put(entry.getValue(), entry.getKey()));
    }

    public static int charFromAvro(pfe_broker.avro.OrderRejectReason reason) {
      return quickfixReasonMap.getOrDefault(reason, quickfix.field.OrdRejReason.OTHER);
    }

    public static quickfix.field.OrdRejReason fromAvro(pfe_broker.avro.OrderRejectReason reason) {
      return new quickfix.field.OrdRejReason(charFromAvro(reason));
    }

    public static pfe_broker.avro.OrderRejectReason toAvro(int reason) {
      return avroReasonMap.getOrDefault(reason, pfe_broker.avro.OrderRejectReason.OTHER);
    }

    public static pfe_broker.avro.OrderRejectReason toAvro(quickfix.field.OrdRejReason reason) {
      return toAvro(reason.getValue());
    }
  }
}
