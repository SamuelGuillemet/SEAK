import logging
from typing import Any, Sequence

from redis.asyncio import Redis
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import InstrumentedAttribute

import app.schemas.account as account_schema
from app.crud.base import CRUDBase
from app.crud.redis.balance import crud_balance
from app.crud.redis.stock import crud_stock
from app.models.account import Account

logger = logging.getLogger("app.crud.account")


class CRUDAccount(
    CRUDBase[Account, account_schema.AccountCreate, account_schema.AccountUpdate]
):
    async def complete_account(
        self, client: Redis, db_obj: Account
    ) -> account_schema.Account:
        validate_account = account_schema.Account.model_validate(db_obj)
        balance = await crud_balance.get(client, db_obj.username)
        stocks = await crud_stock.get_all(client, db_obj.username)

        validate_account.balance = balance.balance
        validate_account.stocks = stocks.stocks
        return validate_account

    async def update_redis(
        self,
        db: AsyncSession,
        client: Redis,
        *,
        db_obj: Account,
        obj_in: account_schema.AccountUpdate,
    ) -> account_schema.Account:
        # Update the account in the database
        old_enabled = db_obj.enabled
        updated_account = await super().update(db, db_obj=db_obj, obj_in=obj_in)
        # Verify if the enabled field was updated
        if updated_account.enabled != old_enabled:
            # If enabled is now True, add the account to redis
            if updated_account.enabled:
                await crud_balance.set(client, updated_account.username, 0)
            # If enabled is now False, remove the account from redis
            else:
                await crud_balance.delete(client, updated_account.username)
                await crud_stock.delete_all(client, updated_account.username)

        return await self.complete_account(client, updated_account)

    async def read_redis(
        self, db: AsyncSession, client: Redis, id: Any, for_update: bool = False
    ) -> account_schema.Account | None:
        account_model = await super().read(db, id, for_update)

        if not account_model:
            return None

        return await self.complete_account(client, account_model)

    async def query_redis(
        self,
        db: AsyncSession,
        client: Redis,
        distinct: InstrumentedAttribute[Any] | None = None,
        skip: int = 0,
        limit: int | None = 100,
        **filters,
    ) -> Sequence[account_schema.Account]:
        acount_models = await super().query(db, distinct, skip, limit, **filters)
        completed_accounts = []

        for account_model in acount_models:
            completed_accounts.append(
                await self.complete_account(client, account_model)
            )
        return completed_accounts


account = CRUDAccount(Account)
