from dataclasses import dataclass

from broker_quickfix_client.wrappers.enums import (
    MarketDataEntryTypeEnum,
    MarketDataRejectReasonEnum,
    MarketDataSubscriptionRequestTypeEnum,
)


@dataclass
class MarketDataReq:
    md_req_id: int
    subscription_request_type: MarketDataSubscriptionRequestTypeEnum
    market_depth: int
    symbols: list[str]
    md_entry_types: list[MarketDataEntryTypeEnum]


@dataclass
class MarketDataDetails:
    md_entry_type: MarketDataEntryTypeEnum
    md_entry_px: float


@dataclass
class MarketDataResponse:
    md_req_id: int
    symbol: str
    market_data: dict[
        int, list[MarketDataDetails]
    ]  # int stands for the order (1 == most recent)


@dataclass
class MarketDataRequestReject:
    md_req_id: int
    reject_reason: MarketDataRejectReasonEnum
