import unittest
from typing import cast

import pytest
from fastapi.testclient import TestClient
from testcontainers.redis import RedisContainer

from app.db.databases.sqlite import SqliteDatabase
from app.dependencies import get_current_active_account, get_db, get_redis


async def override_get_current_active_account():
    pass


class BaseTest(unittest.IsolatedAsyncioTestCase):
    redis_db: RedisContainer | None = None

    @pytest.fixture(autouse=True)
    def inject_fixtures(
        self, client: TestClient, caplog: pytest.LogCaptureFixture, tmp_path
    ):
        self._caplog = caplog
        self._client = client
        self._tmp_path = tmp_path

    def wipe_dependencies_overrides(self):
        self._client.app.dependency_overrides.clear()  # type: ignore

    async def asyncSetUp(self) -> None:
        sqlite_path = "sqlite+aiosqlite:///" + str(self._tmp_path / "test.db")
        self._client.app.dependency_overrides[  # type: ignore
            get_current_active_account
        ] = override_get_current_active_account

        cast(SqliteDatabase, get_db).setup(sqlite_path)
        await cast(SqliteDatabase, get_db).create_all(no_drop=True)

        self.redis_db = RedisContainer()
        self.redis_db.start()
        redis_uri = f"redis://{self.redis_db.get_container_host_ip()}:{self.redis_db.get_exposed_port(6379)}"
        get_redis.setup(redis_uri)

    async def asyncTearDown(self) -> None:
        if self.redis_db:
            self.redis_db.stop()
