import quickfix44
from quickfix import (
    MarketDepth,
    MDEntryType,
    MDReqID,
    NoMDEntryTypes,
    NoRelatedSym,
    SubscriptionRequestType,
    Symbol,
)

from broker_quickfix_client.utils.quickfix import extract_group_field, get_message_field
from broker_quickfix_client.wrappers.enums import (
    MarketDataEntryTypeEnum,
    MarketDataSubscriptionRequestTypeEnum,
)
from broker_quickfix_client.wrappers.market_data import MarketDataReq


class MarketDataRequest(quickfix44.MarketDataRequest):
    def __init__(
        self,
        mDReqID: MDReqID,
        subscriptionRequestType: SubscriptionRequestType,
        marketDepth: MarketDepth,
        symbols: list[Symbol],
        mDEntryTypes: list[MDEntryType],
    ):
        super().__init__()
        self.setField(mDReqID)
        self.setField(subscriptionRequestType)
        self.setField(marketDepth)

        for symbol in symbols:
            related_symbol = quickfix44.MarketDataRequest.NoRelatedSym()
            related_symbol.setField(symbol)
            self.addGroup(related_symbol)

        for md_entry_type in mDEntryTypes:
            no_md_entry_type = quickfix44.MarketDataRequest.NoMDEntryTypes()
            no_md_entry_type.setField(md_entry_type)
            self.addGroup(no_md_entry_type)

    def get_market_data_request(self) -> MarketDataReq:
        return MarketDataReq(
            md_req_id=int(get_message_field(self, MDReqID)),
            subscription_request_type=MarketDataSubscriptionRequestTypeEnum(
                get_message_field(self, SubscriptionRequestType)
            ),
            market_depth=int(get_message_field(self, MarketDepth)),
            symbols=extract_group_field(
                self, Symbol, NoRelatedSym, quickfix44.MarketDataRequest.NoRelatedSym
            ),
            md_entry_types=[
                MarketDataEntryTypeEnum(mdet)
                for mdet in extract_group_field(
                    self,
                    MDEntryType,
                    NoMDEntryTypes,
                    quickfix44.MarketDataRequest.NoMDEntryTypes,
                )
            ],
        )

    @staticmethod
    def _new_market_data_request(
        md_req_id: int,
        subscription_request_type: MarketDataSubscriptionRequestTypeEnum,
        market_depth: int,
        symbols: list[str],
        md_entry_types: list[MarketDataEntryTypeEnum],
    ) -> "MarketDataRequest":
        if market_depth < 0 or market_depth > 10:
            raise ValueError("market_depth must be between 0 and 10")

        return MarketDataRequest(
            MDReqID(str(md_req_id)),
            SubscriptionRequestType(subscription_request_type.value),
            MarketDepth(market_depth),
            [Symbol(symbol) for symbol in symbols],
            [
                MDEntryType(no_md_entry_type.value)
                for no_md_entry_type in md_entry_types
            ],
        )

    @staticmethod
    def new_snapshot_request(
        md_req_id: int,
        market_depth: int,
        symbols: list[str],
        md_entry_types: list[MarketDataEntryTypeEnum],
    ) -> "MarketDataRequest":
        return MarketDataRequest._new_market_data_request(
            md_req_id,
            MarketDataSubscriptionRequestTypeEnum.SNAPSHOT,
            market_depth,
            symbols,
            md_entry_types,
        )

    @staticmethod
    def new_subscribe_request(
        md_req_id: int,
        symbols: list[str],
        md_entry_types: list[MarketDataEntryTypeEnum],
    ) -> "MarketDataRequest":
        return MarketDataRequest._new_market_data_request(
            md_req_id,
            MarketDataSubscriptionRequestTypeEnum.SUBSCRIBE,
            0,
            symbols,
            md_entry_types,
        )

    @staticmethod
    def new_unsubscribe_request(
        old_market_data_request: MarketDataReq,
    ) -> "MarketDataRequest":
        return MarketDataRequest._new_market_data_request(
            old_market_data_request.md_req_id,
            MarketDataSubscriptionRequestTypeEnum.UNSUBSCRIBE,
            0,
            old_market_data_request.symbols,
            old_market_data_request.md_entry_types,
        )
