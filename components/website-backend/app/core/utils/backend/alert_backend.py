import logging
from traceback import print_exception
from typing import Protocol

from fastapi.datastructures import URL, Headers

from app.core.config import settings

logger = logging.getLogger("app.core.utils.backend.alert_backend")


class TestException(Exception):
    pass


class Alert(Protocol):  # pragma: no cover
    def __call__(
        self, exception: Exception, method: str, url: URL, headers: Headers, body: bytes
    ) -> None:
        ...


def alert_backend() -> Alert:
    return alert_to_terminal


def alert_to_terminal(
    exception: Exception, method: str, url: URL, headers: Headers, body: bytes
) -> None:
    if (
        isinstance(exception, TestException) and settings.ENVIRONMENT == "production"
    ):  # pragma: no cover
        return None

    logger.error("### An exception has been raised! ###")
    logger.error("############## Request ##############")
    logger.error(f"{method} {url}")
    logger.error("########## Request headers ##########")
    for key, value in headers.items():
        logger.error(f"- **{key}**: {value}")
    logger.error("########### Request body ############")
    logger.error(body.decode())
    logger.error("############# Exception #############")
    logger.exception(exception, exc_info=False)
    print_exception(type(exception), exception, exception.__traceback__, chain=False)
