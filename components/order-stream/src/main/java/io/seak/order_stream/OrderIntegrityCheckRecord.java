package io.seak.order_stream;

import io.seak.avro.Order;
import io.seak.avro.OrderRejectReason;

public record OrderIntegrityCheckRecord(
  Order order,
  OrderRejectReason orderRejectReason
) {}
