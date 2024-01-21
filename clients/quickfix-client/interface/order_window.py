import tkinter as tk
from tkinter import *
from tkinter.ttk import *
from tkinter import messagebox
#from broker_quickfix_client.wrappers.new_order_single import NewOrderSingle
#from broker_quickfix_client.wrappers.enums import *
class orderWindow(Toplevel):
    def __init__(self, master):
        super().__init__(master=master)

        # WINDOW SETUP
        window_width = 500
        window_height = 300
        screen_width = self.winfo_screenwidth()
        screen_height = self.winfo_screenheight()
        center_x = int(screen_width / 2 - window_width / 2)
        center_y = int(screen_height / 2 - window_height / 2)
        self.geometry(f'{window_width}x{window_height}+{center_x}+{center_y}')
        self.wm_title("Order")

        # Variables for Radiobuttons
        self.side_var = tk.StringVar()
        self.order_type_var = tk.StringVar()

        # Widgets
        order_type_label = Label(self, text="Order Type:")
        order_type_frame = Frame(self)
        for order_type in ["MARKET", "LIMIT", "STOP"]:
            Radiobutton(order_type_frame, text=order_type, variable=self.order_type_var, value=order_type).pack(side=LEFT)

        side_label = Label(self, text="Side:")
        side_frame = Frame(self)
        for side in ["BUY", "SELL"]:
            Radiobutton(side_frame, text=side, variable=self.side_var, value=side).pack(side=LEFT)

        symbol_label = Label(self, text="Symbol:")
        symbol_entry = Entry(self)

        shares_label = Label(self, text="Number of Shares:")
        shares_entry = Entry(self)

        price_label = Label(self, text="Price:")
        price_entry = Entry(self)

        submit_button = Button(self, text="Submit Order", command=self.submit_order)

        # Grid layout
        order_type_label.grid(row=0, column=0, sticky=W, padx=10, pady=5)
        order_type_frame.grid(row=0, column=1, padx=10, pady=5)

        side_label.grid(row=1, column=0, sticky=W, padx=10, pady=5)
        side_frame.grid(row=1, column=1, padx=10, pady=5)

        symbol_label.grid(row=2, column=0, sticky=W, padx=10, pady=5)
        symbol_entry.grid(row=2, column=1, padx=10, pady=5)

        shares_label.grid(row=3, column=0, sticky=W, padx=10, pady=5)
        shares_entry.grid(row=3, column=1, padx=10, pady=5)

        price_label.grid(row=4, column=0, sticky=W, padx=10, pady=5)
        price_entry.grid(row=4, column=1, padx=10, pady=5)

        submit_button.grid(row=6, columnspan=2, pady=10)

        # Set default values for Radiobuttons
        self.order_type_var.set("MARKET")
        self.side_var.set("BUY")

        self.symbol_entry = symbol_entry
        self.shares_entry = shares_entry
        self.price_entry = price_entry
    
    def submit_order(self):
        side = self.side_var.get()
        order_type = self.order_type_var.get()
        symbol = self.symbol_entry.get()
        shares = int(self.shares_entry.get())
        if self.price_entry.get() != "":
            price = float(self.price_entry.get())
        else:
            price = 10
            #price = request_market_data_snapshot([symbol])["Price"]
        
        total_order_price = price*shares
        if self.master.owned_shares and symbol in self.master.owned_shares:
            owned_quantity_shares = self.master.owned_shares[symbol]
        else:
            owned_quantity_shares = 0

        if shares==0 or order_type=="" or symbol=="" or side=="":
            error_message = "Please fill in the fields."
            messagebox.showerror("Error",error_message)
        elif side=="BUY" and total_order_price > self.master.account_balance:
            error_message = "Insufficient funds."
            messagebox.showerror("Error",error_message)
        elif side=="SELL" and shares > owned_quantity_shares:
            error_message = "Insufficient shares."
            messagebox.showerror("Error",error_message)
        elif order_type == "LIMIT" and price!=0:
            # Update the local DB
            cl_ord_id = self.master.database_manager.place_order(side, order_type, symbol, price, self.master.username, shares)
            # Send the order to the broker
            # order = NewOrderSingle.new_limit_order(cl_ord_id, SideEnum.BUY if side=="BUY" else SideEnum.SELL , shares, symbol, price)
            # self.master.application.send(order)
            message = f"Placed {order_type} {side} order for {shares} shares of {symbol} at Price {price}"
            self.master.display_message(message)
            self.master.refresh_main_interface()
            self.destroy()
        elif order_type == "STOP" and price!=0:
            # Update the local DB
            cl_ord_id = self.master.database_manager.place_order(side, order_type, symbol, price, self.master.username, shares)
            # Send the order to the broker
            # order = NewOrderSingle.new_stop_order(cl_ord_id, SideEnum.BUY if side=="BUY" else SideEnum.SELL , shares, symbol, price)
            # self.master.application.send(order)
            message = f"Placed {order_type} {side} order for {shares} shares of {symbol} at Price {price}"
            self.master.display_message(message)
            self.master.refresh_main_interface()
            self.destroy()
        elif order_type == "MARKET" and shares!=0:
            # Update the local DB
            cl_ord_id = self.master.database_manager.place_order(side, order_type, symbol, price, self.master.username, shares)
            # order = NewOrderSingle.new_market_order(cl_ord_id, SideEnum.BUY if side=="BUY" else SideEnum.SELL , shares, symbol)
            # self.master.application.send(order)
            self.master.refresh_main_interface()
            message = f"Placed {order_type} {side} order for {shares} shares of {symbol}"
            self.master.display_message(message)
            self.destroy()

        else:
            error_message = "Invalid order."
            messagebox.showerror("Error",error_message)

        
       
