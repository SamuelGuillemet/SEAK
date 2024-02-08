import logging
import os
from abc import abstractmethod
from functools import lru_cache
from typing import ClassVar, Literal, Optional

from pydantic import Field
from pydantic_settings import BaseSettings

logger = logging.getLogger("app.core.config")

SupportedLocales = Literal["en", "fr"]
SupportedEnvironments = Literal["development", "production", "test"]


class Settings(BaseSettings):
    API_V1_PREFIX: str = "/api/v1"
    LOCALE: SupportedLocales

    ALLOWED_HOSTS: list[str]

    LOG_LEVEL: int
    ENVIRONMENT: SupportedEnvironments

    # Authentication config

    ACCESS_TOKEN_EXPIRE_MINUTES: int = 60 * 24 * 1  # 1 day
    SECRET_KEY: str
    ALGORITHM: str = "HS256"

    # Base account config

    BASE_ACCOUNT_USERNAME: str
    BASE_ACCOUNT_PASSWORD: str

    # Database config

    POSTGRES_HOST: str
    POSTGRES_PORT: int
    POSTGRES_DB: str
    POSTGRES_USER: str
    POSTGRES_PASSWORD: str

    @property
    @abstractmethod
    def DATABASE_URI(self) -> str:
        """The URI for the database."""

    REDIS_HOST: str
    REDIS_PORT: int

    @property
    def REDIS_URI(self) -> str:
        return f"redis://{self.REDIS_HOST}:{self.REDIS_PORT}"

    SERVICE_ACCOUNT_USERNAME: str
    SERVICE_ACCOUNT_PASSWORD: str


class ConfigDevelopment(Settings):
    LOCALE: SupportedLocales = "fr"

    ALLOWED_HOSTS: list[str] = ["*"]

    LOG_LEVEL: int = logging.DEBUG
    ENVIRONMENT: SupportedEnvironments = "development"

    """ Authentication config"""
    # openssl rand -hex 32
    SECRET_KEY: str = Field(
        default="6a50e3ddeef70fd46da504d8d0a226db7f0b44dcdeb65b97751cf2393b33693e",
    )

    """Base account config """

    BASE_ACCOUNT_USERNAME: str = "admin"
    BASE_ACCOUNT_PASSWORD: str = "admin-password*45"

    """Database config"""

    POSTGRES_HOST: str = "localhost"
    POSTGRES_PORT: int = 5432
    POSTGRES_DB: str = "seak"
    POSTGRES_USER: str = "postgres"
    POSTGRES_PASSWORD: str = "postgres"

    POSTGRES_DATABASE_URI: ClassVar[
        str
    ] = "postgresql+asyncpg://{user}:{password}@{host}:{port}/{db}".format(
        user=POSTGRES_USER,
        password=POSTGRES_PASSWORD,
        host=POSTGRES_HOST,
        port=POSTGRES_PORT,
        db=POSTGRES_DB,
    )
    SQLITE_DATABASE_URI: ClassVar[str] = "sqlite+aiosqlite:///./seak.db"

    # you can change it to either SQLITE_DATABASE_URI or POSTGRES_DATABASE_URI
    @property
    def DATABASE_URI(self) -> str:
        value = os.environ.get("DB_TYPE")
        if value == "POSTGRES":
            return self.POSTGRES_DATABASE_URI

        return self.SQLITE_DATABASE_URI

    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379

    SERVICE_ACCOUNT_USERNAME: str = "service_account"
    SERVICE_ACCOUNT_PASSWORD: str = "91467d88cdf9aafc43d697be2d759b30"


class ConfigProduction(Settings):
    LOCALE: SupportedLocales = "fr"

    ALLOWED_HOSTS: list[str] = ["*"]  # ! Shouldn't be set to ["*"] in production!

    LOG_LEVEL: int = logging.INFO
    ENVIRONMENT: SupportedEnvironments = "production"

    """ Authentication config"""
    # openssl rand -hex 32
    SECRET_KEY: str = Field(...)

    """Base account config """

    BASE_ACCOUNT_USERNAME: str = Field(...)
    BASE_ACCOUNT_PASSWORD: str = Field(...)

    """Database config"""

    POSTGRES_HOST: str = Field(...)
    POSTGRES_PORT: int = 5432
    POSTGRES_DB: str = Field(...)
    POSTGRES_USER: str = Field(...)
    POSTGRES_PASSWORD: str = Field(...)

    @property
    def DATABASE_URI(self) -> str:
        return "postgresql+asyncpg://{user}:{password}@{host}:{port}/{db}".format(
            user=self.POSTGRES_USER,
            password=self.POSTGRES_PASSWORD,
            host=self.POSTGRES_HOST,
            port=self.POSTGRES_PORT,
            db=self.POSTGRES_DB,
        )

    REDIS_HOST: str = Field(...)
    REDIS_PORT: int = Field(...)

    SERVICE_ACCOUNT_USERNAME: str = Field(...)
    SERVICE_ACCOUNT_PASSWORD: str = Field(...)


class ConfigTest(Settings):
    LOCALE: SupportedLocales = "en"
    ALLOWED_HOSTS: list[str] = ["*"]

    LOG_LEVEL: int = logging.DEBUG
    ENVIRONMENT: SupportedEnvironments = "test"

    """ Authentication config"""
    SECRET_KEY: str = "6a50e3ddeef70fd46da504d8d0a226db7f0b44dcdeb65b97751cf2393b33693e"

    """Base account config """
    BASE_ACCOUNT_USERNAME: str = "test"
    BASE_ACCOUNT_PASSWORD: str = "test_password*45"  # to match the password policy

    ###

    """Database config"""
    POSTGRES_HOST: str = "localhost"
    POSTGRES_PORT: int = 5432
    POSTGRES_DB: str | None = "test_db"
    POSTGRES_USER: str | None = "test_user"
    POSTGRES_PASSWORD: str | None = "test_password"

    @property
    def DATABASE_URI(self) -> str:
        return "sqlite+aiosqlite:///./test_seak.db"

    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379

    SERVICE_ACCOUNT_USERNAME: str = "test_service_username"
    SERVICE_ACCOUNT_PASSWORD: str = "test_service_password"


env = os.getenv("ENVIRONMENT", "development")


@lru_cache()
def select_settings(_env: Optional[str] = env):
    """
    Returns the application settings based on the environment specified.

    Args:
        _env (Optional[str], optional): Environment to get the settings for. Defaults to env.

    Raises:
        ValueError: If an invalid environment is specified.

    Returns:
        Settings: The application settings.
    """
    logger.info(f"Loading settings for environment {_env}")
    if _env == "development":
        return ConfigDevelopment()

    if _env == "production":
        return ConfigProduction()  # type: ignore

    if _env == "test":
        return ConfigTest()

    raise ValueError(f"Invalid environment {_env}")


settings = select_settings()
