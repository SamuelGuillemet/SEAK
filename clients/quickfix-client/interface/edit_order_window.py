import tkinter as tk
from tkinter import *
from tkinter.ttk import *
import tkinter.font
# from broker_quickfix_client.wrappers.order import Order
 
class editOrderWindow(Toplevel):
     
    def __init__(self, master, item_id):
        super().__init__(master = master)
        #####   WINDOW SETUP    #####
        self.item_id = item_id
        window_width = 500
        window_height = 300

        screen_width = self.winfo_screenwidth()
        screen_height = self.winfo_screenheight()

        center_x = int(screen_width/2 - window_width / 2)
        center_y = int(screen_height/2 - window_height / 2)

        self.geometry(f'{window_width}x{window_height}+{center_x}+{center_y}')

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
        total_price = float(price)*float(shares)
        self.master.order_tree.item(self.item_id)
        current_values = self.master.order_tree.item(self.item_id, 'values')
        cl_ord_id = current_values[0]
        order = self.master.database_manager.edit_order(self.master.username, cl_ord_id, shares, price)
        # order = Order(order_id=order.order_id,
        # client_order_id=order.cl_ord_id,
        # symbol=order.symbol,
        # side=SideEnum.BUY if order.side=="BUY" else SideEnum.SELL,
        # type=OrdTypeEnum.LIMIT if order.type=="LIMIT" else OrdTypeEnum.STOP,
        # price=order.price,
        # quantity=order.quantity,
        # )
        # replaced_order = OrderCancelReplaceRequest.new_replace_order(cl_ord_id, order, price, shares)
        # self.master.application.send(replaced_order)
        
        message = f"Edited order {cl_ord_id} from {current_values[4]} shares at price {float(current_values[5])/float(current_values[4])} to {shares} shares at price {price} "
        self.master.display_message(message)
        self.master.refresh_main_interface()
        self.destroy()
        

        
       
