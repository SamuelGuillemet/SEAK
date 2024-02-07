import asyncio
from typing import AsyncGenerator, Dict, List

from broker_quickfix_client import (
    MarketDataEntryTypeEnum,
    MarketDataRequest,
    MarketDataRequestReject,
    MarketDataRequestRejectHandler,
    MarketDataResponse,
    MarketDataSnapshotFullRefreshHandler,
    setup,
    start_initiator_async,
)
from fastapi import HTTPException, status

from app.core.config import settings
from app.schemas.market_data import MarketData


class QuickfixEngineService:
    def __init__(
        self,
        username: str = settings.SERVICE_ACCOUNT_USERNAME,
        password: str = settings.SERVICE_ACCOUNT_PASSWORD,
    ):
        self.md_req_id = 0
        self.market_data_response: Dict[str, MarketDataResponse] = {}
        self.market_data_events: Dict[str, asyncio.Event] = {}
        self.started = False

        self.application, self.initiator = setup(username, password)
        self._set_market_data_handlers()

    def _set_market_data_handlers(self):
        self.application.set_market_data_snapshot_full_refresh_handler(
            MarketDataSnapshotFullRefreshHandler(
                market_data_snapshot_full_refresh_callback=self.market_data_snapshot_full_refresh_callback
            )
        )
        self.application.set_market_data_request_reject_handler(
            MarketDataRequestRejectHandler(
                market_data_request_reject_callback=self.market_data_request_reject_callback
            )
        )

    def _build_key(self, md_req_id: int, symbol: str) -> str:
        return f"{md_req_id}_{symbol}"

    def _extract_close_value(self, req_id: int, symbol: str) -> float:
        try:
            key = self._build_key(req_id, symbol)
            market_data_response = self.market_data_response.pop(key)
            return market_data_response.market_data.get(1, [])[0].md_entry_px
        except (KeyError, IndexError):
            return 0.0

    def market_data_snapshot_full_refresh_callback(
        self, market_data_response: MarketDataResponse
    ):
        key = self._build_key(
            market_data_response.md_req_id, market_data_response.symbol
        )
        self.market_data_response[key] = market_data_response
        self.market_data_events[key].set()

    def market_data_request_reject_callback(
        self, market_data_request_reject: MarketDataRequestReject
    ):
        md_req_id = market_data_request_reject.md_req_id
        for key, value in self.market_data_events.items():
            if key.startswith(str(md_req_id)):
                value.set()

    async def request_symbols_price(self, symbols: List[str]):
        if not self.started:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Quickfix service is not available",
            )

        if not symbols or len(symbols) == 0:
            return MarketData(market_data={})

        req_id = self.md_req_id
        self.md_req_id += 1
        market_data_snapshot = MarketDataRequest.new_snapshot_request(
            req_id, 1, symbols, [MarketDataEntryTypeEnum.CLOSE]
        )

        for symbol in symbols:
            self.market_data_events[self._build_key(req_id, symbol)] = asyncio.Event()

        self.application.send(market_data_snapshot)

        data: Dict[str, float] = {}
        try:
            for symbol in symbols:
                key = self._build_key(req_id, symbol)
                await asyncio.wait_for(self.market_data_events[key].wait(), timeout=1.0)
                data[symbol] = self._extract_close_value(req_id, symbol)
                self.market_data_events.pop(key)
        except asyncio.TimeoutError as e:
            raise HTTPException(
                status_code=status.HTTP_504_GATEWAY_TIMEOUT,
                detail="Request timeout",
            ) from e

        return MarketData(market_data=data)


class QuickfixServiceDependency:
    service: QuickfixEngineService

    async def setup(self, timeout: float = 1) -> QuickfixEngineService:
        self.service = QuickfixEngineService()
        started = await start_initiator_async(
            self.service.initiator, self.service.application, timeout
        )
        self.service.started = started
        return self.service

    def shutdown(self):
        self.service.initiator.stop()

    async def __call__(self) -> AsyncGenerator[QuickfixEngineService, None]:
        """
        Return a the QuickfixEngineService instance.
        To be used by FastAPI's dependency injection system.
        see https://fastapi.tiangolo.com/tutorial/dependencies/dependencies-with-yield/#a-database-dependency-with-yield
        ! This Must only be used by FastAPI's dependency injection system !
        """
        yield self.service
