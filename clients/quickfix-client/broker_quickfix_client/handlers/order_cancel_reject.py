import logging
from typing import Callable, TypeVar

import quickfix as fix

from broker_quickfix_client.utils.quickfix import get_message_field
from broker_quickfix_client.wrappers.enums import CxlRejResponseToEnum
from broker_quickfix_client.wrappers.order_cancel_reject import OrderCancelReject

logger = logging.getLogger("client.order_cancel_reject_handler")

T = TypeVar("T", bound=OrderCancelReject)
CallbackType = Callable[[T], None] | None


class OrderCancelRejectHandler:
    def __init__(
        self,
        order_cancel_rejected_callback: CallbackType[OrderCancelReject] = None,
        order_cancel_replace_rejected_callback: CallbackType[OrderCancelReject] = None,
    ):
        self.order_cancel_rejected_callback = order_cancel_rejected_callback
        self.order_cancel_replace_rejected_callback = (
            order_cancel_replace_rejected_callback
        )

    def handle_order_cancel_reject(self, order_cancel_reject: fix.Message):
        cancel_reject_response_to = CxlRejResponseToEnum(
            get_message_field(order_cancel_reject, fix.CxlRejResponseTo)
        )

        if cancel_reject_response_to == CxlRejResponseToEnum.ORDER_CANCEL_REQUEST:
            self._handle_order_cancel_rejected(order_cancel_reject)
        elif (
            cancel_reject_response_to
            == CxlRejResponseToEnum.ORDER_CANCEL_REPLACE_REQUEST
        ):
            self._handle_order_cancel_replace_rejected(order_cancel_reject)
        else:
            raise ValueError(
                f"Unknown cancel reject response to: {cancel_reject_response_to}"
            )

    def _extract_common_fields(self, order_cancel_reject: fix.Message) -> dict:
        return {
            "order_id": int(get_message_field(order_cancel_reject, fix.OrderID)),
            "client_order_id": int(get_message_field(order_cancel_reject, fix.ClOrdID)),
            "original_client_order_id": int(
                get_message_field(order_cancel_reject, fix.OrigClOrdID)
            ),
        }

    def _handle_order_cancel_rejected(self, order_cancel_reject: fix.Message):
        common_fields = self._extract_common_fields(order_cancel_reject)

        rejected = OrderCancelReject(**common_fields)

        if self.order_cancel_rejected_callback:
            self.order_cancel_rejected_callback(rejected)
        else:
            logger.warning(f"Order book modification rejected: {rejected}")

    def _handle_order_cancel_replace_rejected(self, order_cancel_reject: fix.Message):
        common_fields = self._extract_common_fields(order_cancel_reject)

        rejected = OrderCancelReject(**common_fields)

        if self.order_cancel_replace_rejected_callback:
            self.order_cancel_replace_rejected_callback(rejected)
        else:
            logger.warning(f"Order book modification rejected: {rejected}")
