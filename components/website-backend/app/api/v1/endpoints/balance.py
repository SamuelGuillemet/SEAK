import logging

from fastapi import APIRouter, HTTPException, Security, status

from app.core.translation import Translator
from app.core.types import SecurityScopes
from app.crud.crud_account import account as accounts
from app.crud.redis.balance import crud_balance
from app.dependencies import DBDependency, RedisDependency, get_current_active_account
from app.schemas import balance as balance_schema

router = APIRouter(tags=["balance"], prefix="/balance")
translator = Translator(element="account")

logger = logging.getLogger("app.api.v1.balance")


@router.put(
    "/{account_id}",
    response_model=balance_schema.Balance,
    dependencies=[
        Security(get_current_active_account, scopes=[SecurityScopes.ADMIN.value])
    ],
)
async def update_balance(
    account_id: int,
    balance: balance_schema.BalanceUpdate,
    db: DBDependency,
    redis: RedisDependency,
):
    """
    Modify the balance of an account by ID.

    This endpoint requires authentication with the "admin" scope.
    """
    account = await accounts.read(db, account_id)
    if account is None:
        logger.debug(f"Account {account_id} not found")
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=translator.ELEMENT_NOT_FOUND
        )
    balance_value = await crud_balance.get(redis, account.username)
    if balance_value.balance is None:
        logger.debug(f"Balance for account {account_id} not found")
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=translator.ELEMENT_NOT_FOUND
        )

    return await crud_balance.set(redis, account.username, balance.balance)
