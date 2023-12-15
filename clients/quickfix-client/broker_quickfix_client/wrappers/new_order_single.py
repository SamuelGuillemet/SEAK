from dataclasses import dataclass

import quickfix44
from quickfix import ClOrdID, OrderQty, OrdType, Price, Side, Symbol, TransactTime

from broker_quickfix_client.utils.quickfix import get_message_field
from broker_quickfix_client.wrappers.enums import OrderTypeEnum, SideEnum


@dataclass
class Order:
    order_id: int | None
    client_order_id: int
    symbol: str
    side: SideEnum
    price: float | None
    quantity: int


class NewOrderSingle(quickfix44.NewOrderSingle):
    def __init__(
        self,
        clOrdID: ClOrdID,
        side: Side,
        orderQty: OrderQty,
        ordType: OrdType,
        symbol: Symbol,
        price: Price | None = None,
    ):
        super().__init__()
        transact_time = TransactTime()

        self.setField(clOrdID)
        self.setField(side)
        self.setField(orderQty)
        self.setField(ordType)
        self.setField(symbol)
        self.setField(transact_time)

        if price:
            self.setField(price)

    def get_order(self) -> Order:
        price = get_message_field(self, Price)
        return Order(
            order_id=None,
            client_order_id=int(get_message_field(self, ClOrdID)),
            symbol=get_message_field(self, Symbol),
            side=SideEnum(get_message_field(self, Side)),
            price=float(price) if price else None,
            quantity=int(get_message_field(self, OrderQty)),
        )

    @staticmethod
    def new_market_order(
        cl_ord_id: int,
        side: SideEnum,
        order_qty: int,
        symbol: str,
    ) -> "NewOrderSingle":
        return NewOrderSingle(
            ClOrdID(str(cl_ord_id)),
            Side(side.value),
            OrderQty(order_qty),
            OrdType(OrderTypeEnum.MARKET.value),
            Symbol(symbol),
        )

    @staticmethod
    def new_limit_order(
        cl_ord_id: int,
        side: SideEnum,
        order_qty: int,
        symbol: str,
        price: float,
    ) -> "NewOrderSingle":
        return NewOrderSingle(
            ClOrdID(str(cl_ord_id)),
            Side(side.value),
            OrderQty(order_qty),
            OrdType(OrderTypeEnum.LIMIT.value),
            Symbol(symbol),
            Price(price),
        )

    @staticmethod
    def new_stop_order(
        cl_ord_id: int,
        side: SideEnum,
        order_qty: int,
        symbol: str,
        price: str,
    ) -> "NewOrderSingle":
        return NewOrderSingle(
            ClOrdID(str(cl_ord_id)),
            Side(side.value),
            OrderQty(order_qty),
            OrdType(OrderTypeEnum.STOP.value),
            Symbol(symbol),
            Price(price),
        )

    @staticmethod
    def new_stop_limit_order(
        cl_ord_id: int,
        side: SideEnum,
        order_qty: int,
        symbol: str,
        price: float,
    ) -> "NewOrderSingle":
        return NewOrderSingle(
            ClOrdID(str(cl_ord_id)),
            Side(side.value),
            OrderQty(order_qty),
            OrdType(OrderTypeEnum.STOP_LIMIT.value),
            Symbol(symbol),
            Price(price),
        )
