import logging
from operator import eq, ne

from fastapi import APIRouter, HTTPException, Security, status

from app.core.translation import Translator
from app.core.types import SecurityScopes
from app.crud.crud_account import account as accounts
from app.dependencies import DBDependency, RedisDependency, get_current_active_account
from app.schemas import account as account_schema

router = APIRouter(tags=["account"], prefix="/account")
translator = Translator(element="account")

logger = logging.getLogger("app.api.v1.account")


@router.get(
    "/",
    response_model=list[account_schema.Account],
    dependencies=[
        Security(get_current_active_account, scopes=[SecurityScopes.ADMIN.value])
    ],
)
async def read_accounts(
    db: DBDependency,
    redis: RedisDependency,
):
    """
    Retrieve a list of accounts.

    This endpoint requires authentication with the "admin" scope.
    """
    return await accounts.query_redis(
        db, redis, limit=None, scope={ne: SecurityScopes.SERVICE.value}
    )


@router.get(
    "/ranking",
    response_model=list[account_schema.RankedAccount],
)
async def read_ranked_accounts(
    db: DBDependency,
    redis: RedisDependency,
):
    """
    Retrieve a list of accounts for ranking.
    """
    raw_accounts = await accounts.query_redis(
        db,
        redis,
        limit=None,
        scope={eq: SecurityScopes.USER.value},
        enabled=True,
    )

    return [
        account_schema.RankedAccount.model_validate(account) for account in raw_accounts
    ]


@router.post("/", response_model=account_schema.Account)
async def create_account(account: account_schema.AccountCreate, db: DBDependency):
    """
    Create a new account.
    """
    if await accounts.query(db, username=account.username, limit=1):
        logger.debug(f"Username {account.username} already exists")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=translator.USERNAME_UNAVAILABLE,
        )
    return await accounts.create(db, obj_in=account)


@router.get(
    "/{account_id}",
    response_model=account_schema.Account,
    dependencies=[
        Security(get_current_active_account, scopes=[SecurityScopes.ADMIN.value])
    ],
)
async def read_account(account_id: int, db: DBDependency, redis: RedisDependency):
    """
    Retrieve an account by ID.

    This endpoint requires authentication with the "admin" scope.
    """
    account = await accounts.read_redis(db, redis, account_id)
    if account is None:
        logger.debug(f"Account {account_id} not found")
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=translator.ELEMENT_NOT_FOUND
        )
    return account


@router.patch(
    "/{account_id}",
    response_model=account_schema.Account,
    dependencies=[
        Security(get_current_active_account, scopes=[SecurityScopes.ADMIN.value])
    ],
)
async def update_account(
    account_id: int,
    account: account_schema.AccountUpdate,
    db: DBDependency,
    redis: RedisDependency,
):
    """
    Update an account by ID.

    This endpoint requires authentication with the "admin" scope.
    """
    old_account = await accounts.read(db, account_id)
    if old_account is None:
        logger.debug(f"Account {account_id} not found")
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=translator.ELEMENT_NOT_FOUND
        )
    results = await accounts.query(db, username=account.username)
    if results and results[0].id != old_account.id:
        logger.debug(f"Username {account.username} already exists")
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=translator.USERNAME_UNAVAILABLE,
        )
    return await accounts.update_redis(db, redis, db_obj=old_account, obj_in=account)


@router.delete(
    "/{account_id}",
    response_model=account_schema.Account,
    dependencies=[
        Security(get_current_active_account, scopes=[SecurityScopes.ADMIN.value])
    ],
)
async def delete_account(account_id: int, db: DBDependency):
    """
    Delete an account by ID.

    This endpoint requires authentication with the "admin" scope.
    """
    account = await accounts.read(db, account_id)
    if account is None:
        logger.debug(f"Account {account_id} not found")
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND, detail=translator.ELEMENT_NOT_FOUND
        )
    return await accounts.delete(db, id=account_id)
