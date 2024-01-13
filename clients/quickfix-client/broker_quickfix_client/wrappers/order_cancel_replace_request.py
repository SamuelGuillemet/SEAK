import quickfix
import quickfix44

from broker_quickfix_client.utils.quickfix import get_message_field
from broker_quickfix_client.wrappers.enums import OrderTypeEnum, SideEnum
from broker_quickfix_client.wrappers.order import Order


class OrderCancelReplaceRequest(quickfix44.OrderCancelReplaceRequest):
    def __init__(
        self,
        orderID: quickfix.OrderID,
        clOrdID: quickfix.ClOrdID,
        origClOrdID: quickfix.OrigClOrdID,
        side: quickfix.Side,
        orderQty: quickfix.OrderQty,
        ordType: quickfix.OrdType,
        symbol: quickfix.Symbol,
        price: quickfix.Price,
    ):
        super().__init__()
        transact_time = quickfix.TransactTime()

        self.setField(orderID)
        self.setField(clOrdID)
        self.setField(origClOrdID)
        self.setField(side)
        self.setField(orderQty)
        self.setField(ordType)
        self.setField(symbol)
        self.setField(price)
        self.setField(transact_time)

    def get_order(self) -> Order:
        return Order(
            order_id=int(get_message_field(self, quickfix.OrderID)),
            client_order_id=int(get_message_field(self, quickfix.ClOrdID)),
            symbol=get_message_field(self, quickfix.Symbol),
            side=SideEnum(get_message_field(self, quickfix.Side)),
            type=OrderTypeEnum(get_message_field(self, quickfix.OrdType)),
            price=float(get_message_field(self, quickfix.Price)),
            quantity=int(get_message_field(self, quickfix.OrderQty)),
        )

    @staticmethod
    def new_replace_order(
        cl_ord_id: int,
        original_order: Order,
        modified_price: float | None = None,
        modified_quantity: int | None = None,
    ) -> "OrderCancelReplaceRequest":
        if modified_price is None and modified_quantity is None:
            raise ValueError("Either price and/or quantity must be modified")
        if original_order.order_id is None:
            raise ValueError("Order id must be set")
        if original_order.type != OrderTypeEnum.LIMIT:
            raise ValueError("Only limit orders can be replaced")

        return OrderCancelReplaceRequest(
            quickfix.OrderID(str(original_order.order_id)),
            quickfix.ClOrdID(str(cl_ord_id)),
            quickfix.OrigClOrdID(str(original_order.client_order_id)),
            quickfix.Side(original_order.side.value),
            quickfix.OrderQty(
                original_order.quantity
                if modified_quantity is None
                else modified_quantity
            ),
            quickfix.OrdType(OrderTypeEnum.LIMIT.value),
            quickfix.Symbol(original_order.symbol),
            quickfix.Price(
                original_order.price if modified_price is None else modified_price
            ),
        )
