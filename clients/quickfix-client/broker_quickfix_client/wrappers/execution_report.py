from dataclasses import dataclass

from broker_quickfix_client.wrappers.enums import (
    OrderRejectReasonEnum,
    OrderTypeEnum,
    SideEnum,
)


@dataclass
class FilledExecutionReport:
    order_id: int
    client_order_id: int
    symbol: str
    side: SideEnum
    type: OrderTypeEnum
    leaves_quantity: int
    price: float
    cum_quantity: int


@dataclass
class RejectedExecutionReport:
    order_id: int
    client_order_id: int
    symbol: str
    side: SideEnum
    type: OrderTypeEnum
    leaves_quantity: int
    reject_reason: OrderRejectReasonEnum
