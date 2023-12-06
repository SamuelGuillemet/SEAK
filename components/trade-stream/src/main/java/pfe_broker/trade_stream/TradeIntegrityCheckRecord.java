package pfe_broker.trade_stream;

import pfe_broker.avro.OrderRejectReason;
import pfe_broker.avro.Trade;

public record TradeIntegrityCheckRecord(
  Trade trade,
  OrderRejectReason orderRejectReason
) {}
