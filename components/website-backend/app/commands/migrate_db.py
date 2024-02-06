import logging

from app.dependencies import get_db

logger = logging.getLogger("app.command")


async def migrate_db() -> None:
    """
    Migrates the database depending on the current engine;
    If the engine is SQLite, it will use the built-in create_all() method.
    """
    logger.info("Migrating database")

    await get_db.create_all()  # type: ignore
    logger.info("Migrations completed.")
