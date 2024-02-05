import tkinter as tk
from tkinter import ttk
import tkinter as tk
from tkinter import Toplevel

class AccountWindow(Toplevel):
    def __init__(self):
        super().__init__()

        self.title("Owned Shares Table")
        # Define the columns
        owned_shares_columns = ("Share_name", "quantity")

        # Create a Treeview widget
        owned_shares_tree = ttk.Treeview(self, columns=owned_shares_columns, show="headings")

        # Configure column headings
        for col in owned_shares_columns:
            owned_shares_tree.heading(col, text=col)
            owned_shares_tree.column(col, width=100, anchor=tk.CENTER)

        # Insert data into the Treeview
        if self.master.owned_shares:
            for share_name, quantity in self.master.owned_shares.items():
                owned_shares_tree.insert("", "end", values=(share_name, quantity))

        # Pack the Treeview
        owned_shares_tree.pack(padx=10, pady=10, fill=tk.BOTH, expand=True)
