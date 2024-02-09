import logging

from fastapi import APIRouter, HTTPException, Security, status

from app.core.translation import Translator
from app.core.types import SecurityScopes
from app.crud.crud_account import account as accounts
from app.crud.redis.stock import crud_stock
from app.dependencies import DBDependency, RedisDependency, get_current_active_account
from app.schemas import stock as stock_schema

router = APIRouter(tags=["stock"], prefix="/stock")

translator = Translator(element="account")

logger = logging.getLogger("app.api.v1.stock")


@router.put(
    "/{account_id}/{symbol}",
    response_model=stock_schema.Stock,
    dependencies=[
        Security(get_current_active_account, scopes=[SecurityScopes.ADMIN.value])
    ],
)
async def update_stock(
    account_id: int,
    symbol: str,
    stock: stock_schema.StockUpdate,
    db: DBDependency,
    redis: RedisDependency,
):
    """
    Modify the stock of an account by ID and symbol.

    This endpoint requires authentication with the "admin" scope.
    """
    account = await accounts.read(db, account_id)
    if account is None:
        logger.debug(f"Account {account_id} not found")
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=translator.ELEMENT_NOT_FOUND,
        )
    return await crud_stock.set(redis, account.username, symbol, stock.quantity)
