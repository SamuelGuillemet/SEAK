import logging
from typing import Callable, TypeVar

import quickfix as fix
import quickfix44 as fix44

from broker_quickfix_client.utils.quickfix import get_message_field
from broker_quickfix_client.wrappers.enums import MarketDataEntryTypeEnum
from broker_quickfix_client.wrappers.market_data import (
    MarketDataDetails,
    MarketDataResponse,
)

logger = logging.getLogger("client.market_data_response_handler")

T = TypeVar("T", bound=MarketDataResponse)
CallbackType = Callable[[T], None] | None


class MarketDataSnapshotFullRefreshHandler:
    def __init__(
        self,
        market_data_snapshot_full_refresh_callback: CallbackType[
            MarketDataResponse
        ] = None,
    ):
        self.market_data_snapshot_full_refresh_callback = (
            market_data_snapshot_full_refresh_callback
        )

    def handle_market_data_snapshot_full_refresh(
        self, market_data_snapshot_full_refresh: fix.Message
    ):
        common_fields = self._extract_common_fields(market_data_snapshot_full_refresh)
        market_data: dict[int, list[MarketDataDetails]] = {}
        for i in range(
            int(get_message_field(market_data_snapshot_full_refresh, fix.NoMDEntries))
        ):
            group = fix44.MarketDataSnapshotFullRefresh.NoMDEntries()
            market_data_snapshot_full_refresh.getGroup(i + 1, group)
            position_no = int(get_message_field(group, fix.MDEntryPositionNo))
            if position_no not in market_data:
                market_data[position_no] = []

            market_data[position_no].append(
                MarketDataDetails(
                    md_entry_type=MarketDataEntryTypeEnum(
                        get_message_field(group, fix.MDEntryType)
                    ),
                    md_entry_px=float(get_message_field(group, fix.MDEntryPx)),
                )
            )

        snapshot = MarketDataResponse(**common_fields, market_data=market_data)

        if self.market_data_snapshot_full_refresh_callback:
            self.market_data_snapshot_full_refresh_callback(snapshot)
        else:
            logger.warning(f"Market data snapshot full refresh received: {snapshot}")

    def _extract_common_fields(self, market_data_snapshot_full_refresh: fix.Message):
        return {
            "md_req_id": int(
                get_message_field(market_data_snapshot_full_refresh, fix.MDReqID)
            ),
            "symbol": get_message_field(market_data_snapshot_full_refresh, fix.Symbol),
        }
