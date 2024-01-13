from dataclasses import dataclass

from broker_quickfix_client.wrappers.enums import (
    OrderRejectReasonEnum,
    OrderTypeEnum,
    SideEnum,
)


@dataclass
class BaseExecutionReport:
    order_id: int
    client_order_id: int
    symbol: str
    side: SideEnum
    leaves_quantity: int
    type: OrderTypeEnum


@dataclass
class FilledExecutionReport(BaseExecutionReport):
    cum_quantity: int
    price: float


@dataclass
class RejectedExecutionReport(BaseExecutionReport):
    reject_reason: OrderRejectReasonEnum


@dataclass
class AcceptedOrderExecutionReport(BaseExecutionReport):
    price: float


@dataclass
class ReplacedOrderExecutionReport(BaseExecutionReport):
    original_client_order_id: int
    price: float


@dataclass
class CanceledOrderExecutionReport(BaseExecutionReport):
    original_client_order_id: int
    price: float
