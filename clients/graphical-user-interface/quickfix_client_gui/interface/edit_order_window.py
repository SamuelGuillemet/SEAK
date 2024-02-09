from tkinter import Button, Entry, Label, Toplevel, W

from broker_quickfix_client.wrappers.enums import OrderTypeEnum, SideEnum
from broker_quickfix_client.wrappers.order import Order as QuickfixOrder
from broker_quickfix_client.wrappers.order_cancel_replace_request import (
    OrderCancelReplaceRequest,
)


class EditOrderWindow(Toplevel):
    def __init__(self, master, item_id):
        super().__init__(master=master)
        #   WINDOW SETUP    #####
        self.cl_ord_id = item_id
        window_width = 500
        window_height = 300

        screen_width = self.winfo_screenwidth()
        screen_height = self.winfo_screenheight()

        center_x = int(screen_width / 2 - window_width / 2)
        center_y = int(screen_height / 2 - window_height / 2)

        self.geometry(f"{window_width}x{window_height}+{center_x}+{center_y}")

        self.wm_title("Order")

        shares_label = Label(self, text="Number of Shares:")
        shares_entry = Entry(self)

        price_label = Label(self, text="Price:")
        price_entry = Entry(self)

        submit_button = Button(self, text="Submit Order", command=self.edit_order)

        self.shares_entry = shares_entry
        self.price_entry = price_entry

        shares_label.grid(row=3, column=0, sticky=W, padx=10, pady=5)
        shares_entry.grid(row=3, column=1, padx=10, pady=5)

        price_label.grid(row=4, column=0, sticky=W, padx=10, pady=5)
        price_entry.grid(row=4, column=1, padx=10, pady=5)

        submit_button.grid(row=6, columnspan=2, pady=10)

    def edit_order(self):
        shares = self.shares_entry.get()
        price = self.price_entry.get()
        order = self.master.database_manager.edit_order(
            self.master.username, self.cl_ord_id, shares, price
        )
        order = QuickfixOrder(
            order_id=order.order_id,
            client_order_id=order.cl_ord_id,
            symbol=order.symbol,
            side=SideEnum.BUY if order.side == "BUY" else SideEnum.SELL,
            type=OrderTypeEnum.LIMIT if order.type == "LIMIT" else OrderTypeEnum.STOP,
            price=order.price,
            quantity=order.quantity,
        )
        replaced_order = OrderCancelReplaceRequest.new_replace_order(
            self.cl_ord_id, order, float(price), int(shares)
        )
        self.master.application.send(replaced_order)

        message = f"Edited order {self.cl_ord_id}"
        self.master.display_message(message)
        self.master.refresh_main_interface()
        self.destroy()
