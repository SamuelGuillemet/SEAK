import logging

from app.crud.crud_account import account as accounts
from app.crud.redis.balance import crud_balance
from app.crud.redis.stock import crud_stock
from app.dependencies import get_db, get_redis

logger = logging.getLogger("app.command")


async def delete_user(username: str) -> None:
    logger.info(f"Deleting user {username}")

    async with get_db.get_session() as session:
        account_query = await accounts.query(db=session, username=username)
        for account in account_query:
            await accounts.delete(db=session, id=account.id)
            logger.info(f"User {username} deleted")

    async with get_redis.get_client() as client:
        await crud_balance.delete(client, username)
        await crud_stock.delete_all(client, username)
        logger.info(f"User {username} balance deleted")


async def delete_users(file: str) -> None:
    with open(file, "r", encoding="utf-8") as f:
        for line in f:
            username, _ = line.split()

            try:
                await delete_user(username)
            except Exception as e:
                logger.error(f"Error deleting user {username}: {e}")

    logger.info("Users deleted")
