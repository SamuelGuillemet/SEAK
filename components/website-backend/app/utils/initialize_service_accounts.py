import logging

from app.core.config import settings
from app.core.types import SecurityScopes
from app.crud.crud_account import account as accounts
from app.dependencies import get_db
from app.schemas import account as account_schema

logger = logging.getLogger("app.utils.initialize_service_accounts")


async def initialize_services_account():
    """
    Initialize the services account.
    """
    logger.info("Initializing services account...")
    await get_db.is_created()
    async with get_db.get_session() as session:
        # Create account
        account = await accounts.create(
            db=session,
            obj_in=account_schema.AccountCreate(
                username=settings.SERVICE_ACCOUNT_USERNAME,
                password=settings.SERVICE_ACCOUNT_PASSWORD,
                first_name="QuickFix",
                last_name="Service Account",
            ),
        )
        logger.info("Service account created")

        updated_account = account_schema.AccountUpdate(
            **account_schema.Account.model_validate(account).model_dump()
        )
        updated_account.enabled = True
        updated_account.scope = SecurityScopes.SERVICE
        await accounts.update(
            db=session,
            db_obj=account,
            obj_in=updated_account,
        )
        logger.info("Service account activated")
