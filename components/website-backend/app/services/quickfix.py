import asyncio

from broker_quickfix_client import (
    ClientApplication,
    MarketDataEntryTypeEnum,
    MarketDataRequest,
    MarketDataResponse,
    MarketDataSnapshotFullRefreshHandler,
    SocketInitiator,
    setup,
    start_initiator,
)

from app.core.config import settings


class QuickfixEngineCLient:
    md_req_id: int
    application: ClientApplication
    initiator: SocketInitiator

    market_data_response: dict[int, MarketDataResponse]
    market_data_events: dict[int, asyncio.Event]

    def __init__(
        self,
        username: str = settings.SERVICE_ACCOUNT_USERNAME,
        password: str = settings.SERVICE_ACCOUNT_PASSWORD,
    ):
        self.md_req_id = 0
        self.market_data_response = {}
        self.market_data_events = {}

        self.application, self.initiator = setup(username, password)
        self.application.set_market_data_snapshot_full_refresh_handler(
            MarketDataSnapshotFullRefreshHandler(
                market_data_snapshot_full_refresh_callback=self.market_data_snapshot_full_refresh_callback
            )
        )
        start_initiator(self.initiator, self.application)

    def market_data_snapshot_full_refresh_callback(
        self,
        market_data_response: MarketDataResponse,
    ):

        self.market_data_response[market_data_response.md_req_id] = market_data_response
        self.market_data_events[market_data_response.md_req_id].set()

    async def request_symbols_price(self, symbols: list[str]):
        market_data_snapshot = MarketDataRequest.new_snapshot_request(
            self.md_req_id, 1, symbols, [MarketDataEntryTypeEnum.CLOSE]
        )
        self.md_req_id += 1

        event = self.market_data_events[self.md_req_id] = asyncio.Event()
        self.application.send(market_data_snapshot)

        await event.wait()

        response = self.market_data_response[self.md_req_id]

        self.market_data_response.pop(self.md_req_id)
        self.market_data_events.pop(self.md_req_id)

        return response
