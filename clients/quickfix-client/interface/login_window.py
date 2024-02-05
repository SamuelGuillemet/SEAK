from tkinter import *
from tkinter.ttk import *
from interface.main_interface import MainInterface
import tkinter as tk
from tkinter import messagebox
from broker_quickfix_client.application import setup, start_initiator
from time import sleep
from threading import Thread

class LoginWindow(tk.Tk):
    def __init__(self):
        super().__init__()
        window_width, window_height = 300, 150
        screen_width, screen_height = self.winfo_screenwidth(), self.winfo_screenheight()
        center_x, center_y = int((screen_width - window_width) / 2), int((screen_height - window_height) / 2)
        self.geometry(f'{window_width}x{window_height}+{center_x}+{center_y}')
        self.wm_title("Login")

        username_label = Label(self, text="Username:")
        username_entry = Entry(self)
        password_label = Label(self, text="Password:")
        password_entry = Entry(self, show="*")

        self.username_entry = username_entry
        self.password_entry = password_entry
        # Login button
        login_button = Button(self, text="Login", command=self.validate_login)

        # Arrange widgets using grid layout
        username_label.grid(row=0, column=0, pady=10, padx=10)
        username_entry.grid(row=0, column=1, pady=10, padx=10)
        password_label.grid(row=1, column=0, pady=10, padx=10)
        password_entry.grid(row=1, column=1, pady=10, padx=10)
        login_button.grid(row=2, column=0, columnspan=2, pady=10, padx=10)
        self.protocol("WM_DELETE_WINDOW", self.on_close)

    def validate_login(self):
        username = self.username_entry.get()
        password = self.password_entry.get()
        if username == "user1" and password == "password":
            self.destroy()
            application, initiator = setup(username, password)
            Thread(target=start_initiator, args=(initiator,application)).start()
            MainInterface(username, application, initiator)
            return True
        else:
            # Attempt logon
            
            application, initiator = setup(username, password)
            Thread(target=start_initiator, args=(initiator,application)).start()
            sleep(3)
            # Check if logon was successful
            if application.get_session_id():
                self.destroy()
                MainInterface(username,application,initiator) # add those to the init
                return True
            else:
                messagebox.showerror("Error","Wrong username or password")
                return False
    def on_close(self):
        #self.initiator.stop()
        self.destroy()

if __name__ == "__main__":
    app = LoginWindow()
    app.mainloop()