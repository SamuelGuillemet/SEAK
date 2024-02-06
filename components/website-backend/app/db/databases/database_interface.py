from abc import ABC, abstractmethod
from typing import Any, AsyncGenerator

from sqlalchemy.ext.asyncio import AsyncEngine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import Session

from app.db.base_class import Base


class DatabaseInterface(ABC):
    def __init__(self):
        self.async_sessionmaker: async_sessionmaker[AsyncSession] | None = None
        self.async_engine: AsyncEngine | None = None

    async def __call__(self) -> AsyncGenerator[AsyncSession, Any]:
        """
        Create a new session to interact with the database.
        To be used by FastAPI's dependency injection system.
        see https://fastapi.tiangolo.com/tutorial/dependencies/dependencies-with-yield/#a-database-dependency-with-yield
        ! This Must only be used by FastAPI's dependency injection system !
        """

        session = self.get_session()

        try:
            yield session
        finally:
            await session.close()

    @abstractmethod
    def setup(self) -> async_sessionmaker[AsyncSession]:  # pragma: no cover
        ...

    @abstractmethod
    async def drop(self) -> None:  # pragma: no cover
        ...

    async def shutdown(self) -> None:
        """
        Close the SQLAlchemy engine.
        """
        if self.async_engine:
            await self.async_engine.dispose()

    def get_session(self) -> AsyncSession:
        """
        Create a new session to interact with the database in a synchronous way.
        You need to close the session after using it.
        """
        if not self.async_sessionmaker:
            raise RuntimeError("Database not initialized")

        return self.async_sessionmaker()  # type: ignore

    def sync_wrapper(self, session: Session, method: Any) -> Any:
        """
        Wrapper to get a connection from the session and close it after the method is called.
        """
        connection = session.connection()
        return method(connection)

    async def create_all(self, no_drop: bool = False) -> None:
        """
        Create all tables, with a drop first if they already exist.
        """

        metadata = Base.metadata
        async with self.get_session() as session:
            async with session.begin():
                if not no_drop:
                    await session.run_sync(self.sync_wrapper, metadata.drop_all)
                await session.run_sync(self.sync_wrapper, metadata.create_all)

    async def is_created(self) -> bool:
        """
        Check if the database is already created.
        """
        async with self.get_session() as session:
            async with session.begin():
                return await session.run_sync(
                    self.sync_wrapper, Base.metadata.create_all
                )
