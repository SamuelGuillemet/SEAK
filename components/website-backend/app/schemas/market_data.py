from app.schemas.base import DefaultModel


class MarketData(DefaultModel):
    market_data: dict[str, float]


class MarketDataRequest(DefaultModel):
    symbols: list[str]
