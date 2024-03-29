from broker_quickfix_client.wrappers.execution_report import (
    AcceptedOrderExecutionReport,
    CanceledOrderExecutionReport,
    FilledExecutionReport,
    RejectedExecutionReport,
    ReplacedOrderExecutionReport,
)
from broker_quickfix_client.wrappers.market_data import MarketDataResponse
from broker_quickfix_client.wrappers.order_cancel_reject import OrderCancelReject
from sqlalchemy import Column, Float, ForeignKey, Integer, String, create_engine
from sqlalchemy.orm import declarative_base, relationship, sessionmaker

from quickfix_client_gui.interface.candlestick_chart import create_candlestick_chart

Base = declarative_base()


class Share(Base):
    __tablename__ = "shares"
    id = Column(Integer, primary_key=True)
    owner_id = Column(Integer, ForeignKey("users.id"))
    symbol = Column(String)
    quantity = Column(Integer)

    # many-to-one relationship to the User table
    owner = relationship("User", back_populates="shares")


class User(Base):
    __tablename__ = "users"
    id = Column(Integer, primary_key=True)
    username = Column(String, unique=True)
    balance = Column(Integer)

    # one-to-many relationship with the Share table
    shares = relationship("Share", back_populates="owner")
    orders = relationship("Order", back_populates="user")


class Order(Base):
    __tablename__ = "orders"
    cl_ord_id = Column(Integer, primary_key=True)
    order_id = Column(Integer)
    symbol = Column(String)
    side = Column(String)
    type = Column(String)
    price = Column(Float)
    quantity = Column(Integer)
    status = Column(String)
    user_id = Column(Integer, ForeignKey("users.id"))

    # many-to-one relationship to the User table
    user = relationship("User", back_populates="orders")


class DatabaseManager:
    def __init__(self, db_url):
        self.engine = create_engine(db_url)
        Base.metadata.create_all(self.engine)
        Session = sessionmaker(bind=self.engine)
        self.session = Session()
        self.init_example_data()

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
            order1 = Order(
                order_id=1,
                user=user1,
                side="Buy",
                type="LIMIT",
                symbol="AAPL",
                quantity=50,
                price=150,
                status="Pending",
            )
            order2 = Order(
                order_id=2,
                user=user1,
                side="Sell",
                type="MARKET",
                symbol="GOOGL",
                quantity=20,
                price=200,
                status="Filled",
            )

            self.session.add_all([order1, order2])
            self.session.commit()

    def create_user(self, username):
        user = self.session.query(User).filter_by(username=username).first()
        if user is None:
            user1 = User(username=username, balance=1000)
            self.session.add(user1)
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
                status="Pending",
                user=user,
            )

            # Save the order to the database
            self.session.add(new_order)
            self.session.commit()

            # Get the client ID of the created order
            cl_ord_id = new_order.cl_ord_id

            if type == "LIMIT":
                if side == "BUY":
                    # Vzerify if the user has enough balance
                    if user.balance < price * quantity:
                        # Rollback the order if the user doesn't have enough balance
                        self.session.delete(new_order)
                        self.session.commit()
                        return None
                elif side == "SELL":
                    # Verify if the user has enough shares
                    share = (
                        self.session.query(Share)
                        .filter_by(owner=user, symbol=symbol)
                        .first()
                    )
                    if not share or share.quantity < quantity:
                        # Rollback the order if the user doesn't have enough shares
                        self.session.delete(new_order)
                        self.session.commit()
                        return None

            elif type == "MARKET":
                if side == "BUY":
                    # Can't reduce balance, price is unknown
                    pass

                elif side == "SELL":
                    # Reduce user's shares
                    share = (
                        self.session.query(Share)
                        .filter_by(owner=user, symbol=symbol)
                        .first()
                    )

                    if not share or share.quantity <= quantity:
                        # Rollback the order if the user doesn't have enough shares to sell
                        self.session.delete(new_order)
                        self.session.commit()
                        return None

                    share.quantity -= quantity

                self.session.commit()

            return cl_ord_id

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
        order = (
            self.session.query(Order).filter_by(cl_ord_id=cl_ord_id, user=user).first()
        )

        if order and order.status == "Pending":
            order.status = "Editing"
            self.session.commit()

        return order

    def get_order(self, username, cl_ord_id):
        user = self.session.query(User).filter_by(username=username).first()
        order = (
            self.session.query(Order).filter_by(cl_ord_id=cl_ord_id, user=user).first()
        )

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
        if not order:
            print(f"Order {client_order_id} not found in the database.")

        order.status = "Filled"
        order.order_id = execution_report.order_id

        if order.type == "MARKET" and order.side == "BUY":
            # Remove money now that the order is filled
            order.user.balance -= execution_report.price * order.quantity
            order.price = execution_report.price

        if order.side == "BUY":
            # Add shares to the user
            share = (
                self.session.query(Share)
                .filter_by(owner=order.user, symbol=order.symbol)
                .first()
            )
            if share:
                share.quantity += order.quantity
            else:
                new_share = Share(
                    owner=order.user, symbol=order.symbol, quantity=order.quantity
                )
                self.session.add(new_share)
        elif order.side == "SELL":
            # Add money to the user
            order.user.balance += execution_report.price * order.quantity

        self.session.commit()
        message = f"Order {client_order_id} filled successfully."
        print(message)

    def order_rejected_callback(self, report: RejectedExecutionReport):
        client_order_id = report.client_order_id
        order = self.session.query(Order).filter_by(cl_ord_id=client_order_id).first()
        if order:
            order.status = "Rejected"
            order.order_id = report.order_id
            user = order.user

            if order.type == "MARKET" and order.side == "SELL":
                # Rollback
                share = (
                    self.session.query(Share)
                    .filter_by(owner=user, symbol=order.symbol)
                    .first()
                )
                share.quantity += order.quantity

            self.session.commit()
            message = f"Order {client_order_id} rejected."
            print(message)
        else:
            print(f"Order {client_order_id} not found in the database.")

    def order_accepted_callback(self, report: AcceptedOrderExecutionReport):
        client_order_id = report.client_order_id
        order = self.session.query(Order).filter_by(cl_ord_id=client_order_id).first()
        if order:
            order.status = "Accepted"
            order.order_id = report.order_id

            if order.type == "LIMIT":
                if order.side == "BUY":
                    # Reduce user's balance
                    order.user.balance -= order.price * order.quantity
                elif order.side == "SELL":
                    # Reduce user's shares
                    share = (
                        self.session.query(Share)
                        .filter_by(owner=order.user, symbol=order.symbol)
                        .first()
                    )
                    share.quantity -= order.quantity

            self.session.commit()
            message = f"Order {client_order_id} accepted."
            print(message)
        else:
            print(f"Order {client_order_id} not found in the database.")

    def order_canceled_callback(self, report: CanceledOrderExecutionReport):
        client_order_id = report.client_order_id
        order = (
            self.session.query(Order).filter_by(client_order_id=client_order_id).first()
        )
        if order:
            order.status = "Canceled"

            if order.type == "LIMIT":
                if order.side == "BUY":
                    # Add user's balance
                    order.user.balance += order.price * order.quantity
                elif order.side == "SELL":
                    # Add user's shares
                    share = (
                        self.session.query(Share)
                        .filter_by(owner=order.user, symbol=order.symbol)
                        .first()
                    )
                    share.quantity += order.quantity
            self.session.commit()
            message = f"Order {client_order_id} canceled."
            print(message)
        else:
            print(f"Order {client_order_id} not found in the database.")

    def order_replaced_callback(self, report: ReplacedOrderExecutionReport):
        client_order_id = report.client_order_id
        original_order = (
            self.session.query(Order).filter_by(client_order_id=client_order_id).first()
        )
        if original_order:
            original_order.status = "Replaced"

            if original_order.type == "LIMIT":
                if original_order.side == "BUY":
                    # Add user's balance
                    original_order.user.balance += (
                        original_order.price * original_order.quantity
                        - report.price * report.leaves_quantity
                    )
                elif original_order.side == "SELL":
                    # Add user's shares
                    share = (
                        self.session.query(Share)
                        .filter_by(
                            owner=original_order.user, symbol=original_order.symbol
                        )
                        .first()
                    )
                    share.quantity += original_order.quantity - report.leaves_quantity

            original_order.price = report.price
            original_order.quantity = report.leaves_quantity

            self.session.commit()
            message = f"Order {client_order_id} replaced."
            print(message)
        else:
            print(
                f"Order {client_order_id} not found in the database,"
                f"original order id {report.original_client_order_id}."
            )

    def order_cancel_rejected_callback(self, reject: OrderCancelReject):
        client_order_id = reject.client_order_id
        order = self.session.query(Order).filter_by(cl_ord_id=client_order_id).first()
        if order:
            order.status = "Pending"
            message = f"Order cancel request for {client_order_id} rejected."
            print(message)
        else:
            message = f"Order {client_order_id} not found in the database."
            print(message)

    # def order_cancel_replace_rejected_callback(self, report: ):
    #     return

    def market_data_request_reject_callback(
        self,
        market_data_response: MarketDataResponse,
    ):
        message = "Market data request rejected"
        print(message)

    def market_data_snapshot_full_refresh_callback(
        self,
        market_data_response: MarketDataResponse,
    ):
        create_candlestick_chart(market_data_response)
        message = "Market data snapshot received"
        print(message)
