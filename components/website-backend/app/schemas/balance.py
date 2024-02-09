from pydantic import Field

from app.schemas.base import DefaultModel


class Balance(DefaultModel):
    balance: float | None = None


class BalanceUpdate(DefaultModel):
    balance: float = Field(..., ge=0)
