import logging
from typing import Callable, TypeVar

import quickfix as fix

from broker_quickfix_client.utils.quickfix import get_message_field
from broker_quickfix_client.wrappers.enums import (
    ExecTypeEnum,
    OrderRejectReasonEnum,
    OrderStatusEnum,
    OrderTypeEnum,
    SideEnum,
)
from broker_quickfix_client.wrappers.execution_report import (
    AcceptedOrderExecutionReport,
    BaseExecutionReport,
    CanceledOrderExecutionReport,
    FilledExecutionReport,
    RejectedExecutionReport,
    ReplacedOrderExecutionReport,
)

logger = logging.getLogger("client.execution_report_handler")

T = TypeVar("T", bound=BaseExecutionReport)
CallbackType = Callable[[T], None] | None


class ExecutionReportHandler:
    def __init__(
        self,
        order_filled_callback: CallbackType[FilledExecutionReport] = None,
        order_rejected_callback: CallbackType[RejectedExecutionReport] = None,
        order_accepted_callback: CallbackType[AcceptedOrderExecutionReport] = None,
        order_replaced_callback: CallbackType[ReplacedOrderExecutionReport] = None,
        order_canceled_callback: CallbackType[CanceledOrderExecutionReport] = None,
    ):
        self.order_filled_callback = order_filled_callback
        self.order_rejected_callback = order_rejected_callback
        self.order_accepted_callback = order_accepted_callback
        self.order_replaced_callback = order_replaced_callback
        self.order_canceled_callback = order_canceled_callback

    def handle_execution_report(self, execution_report: fix.Message):
        ord_status = OrderStatusEnum(get_message_field(execution_report, fix.OrdStatus))
        exec_type = ExecTypeEnum(get_message_field(execution_report, fix.ExecType))

        if (
            ord_status == OrderStatusEnum.REJECTED
            and exec_type == ExecTypeEnum.REJECTED
        ):
            self._handle_order_rejected(execution_report)
        elif ord_status == OrderStatusEnum.FILLED and exec_type == ExecTypeEnum.TRADE:
            self._handle_order_filled(execution_report)
        elif ord_status == OrderStatusEnum.NEW and exec_type == ExecTypeEnum.NEW:
            self._handle_order_accepted(execution_report)
        elif ord_status == OrderStatusEnum.NEW and exec_type == ExecTypeEnum.REPLACED:
            self._handle_order_replaced(execution_report)
        elif (
            ord_status == OrderStatusEnum.CANCELED
            and exec_type == ExecTypeEnum.CANCELED
        ):
            self._handle_order_canceled(execution_report)
        else:
            logger.warning(f"Unsupported execution report for {ord_status} {exec_type}")

    def _extract_common_fields(self, execution_report):
        return {
            "order_id": int(get_message_field(execution_report, fix.OrderID)),
            "client_order_id": int(get_message_field(execution_report, fix.ClOrdID)),
            "symbol": get_message_field(execution_report, fix.Symbol),
            "side": SideEnum(get_message_field(execution_report, fix.Side)),
            "type": OrderTypeEnum(get_message_field(execution_report, fix.OrdType)),
            "leaves_quantity": int(get_message_field(execution_report, fix.LeavesQty)),
        }

    def _handle_order_rejected(self, execution_report: fix.Message):
        common_fields = self._extract_common_fields(execution_report)
        reject_reason = OrderRejectReasonEnum(
            get_message_field(execution_report, fix.OrdRejReason)
        )

        rejected = RejectedExecutionReport(
            **common_fields,
            reject_reason=reject_reason,
        )

        if self.order_rejected_callback:
            self.order_rejected_callback(rejected)
        else:
            logger.debug(f"Order rejected: {rejected}")

    def _handle_order_filled(self, execution_report: fix.Message):
        common_fields = self._extract_common_fields(execution_report)
        price = get_message_field(execution_report, fix.AvgPx)
        cum_quantity = get_message_field(execution_report, fix.CumQty)

        filled = FilledExecutionReport(
            **common_fields,
            price=float(price),
            cum_quantity=int(cum_quantity),
        )
        if self.order_filled_callback:
            self.order_filled_callback(filled)
        else:
            logger.debug(f"Order filled: {filled}")

    def _handle_order_accepted(self, execution_report: fix.Message):
        common_fields = self._extract_common_fields(execution_report)
        price = get_message_field(execution_report, fix.AvgPx)

        accepted = AcceptedOrderExecutionReport(
            **common_fields,
            price=float(price),
        )

        if self.order_accepted_callback:
            self.order_accepted_callback(accepted)
        else:
            logger.debug(f"Order accepted: {accepted}")

    def _handle_order_replaced(self, execution_report: fix.Message):
        common_fields = self._extract_common_fields(execution_report)
        price = get_message_field(execution_report, fix.AvgPx)
        original_client_order_id = get_message_field(execution_report, fix.OrigClOrdID)

        replaced = ReplacedOrderExecutionReport(
            **common_fields,
            price=float(price),
            original_client_order_id=int(original_client_order_id),
        )

        if self.order_replaced_callback:
            self.order_replaced_callback(replaced)
        else:
            logger.debug(f"Order replaced: {replaced}")

    def _handle_order_canceled(self, execution_report: fix.Message):
        common_fields = self._extract_common_fields(execution_report)
        original_client_order_id = get_message_field(execution_report, fix.OrigClOrdID)

        canceled = CanceledOrderExecutionReport(
            **common_fields,
            original_client_order_id=int(original_client_order_id),
            price=0,
        )

        if self.order_canceled_callback:
            self.order_canceled_callback(canceled)
        else:
            logger.debug(f"Order canceled: {canceled}")
