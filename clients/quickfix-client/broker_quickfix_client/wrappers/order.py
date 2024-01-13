from dataclasses import dataclass

from broker_quickfix_client.wrappers.enums import OrderTypeEnum, SideEnum


@dataclass
class Order:
    order_id: int | None
    client_order_id: int
    symbol: str
    side: SideEnum
    type: OrderTypeEnum
    price: float | None
    quantity: int
