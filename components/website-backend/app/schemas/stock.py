from pydantic import Field

from app.schemas.base import DefaultModel


class Stock(DefaultModel):
    symbol: str
    quantity: int


class StockUpdate(DefaultModel):
    quantity: int = Field(..., ge=0)


class AccountStock(DefaultModel):
    stocks: list[Stock] = Field(default_factory=list)
