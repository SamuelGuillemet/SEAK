from typing import Any, AsyncGenerator

from redis.asyncio import ConnectionPool, Redis

from app.core.config import settings


class RedisDB:
    def __init__(self) -> None:
        self.connection_pool: ConnectionPool | None = None

    def setup(self) -> ConnectionPool:
        """
        Create a new Redis connection pool.
        """
        self.connection_pool = ConnectionPool.from_url(
            settings.REDIS_URI, decode_responses=True
        )
        return self.connection_pool

    def get_client(self) -> Redis:
        """
        Get the connection pool.
        """
        if not self.connection_pool:
            raise RuntimeError("Redis not initialized")

        return Redis.from_pool(self.connection_pool)

    async def __call__(self) -> AsyncGenerator[Redis, Any]:
        """
        Create a new Redis connection.
        To be used by FastAPI's dependency injection system.
        see https://fastapi.tiangolo.com/tutorial/dependencies/dependencies-with-yield/#a-database-dependency-with-yield
        ! This Must only be used by FastAPI's dependency injection system !
        """
        client = self.get_client()

        try:
            yield client
        finally:
            await client.aclose(close_connection_pool=False)
