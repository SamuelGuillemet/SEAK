import os

from sqlalchemy.ext.asyncio import (
    AsyncEngine,
    AsyncSession,
    async_sessionmaker,
    create_async_engine,
)

from app.core.config import settings
from app.db.databases.database_interface import DatabaseInterface


class SqliteDatabase(DatabaseInterface):
    __name__ = "SQLite"

    ERROR_MESSAGE = "Don't use SQLite in production"

    def setup(
        self, path: str = settings.DATABASE_URI
    ) -> async_sessionmaker[AsyncSession]:
        """
        Create a new SQLAlchemy engine and sessionmaker.
        """
        if settings.ENVIRONMENT == "production":
            raise ValueError(self.ERROR_MESSAGE)

        self.async_engine: AsyncEngine = create_async_engine(path)
        self.async_sessionmaker = async_sessionmaker(
            self.async_engine, class_=AsyncSession, expire_on_commit=False
        )
        return self.async_sessionmaker

    async def drop(self, path: str = settings.DATABASE_URI) -> None:
        """
        Drop the database, by deleting the db file.
        """
        if settings.ENVIRONMENT == "production":
            raise ValueError(self.ERROR_MESSAGE)

        file_name = path.split("sqlite:///")[-1]
        if os.path.exists(file_name):  # pragma: no cover
            os.remove(file_name)
