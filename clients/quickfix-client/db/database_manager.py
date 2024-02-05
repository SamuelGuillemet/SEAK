from sqlalchemy import create_engine, Column, Integer, String, ForeignKey, Float
from sqlalchemy.orm import declarative_base, sessionmaker, relationship
from broker_quickfix_client.wrappers.execution_report import (
    FilledExecutionReport,
    RejectedExecutionReport,
    AcceptedOrderExecutionReport,
    ReplacedOrderExecutionReport,
    CanceledOrderExecutionReport,
)
from broker_quickfix_client.wrappers.order_cancel_reject import  OrderCancelReject

Base = declarative_base()

class Share(Base):
    __tablename__ = 'shares'
    id = Column(Integer, primary_key=True)
    owner_id = Column(Integer, ForeignKey('users.id'))
    symbol = Column(String)
    quantity = Column(Integer)
    
    # many-to-one relationship to the User table
    owner = relationship("User", back_populates="shares")

class User(Base):
    __tablename__ = 'users'
    id = Column(Integer, primary_key=True)
    username = Column(String, unique=True)
    balance = Column(Integer)
    
    # one-to-many relationship with the Share table
    shares = relationship("Share", back_populates="owner")
    orders = relationship("Order", back_populates="user")
    
class Order(Base):
    __tablename__ = 'orders'
    cl_ord_id = Column(Integer, primary_key=True)
    order_id = Column(Integer)
    symbol = Column(String)
    side = Column(String)
    type = Column(String)
    price = Column(Float)
    quantity = Column(Integer)
    status = Column(String)
    user_id = Column(Integer, ForeignKey('users.id'))
    

    # many-to-one relationship to the User table
    user = relationship("User", back_populates="orders")

class DatabaseManager:
    def __init__(self, db_url):
        self.engine = create_engine(db_url)
        Base.metadata.create_all(self.engine)
        Session = sessionmaker(bind=self.engine)
        self.session = Session()
        self.init_example_data()
        
    def set_refresh_callback(self, callback):
        self.refresh_callback = callback

    def init_example_data(self):
        user1 = self.session.query(User).filter_by(username="user1").first()

        if not user1:
            user1 = User(username="user1", balance=1000)
            self.session.add(user1)
            self.session.commit()

            # Add example shares
            share1 = Share(owner=user1, symbol="AAPL", quantity=100)
            share2 = Share(owner=user1, symbol="GOOGL", quantity=200)
            self.session.add_all([share1, share2])
            self.session.commit()
            
            # Add example orders
            order1 = Order(order_id=1, user=user1, side="Buy", type="LIMIT", symbol="AAPL", quantity=50, price=150, status="Pending")
            order2 = Order(order_id=2, user=user1, side="Sell", type="MARKET", symbol="GOOGL", quantity=20, price=200, status="Filled")

            self.session.add_all([order1, order2])
            self.session.commit()

    def get_user_balance(self, username):
        user = self.session.query(User).filter_by(username=username).first()

        if user:
            balance = user.balance
            return balance
        else:
            return 0

    def get_user_shares(self, username):
        user = self.session.query(User).filter_by(username=username).first()

        if user:
            owned_shares = {share.symbol: share.quantity for share in user.shares}
            return owned_shares
        else:
            return None
        
    def place_order(self, side, type, symbol, price, username, quantity):
        user = self.session.query(User).filter_by(username=username).first()

        if user:
            # Create a new order
            new_order = Order(
                side=side,
                type=type,
                symbol=symbol,
                quantity=quantity,
                price=price,
                status="Filled" if type == "MARKET" else "Pending",
                user=user
            )

            # Save the order to the database
            self.session.add(new_order)
            self.session.commit()

            # Get the client ID of the created order
            cl_ord_id = new_order.cl_ord_id

            if side == "BUY":
                # Buy logic
                if type == "MARKET":
                    # Update shares owned by the user for market order
                    user_shares = self.session.query(Share).filter_by(owner=user, symbol=symbol).first()

                    if user_shares:
                        user_shares.quantity += quantity
                    else:
                        new_share = Share(owner=user, symbol=symbol, quantity=quantity)
                        self.session.add(new_share)

                    # Update user's balance
                    user.balance -= price*quantity
                else:
                    # For non-market orders, buy logic remains the same
                    share = self.session.query(Share).filter_by(owner=user, symbol=symbol).first()

                    if share:
                        share.quantity += quantity
                    else:
                        new_share = Share(owner=user, symbol=symbol, quantity=quantity)
                        self.session.add(new_share)

                    user.balance -= price*quantity

                # Update order status based on order type
                new_order.status = "Filled" if type == "MARKET" else "Pending"
                self.session.commit()
                return cl_ord_id

            elif side == "SELL":
                # Sell logic
                share = self.session.query(Share).filter_by(owner=user, symbol=symbol).first()

                if share and share.quantity >= quantity:
                    share.quantity -= quantity
                    user.balance += price*quantity

                    # Update order status based on order type
                    new_order.status = "Filled" if type == "MARKET" else "Pending"
                    self.session.commit()
                    return cl_ord_id
                else:
                    # Rollback the order if the user doesn't have enough shares to sell
                    self.session.delete(new_order)
                    self.session.commit()
                    return None

            else:
                # Invalid order side
                self.session.delete(new_order)
                self.session.commit()
                return None

        else:
            # User not found
            return None



    def cancel_order(self, cl_ord_id):
        order = self.session.query(Order).filter_by(cl_ord_id=cl_ord_id).first()

        if order and order.status == "Pending":
            order.status = "Cancelling"
            self.session.commit()
            return order
        else:
            print("Cannot cancel order")
            return order

    def edit_order(self, username, cl_ord_id, shares_quantity, price):
        user = self.session.query(User).filter_by(username=username).first()
        order = self.session.query(Order).filter_by(cl_ord_id=cl_ord_id, user=user).first()

        if user and order and order.status == "Pending":
            order.quantity = shares_quantity
            order.price = price
            self.session.commit()
            return order
        else:
            print("Cannot edit order")
            return order 
        
    def get_order(self, username,cl_ord_id):
        user = self.session.query(User).filter_by(username=username).first()
        order = self.session.query(Order).filter_by(cl_ord_id=cl_ord_id, user=user).first()

        if user and order:
            return order
        else:
            print("Order not found")
            return order 
    
    def get_all_orders(self, username):
        user = self.session.query(User).filter_by(username=username).first()

        if user:
            orders = self.session.query(Order).filter_by(user_id=user.id).all()
            return orders
        else:
            return None

    
    def order_filled_callback(self, execution_report: FilledExecutionReport):
        client_order_id = execution_report.client_order_id
        order = self.session.query(Order).filter_by(cl_ord_id=client_order_id).first()
        if order:
            order.status = "Filled"
            self.session.commit()
            print(f"Order {client_order_id} filled successfully.")
            if self.refresh_callback:
                self.refresh_callback()
        else:
            print(f"Order {client_order_id} not found in the database.")
            
    def order_rejected_callback(self, report: RejectedExecutionReport):
        client_order_id = report.client_order_id
        order = self.session.query(Order).filter_by(cl_ord_id=client_order_id).first()
        if order:
            order.status = "Rejected"
            user = order.user
            # Rollback
            if order.side == "BUY":
                user.balance += order.price * order.quantity
                share = self.session.query(Share).filter_by(owner=user, symbol=order.symbol).first()
                share.quantity -= order.quantity
            elif order.side == "SELL":
                user.balance -= order.price * order.quantity
                share = self.session.query(Share).filter_by(owner=user, symbol=order.symbol).first()
                share.quantity += order.quantity
            self.session.commit()
            if self.refresh_callback:
                self.refresh_callback()
            print(f"Order {client_order_id} rejected.")
        else:
            print(f"Order {client_order_id} not found in the database.")

    def order_accepted_callback(self, report: AcceptedOrderExecutionReport):
        client_order_id = report.client_order_id
        order = self.session.query(Order).filter_by(cl_ord_id=client_order_id).first()
        if order:
            order.status = "Accepted"
            self.session.commit()
            print(f"Order {client_order_id} accepted.")
            if self.refresh_callback:
                self.refresh_callback()
        else:
            print(f"Order {client_order_id} not found in the database.")

    def order_canceled_callback(self, report: CanceledOrderExecutionReport):
        client_order_id = report.client_order_id
        order = self.session.query(Order).filter_by(cl_ord_id=client_order_id).first()
        if order:
            order.status = "Canceled"
            self.session.commit()
            print(f"Order {client_order_id} canceled.")
            if self.refresh_callback:
                self.refresh_callback()
        else:
            print(f"Order {client_order_id} not found in the database.")

    def order_replaced_callback(self, report: ReplacedOrderExecutionReport):
        client_order_id = report.client_order_id
        order = self.session.query(Order).filter_by(cl_ord_id=client_order_id).first()
        if order:
            order.status = "Replaced"
            self.session.commit()
            print(f"Order {client_order_id} replaced.")
            if self.refresh_callback:
                self.refresh_callback()
        else:
            print(f"Order {client_order_id} not found in the database.")

    def order_cancel_rejected_callback(self, reject: OrderCancelReject):
        client_order_id = reject.client_order_id
        order = self.session.query(Order).filter_by(cl_ord_id=client_order_id).first()
        if order:
            order.status = "Pending"
            print(f"Order cancel request for {client_order_id} rejected.")
            if self.refresh_callback:
                self.refresh_callback()
        else:
            print(f"Order {client_order_id} not found in the database.")

    # def order_cancel_replace_rejected_callback(self, report: ):
    #     return

    # def market_data_snapshot_full_refresh_callback(
    #     market_data_response: MarketDataResponse,
    # ):
    #     create_candlestick_chart(market_data_response)
        # if self.refresh_callback:
        #             self.refresh_callback()
    #     return