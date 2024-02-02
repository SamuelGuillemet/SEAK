from app.crud.base import CRUDBase
from app.models.users import Users
from app.schemas.account import AccountCreate, AccountUpdate


class CRUDAccount(CRUDBase[Users, AccountCreate, AccountUpdate]):
    ...


account = CRUDAccount(Users)
