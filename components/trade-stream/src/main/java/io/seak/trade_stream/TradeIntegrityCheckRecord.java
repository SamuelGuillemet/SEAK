package io.seak.trade_stream;

import io.seak.avro.OrderRejectReason;
import io.seak.avro.Trade;

public record TradeIntegrityCheckRecord(
  Trade trade,
  OrderRejectReason orderRejectReason
) {}
