import logging
import random
import string

from app.core.types import SecurityScopes
from app.crud.crud_account import account as accounts
from app.crud.redis.balance import crud_balance
from app.dependencies import get_db, get_redis
from app.schemas import account as account_schema

logger = logging.getLogger("app.command")


async def add_user(username: str, password: str, default_balance: int) -> None:
    logger.info(f"Creating new user with username {username} and password {password}")

    async with get_db.get_session() as session:
        # Create account
        account = await accounts.create(
            db=session,
            obj_in=account_schema.AccountCreate(
                username=username,
                password=password,
                first_name="User",
                last_name="User",
            ),
        )
        logger.info(f"User {username} created")

        updated_account = account_schema.AccountUpdate(
            **account_schema.Account.model_validate(account).model_dump()
        )
        updated_account.enabled = True
        updated_account.scope = SecurityScopes.USER
        await accounts.update(
            db=session,
            db_obj=account,
            obj_in=updated_account,
        )
        logger.info(f"User {username} activated")

    async with get_redis.get_client() as client:
        # Create balance
        await crud_balance.set(client, username, default_balance)
        logger.info(f"User {username} balance created with 10000")


async def add_n_users(n: int, file: str, default_balance: int) -> None:
    with open(file, "w", encoding="utf-8") as f:
        for i in range(n):
            username = f"user{i}"
            random_password = "".join(
                [random.choice(string.ascii_letters) for _ in range(16)]
            )
            await add_user(username, random_password, default_balance)
            f.write(f"{username} {random_password}\n")

            logger.info(f"User {username} created with password {random_password}")

    logger.info(f"{n} users created")
