import datetime
import tkinter as tk
import uuid
from tkinter import scrolledtext, ttk

from broker_quickfix_client.handlers.execution_report import ExecutionReportHandler
from broker_quickfix_client.handlers.market_data_request_reject import (
    MarketDataRequestRejectHandler,
)
from broker_quickfix_client.handlers.market_data_snapshot_full_refresh import (
    MarketDataSnapshotFullRefreshHandler,
)
from broker_quickfix_client.handlers.order_cancel_reject import OrderCancelRejectHandler
from broker_quickfix_client.wrappers.enums import (
    MarketDataEntryTypeEnum,
    OrderTypeEnum,
    SideEnum,
)
from broker_quickfix_client.wrappers.market_data_request import MarketDataRequest
from broker_quickfix_client.wrappers.order import Order as QuickfixOrder
from broker_quickfix_client.wrappers.order_cancel_request import OrderCancelRequest
from PIL import Image, ImageTk

from quickfix_client_gui.db.database_manager import DatabaseManager
from quickfix_client_gui.interface.account_window import AccountWindow
from quickfix_client_gui.interface.edit_order_window import EditOrderWindow
from quickfix_client_gui.interface.order_window import OrderWindow


class MainInterface(tk.Tk):
    def __init__(self, username, application, initiator):
        # def __init__(self,username):
        super().__init__()

        # Set up window dimensions and position
        window_width, window_height = 800, 900
        screen_width, screen_height = (
            self.winfo_screenwidth(),
            self.winfo_screenheight(),
        )
        center_x, center_y = int((screen_width - window_width) / 2), int(
            (screen_height - window_height) / 2
        )
        self.geometry(f"{window_width}x{window_height}+{center_x}+{center_y}")
        self.wm_title("Client interface")

        # Initialize class attributes
        self.database_manager = DatabaseManager("sqlite:///quickfix_client_database.db")
        self.database_manager.set_refresh_callback(self.refresh_main_interface)
        self.database_manager.set_display_message_callback(self.display_message)
        self.username = username
        self.account_balance = self.database_manager.get_user_balance(self.username)
        self.owned_shares = self.database_manager.get_user_shares(self.username)
        self.application, self.initiator = application, initiator

        execution_report_handler = ExecutionReportHandler(
            order_filled_callback=self.database_manager.order_filled_callback,
            order_rejected_callback=self.database_manager.order_rejected_callback,
            order_accepted_callback=self.database_manager.order_accepted_callback,
            order_canceled_callback=self.database_manager.order_canceled_callback,
            order_replaced_callback=self.database_manager.order_replaced_callback,
        )

        order_cancel_reject_handler = OrderCancelRejectHandler(
            order_cancel_rejected_callback=self.database_manager.order_cancel_rejected_callback,
            # order_cancel_replace_rejected_callback=self.database_manager.order_cancel_replace_rejected_callback,
        )

        market_data_handler = MarketDataSnapshotFullRefreshHandler(
            market_data_snapshot_full_refresh_callback=self.database_manager.market_data_snapshot_full_refresh_callback,
        )
        market_data_request_reject_handler = MarketDataRequestRejectHandler(
            market_data_request_reject_callback=self.database_manager.market_data_request_reject_callback,
        )
        self.application.set_execution_report_handler(execution_report_handler)
        self.application.set_order_cancel_reject_handler(order_cancel_reject_handler)
        self.application.set_market_data_snapshot_full_refresh_handler(
            market_data_handler
        )
        self.application.set_market_data_request_reject_handler(
            market_data_request_reject_handler
        )

        # Create main widgets
        self.create_widgets()

        # Handle window deletion
        self.protocol("WM_DELETE_WINDOW", self.on_close)

    def create_widgets(self):
        self.create_account_info_widgets()
        self.create_message_display()
        self.create_market_data_widgets()
        self.create_image_widget()
        self.create_order_buttons()
        self.create_order_treeview()

    def create_account_info_widgets(self):
        # Display Account Name
        account_name_label = tk.Label(self, text=f"Username:{self.username}")
        account_name_label.grid(row=0, column=0, padx=(10, 0), pady=(5, 0), sticky=tk.E)

        # Display Account Balance
        self.account_balance_label = tk.Label(
            self, text=f"Account Balance:{self.account_balance}"
        )
        self.account_balance_label.grid(
            row=0, column=1, padx=(10, 0), pady=(5, 0), sticky=tk.E
        )

        # Display Owned Shares Table
        order_button = tk.Button(
            self, text="View owned shares", command=lambda: AccountWindow()
        )
        order_button.grid(row=0, column=2, pady=(5, 0), padx=(10, 0), sticky=tk.E)

    def create_market_data_widgets(self):
        refresh_button = tk.Button(
            self, text="Refresh", command=lambda: self.refresh_main_interface()
        )
        refresh_button.grid(row=1, column=3, pady=(10, 0), padx=(0, 10))

        symbol_label = tk.Label(self, text="Symbols:")
        self.symbol_entry = tk.Entry(self)
        symbol_label.grid(row=1, column=1, pady=(10, 0), sticky=tk.E)
        self.symbol_entry.grid(row=1, column=2, pady=(10, 0), padx=(0, 10))

    def create_image_widget(self):
        image_path = "quickfix_client_gui/charts/placeholder.png"

        chart_image = Image.open(image_path)
        chart_image = chart_image.resize((450, 300), Image.LANCZOS)
        self.chart_image = ImageTk.PhotoImage(chart_image)
        chart_label = tk.Label(self, image=self.chart_image)
        chart_label.grid(row=2, column=0, columnspan=3, pady=(10, 0), padx=(10, 0))

    def create_order_buttons(self):
        order_button = tk.Button(
            self, text="Place order", command=lambda: OrderWindow(self)
        )
        order_button.grid(row=3, column=0, pady=(10, 0), padx=(10, 0))

        cancel_order_button = tk.Button(
            self, text="Cancel Order", command=lambda: self.cancel_order()
        )
        cancel_order_button.grid(row=3, column=1, pady=(10, 0))

        edit_order_button = tk.Button(
            self, text="Edit Order", command=lambda: self.edit_order()
        )
        edit_order_button.grid(row=3, column=2, pady=(10, 0))

    def create_order_treeview(self):
        order_columns = (
            "Order Id",
            "Side",
            "Order Type",
            "Symbol",
            "Quantity",
            "Price",
            "Status",
        )
        self.order_tree = ttk.Treeview(self, columns=order_columns, show="headings")
        for col in order_columns:
            self.order_tree.heading(col, text=col)
            self.order_tree.column(col, width=100, anchor=tk.CENTER)
        self.order_tree.grid(row=4, column=0, columnspan=3, pady=(10, 0), padx=(10, 0))
        self.refresh_main_interface()

    def refresh_main_interface(self):
        # Refresh balance value
        self.account_balance = self.database_manager.get_user_balance(self.username)
        self.account_balance_label.config(
            text=f"Account Balance: {self.account_balance}"
        )
        # Refresh owned shares
        self.owned_shares = self.database_manager.get_user_shares(self.username)

        # Clear existing items in the Treeview
        for item in self.order_tree.get_children():
            self.order_tree.delete(item)

        # Fetch the latest order data from the database
        orders = self.database_manager.get_all_orders(self.username)
        # Insert the orders into the Treeview
        if orders:
            for order in orders:
                self.order_tree.insert(
                    "",
                    "end",
                    values=(
                        order.cl_ord_id,
                        order.side,
                        order.type,
                        order.symbol,
                        order.quantity,
                        order.price,
                        order.status,
                    ),
                )

        # Refresh the chart
        symbol = self.symbol_entry.get()
        if symbol != "":
            timestamp = datetime.datetime.now().strftime("%Y%m%d%H%M")
            market_data_request_snapshot = MarketDataRequest.new_snapshot_request(
                str(uuid.uuid4().hex),
                f"{self.username}{timestamp}",
                [symbol],
                [
                    MarketDataEntryTypeEnum.OPEN,
                    MarketDataEntryTypeEnum.CLOSE,
                    MarketDataEntryTypeEnum.HIGH,
                    MarketDataEntryTypeEnum.LOW,
                ],
            )
            self.application.send(market_data_request_snapshot)
            message = f"Snapshot for symbols {symbol}"
        else:
            message = "Please select a symbol"
        self.display_message(message)
        return

    def create_message_display(self):
        message_frame = tk.Frame(self, bg="lightgray", height=10)
        message_frame.grid(
            row=5,
            column=0,
            columnspan=3,
            pady=(10, 0),
            padx=(10, 0),
            sticky=tk.W + tk.E + tk.S + tk.N,
        )

        self.message_text = scrolledtext.ScrolledText(
            message_frame, wrap=tk.WORD, bg="lightgray", height=10
        )
        self.message_text.pack(expand=True, fill=tk.BOTH)

    def display_message(self, message):
        self.message_text.config(state="normal")
        self.message_text.insert(tk.END, message + "\n")
        self.message_text.config(state="disabled")
        self.message_text.see(tk.END)

    def cancel_order(self):
        # Cancel selected order
        selected_item_id = self.order_tree.selection()
        if selected_item_id:
            cl_ord_id = self.order_tree.item(selected_item_id, "values")[0]
            self.display_message(f"Cancelling order {cl_ord_id}")
            order = self.database_manager.cancel_order(cl_ord_id)
            order = QuickfixOrder(
                order_id=order.order_id,
                client_order_id=order.cl_ord_id,
                symbol=order.symbol,
                side=SideEnum.BUY if order.side == "BUY" else SideEnum.SELL,
                type=(
                    OrderTypeEnum.LIMIT if order.type == "LIMIT" else OrderTypeEnum.STOP
                ),
                price=order.price,
                quantity=order.quantity,
            )
            print(order)
            canceled_order = OrderCancelRequest.new_cancel_order(cl_ord_id, order)
            self.application.send(canceled_order)
            self.refresh_main_interface()
        else:
            message = "Please select an order to cancel."
            self.display_message(message)

    def edit_order(self):
        # Edit selected order
        selected_item_id = self.order_tree.selection()
        if selected_item_id:
            current_values = self.order_tree.item(selected_item_id, "values")
            cl_ord_id = current_values[0]
            order = self.database_manager.get_order(self.username, cl_ord_id)
            if order and order.order_id:
                self.order_tree.set(selected_item_id, "Status", "Editing")
                EditOrderWindow(self, selected_item_id)
        else:
            message = "Please select an order to Edit."
            self.display_message(message)

        pass

    def on_close(self):
        self.initiator.stop()
        self.destroy()
