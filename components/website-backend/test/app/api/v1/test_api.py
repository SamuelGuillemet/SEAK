from fastapi import APIRouter

from app.api.v1.api import api_v1_router
from app.api.v1.endpoints.account import router as account_router
from app.api.v1.endpoints.auth import router as auth_router


def test_api_v1_router():
    assert isinstance(api_v1_router, APIRouter)


def test_api_v1_router_include_all_router():
    all_routers = [
        account_router,
        auth_router,
    ]
    for router in all_routers:
        for route in router.routes:
            assert route in api_v1_router.routes
