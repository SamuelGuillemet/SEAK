import logging

from app.core.config import settings
from app.core.types import SecurityScopes
from app.crud.crud_account import account as accounts
from app.crud.redis.balance import crud_balance
from app.dependencies import get_db, get_redis
from app.schemas import account as account_schema
from app.utils.initialize_service_accounts import initialize_services_account

logger = logging.getLogger("app.command")


async def init_db() -> None:
    logger.info("Creating initial data")

    async with get_db.get_session() as session:
        # Create account
        account = await accounts.create(
            db=session,
            obj_in=account_schema.AccountCreate(
                username=settings.BASE_ACCOUNT_USERNAME,
                password=settings.BASE_ACCOUNT_PASSWORD,
                first_name="Admin",
                last_name="Admin",
            ),
        )
        logger.info("Base account created")

        updated_account = account_schema.AccountUpdate(
            **account_schema.Account.model_validate(account).model_dump()
        )
        updated_account.enabled = True
        updated_account.scope = SecurityScopes.ADMIN
        await accounts.update(
            db=session,
            db_obj=account,
            obj_in=updated_account,
        )
        logger.info("Base account activated")

    async with get_redis.get_client() as client:
        # Create balance
        await crud_balance.set(client, settings.BASE_ACCOUNT_USERNAME, 0)
        logger.info("Base balance created")

    await initialize_services_account(setup_client=False)

    logger.info("Initial data created")
