import logging
from typing import Callable

import quickfix as fix

from broker_quickfix_client.utils.quickfix import get_message_field
from broker_quickfix_client.wrappers.enums import (
    OrderRejectReasonEnum,
    OrderStatusEnum,
    OrderTypeEnum,
    SideEnum,
)
from broker_quickfix_client.wrappers.execution_report import (
    FilledExecutionReport,
    RejectedExecutionReport,
)

logger = logging.getLogger("client.execution_report_handler")


class ExecutionReportHandler:
    def __init__(
        self,
        on_filled_report: Callable[[FilledExecutionReport], None] | None = None,
        on_rejected_report: Callable[[RejectedExecutionReport], None] | None = None,
    ):
        self.fill_callback = on_filled_report
        self.reject_callback = on_rejected_report

    def handle_execution_report(self, execution_report: fix.Message):
        ord_status = OrderStatusEnum(get_message_field(execution_report, fix.OrdStatus))

        if ord_status == OrderStatusEnum.REJECTED:
            self.handle_reject(execution_report)
        elif ord_status == OrderStatusEnum.FILLED:
            self.handle_fill(execution_report)
        else:
            logger.warning(f"Unknown order status: {ord_status}")

    def handle_reject(self, execution_report: fix.Message):
        order_id = get_message_field(execution_report, fix.OrderID)
        client_order_id = get_message_field(execution_report, fix.ClOrdID)
        symbol = get_message_field(execution_report, fix.Symbol)
        side = SideEnum(get_message_field(execution_report, fix.Side))
        order_type = OrderTypeEnum(get_message_field(execution_report, fix.OrdType))
        leaves_qty = get_message_field(execution_report, fix.LeavesQty)
        reject_reason = OrderRejectReasonEnum(
            get_message_field(execution_report, fix.OrdRejReason)
        )

        rejected = RejectedExecutionReport(
            order_id=int(order_id),
            client_order_id=int(client_order_id),
            symbol=symbol,
            side=side,
            type=order_type,
            leaves_quantity=int(leaves_qty),
            reject_reason=reject_reason,
        )

        if self.reject_callback:
            self.reject_callback(rejected)
        else:
            logger.debug(f"Order rejected: {rejected}")

    def handle_fill(self, execution_report: fix.Message):
        order_id = get_message_field(execution_report, fix.OrderID)
        client_order_id = get_message_field(execution_report, fix.ClOrdID)
        symbol = get_message_field(execution_report, fix.Symbol)
        side = SideEnum(get_message_field(execution_report, fix.Side))
        order_type = OrderTypeEnum(get_message_field(execution_report, fix.OrdType))
        leaves_qty = get_message_field(execution_report, fix.LeavesQty)
        price = get_message_field(execution_report, fix.AvgPx)
        cum_quantity = get_message_field(execution_report, fix.CumQty)

        filled = FilledExecutionReport(
            order_id=int(order_id),
            client_order_id=int(client_order_id),
            symbol=symbol,
            side=side,
            type=order_type,
            leaves_quantity=int(leaves_qty),
            price=float(price),
            cum_quantity=int(cum_quantity),
        )
        if self.fill_callback:
            self.fill_callback(filled)
        else:
            logger.debug(f"Order filled: {filled}")
