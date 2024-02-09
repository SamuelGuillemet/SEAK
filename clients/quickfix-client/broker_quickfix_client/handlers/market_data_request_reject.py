import logging
from typing import Callable, TypeVar

import quickfix as fix

from broker_quickfix_client.utils.quickfix import get_message_field
from broker_quickfix_client.wrappers.enums import MarketDataRejectReasonEnum
from broker_quickfix_client.wrappers.market_data import MarketDataRequestReject

logger = logging.getLogger("client.market_data_request_reject_handler")

T = TypeVar("T", bound=MarketDataRequestReject)
CallbackType = Callable[[T], None] | None


class MarketDataRequestRejectHandler:
    def __init__(
        self,
        market_data_request_reject_callback: CallbackType[
            MarketDataRequestReject
        ] = None,
    ):
        self.market_data_request_reject_callback = market_data_request_reject_callback

    def handle_market_data_request_reject(
        self, market_data_request_reject: fix.Message
    ):
        common_fields = self._extract_common_fields(market_data_request_reject)
        rejected = MarketDataRequestReject(**common_fields)

        if self.market_data_request_reject_callback:
            self.market_data_request_reject_callback(rejected)
        else:
            logger.warning(f"Market data request reject received: {rejected}")

    def _extract_common_fields(self, market_data_request_reject: fix.Message) -> dict:
        return {
            "md_req_id": int(
                get_message_field(market_data_request_reject, fix.MDReqID)
            ),
            "reject_reason": MarketDataRejectReasonEnum(
                get_message_field(market_data_request_reject, fix.MDReqRejReason)
            ),
        }
