import logging

from fastapi import APIRouter

from app.dependencies import QuickfixEngineDependency
from app.schemas import market_data as market_data_schema

router = APIRouter(tags=["market_data"], prefix="/market_data")

logger = logging.getLogger("app.api.v1.market_data")


@router.post(
    "/",
    response_model=market_data_schema.MarketData,
)
async def read_market_data(
    market_data_request: market_data_schema.MarketDataRequest,
    quickfix_engine: QuickfixEngineDependency,
):
    """
    List the price of the given symbols.
    """
    return await quickfix_engine.request_symbols_price(market_data_request.symbols)
