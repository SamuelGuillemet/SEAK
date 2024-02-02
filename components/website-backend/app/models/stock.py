from typing import TYPE_CHECKING

from sqlalchemy.orm import Mapped, relationship

from app.db.base_class import Base, Str256, build_fk_annotation

if TYPE_CHECKING:  # pragma: no cover
    from .account import Account


account_fk = build_fk_annotation("account")


class Stock(Base):
    symbol: Mapped[Str256]
    quantity: Mapped[int]

    account_id: Mapped[account_fk]
    account: Mapped["Account"] = relationship(back_populates="stocks", lazy="selectin")
