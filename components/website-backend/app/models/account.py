from sqlalchemy import UniqueConstraint
from sqlalchemy.orm import Mapped

from app.core.types import SecurityScopes
from app.db.base_class import Base, Str256, Str512


class Account(Base):
    username: Mapped[Str256]
    password: Mapped[Str512]
    first_name: Mapped[Str256]
    last_name: Mapped[Str256]
    scope: Mapped[SecurityScopes]
    enabled: Mapped[bool]

    __table_args__ = (UniqueConstraint("username"),)
