package pfe_broker.order_stream;

import pfe_broker.avro.Order;
import pfe_broker.avro.OrderRejectReason;

public record OrderIntegrityCheckRecord(
  Order order,
  OrderRejectReason orderRejectReason
) {}
