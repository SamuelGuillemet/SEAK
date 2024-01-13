import quickfix
import quickfix44

from broker_quickfix_client.utils.quickfix import get_message_field
from broker_quickfix_client.wrappers.enums import OrderTypeEnum, SideEnum
from broker_quickfix_client.wrappers.order import Order


class OrderCancelRequest(quickfix44.OrderCancelRequest):
    def __init__(
        self,
        orderID: quickfix.OrderID,
        clOrdID: quickfix.ClOrdID,
        origClOrdID: quickfix.OrigClOrdID,
        side: quickfix.Side,
        symbol: quickfix.Symbol,
    ):
        super().__init__()
        transact_time = quickfix.TransactTime()

        self.setField(orderID)
        self.setField(clOrdID)
        self.setField(origClOrdID)
        self.setField(side)
        self.setField(symbol)
        self.setField(transact_time)

    def get_order(self) -> Order:
        return Order(
            order_id=int(get_message_field(self, quickfix.OrderID)),
            client_order_id=int(get_message_field(self, quickfix.ClOrdID)),
            symbol=get_message_field(self, quickfix.Symbol),
            side=SideEnum(get_message_field(self, quickfix.Side)),
            type=OrderTypeEnum.LIMIT,
            price=0.0,
            quantity=0,
        )

    @staticmethod
    def new_cancel_order(
        cl_ord_id: int,
        original_order: Order,
    ) -> "OrderCancelRequest":
        if original_order.order_id is None:
            raise ValueError("Order id must be set")
        if original_order.type != OrderTypeEnum.LIMIT:
            raise ValueError("Only limit orders can be canceled")

        return OrderCancelRequest(
            quickfix.OrderID(str(original_order.order_id)),
            quickfix.ClOrdID(str(cl_ord_id)),
            quickfix.OrigClOrdID(str(original_order.client_order_id)),
            quickfix.Side(original_order.side.value),
            quickfix.Symbol(original_order.symbol),
        )
