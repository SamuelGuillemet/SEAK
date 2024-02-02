from typing import TYPE_CHECKING, List

from sqlalchemy.orm import Mapped, relationship

from app.core.types import SecurityScopes
from app.db.base_class import Base, Str256, Str512

if TYPE_CHECKING:  # pragma: no cover
    from .stock import Stock


class Account(Base):
    username: Mapped[Str256]
    password: Mapped[Str512]
    first_name: Mapped[Str256]
    last_name: Mapped[Str256]
    scope: Mapped[SecurityScopes]
    enabled: Mapped[bool]
    balance: Mapped[float]

    stocks: Mapped[List["Stock"]] = relationship(
        back_populates="account", lazy="selectin"
    )
