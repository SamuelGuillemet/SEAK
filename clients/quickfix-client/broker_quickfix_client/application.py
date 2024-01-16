# pylint: disable=unused-argument,invalid-name,super-init-not-called

import logging
from time import sleep

from quickfix import (
    Application,
    FileStoreFactory,
    Message,
    MsgType,
    MsgType_ExecutionReport,
    MsgType_Logon,
    MsgType_MarketDataSnapshotFullRefresh,
    MsgType_OrderCancelReject,
    Password,
    Session,
    SessionID,
    SocketInitiator,
    Username,
)

from broker_quickfix_client.handlers.execution_report import ExecutionReportHandler
from broker_quickfix_client.handlers.order_cancel_reject import OrderCancelRejectHandler
from broker_quickfix_client.utils.logger import setup_logs
from broker_quickfix_client.utils.quickfix import log_quick_fix_message, set_settings

logger = logging.getLogger("client.application")


class ClientApplication(Application):
    session_id: SessionID | None = None

    execution_report_handler = ExecutionReportHandler()
    order_cancel_reject_handler = OrderCancelRejectHandler()

    username: str | None = None
    password: str | None = None

    def set_execution_report_handler(
        self, execution_report_handler: ExecutionReportHandler
    ):
        self.execution_report_handler = execution_report_handler

    def set_order_cancel_reject_handler(
        self, order_cancel_reject_handler: OrderCancelRejectHandler
    ):
        self.order_cancel_reject_handler = order_cancel_reject_handler

    def onCreate(self, sessionId: SessionID):
        pass

    def onLogon(self, sessionId: SessionID):
        self.session_id = sessionId

    def onLogout(self, sessionId: SessionID):
        pass

    def toAdmin(self, message: Message, sessionId: SessionID):
        log_quick_fix_message(message, "Sending")
        if message.getHeader().getField(MsgType()).getString() == MsgType_Logon:
            message.setField(Username(self.username))
            message.setField(Password(self.password))

    def toApp(self, message: Message, sessionId: SessionID):
        log_quick_fix_message(message, "Sending", logging.INFO)

    def fromAdmin(self, message: Message, sessionId: SessionID):
        log_quick_fix_message(message, "Received")

    def fromApp(self, message: Message, sessionId: SessionID):
        log_quick_fix_message(message, "Received", logging.INFO)

        msg_type = message.getHeader().getField(MsgType()).getString()

        if msg_type == MsgType_ExecutionReport:
            self.execution_report_handler.handle_execution_report(message)
        elif msg_type == MsgType_OrderCancelReject:
            self.order_cancel_reject_handler.handle_order_cancel_reject(message)
        elif msg_type == MsgType_MarketDataSnapshotFullRefresh:
            logger.info("Market data snapshot full refresh received")
        else:
            logger.warning(f"Unknown message type: {msg_type}")

    def send(self, message: Message):
        return Session.sendToTarget(message, self.session_id)

    def get_session_id(self):
        return self.session_id

    def set_credentials(self, username, password):
        self.username = username
        self.password = password


def build_initiator(username: str, application: ClientApplication) -> SocketInitiator:
    settings = set_settings(username)
    store_factory = FileStoreFactory(settings)
    initiator = SocketInitiator(application, store_factory, settings)
    return initiator


def start_initiator(initiator: SocketInitiator, application: ClientApplication):
    if not application.username or not application.password:
        raise ValueError("Username and password must be set before starting initiator")

    initiator.start()
    # Wait for the session to logon before sending messages.
    while not application.get_session_id():
        sleep(0.1)


def setup(
    username: str,
    password: str,
    execution_report_handler: ExecutionReportHandler | None = None,
    order_cancel_reject_handler: OrderCancelRejectHandler | None = None,
):
    setup_logs("client")
    setup_logs("quickfix")
    application = ClientApplication()

    application.set_credentials(username, password)
    if execution_report_handler:
        application.set_execution_report_handler(execution_report_handler)
    if order_cancel_reject_handler:
        application.set_order_cancel_reject_handler(order_cancel_reject_handler)

    initiator = build_initiator(username, application)
    start_initiator(initiator, application)
    return application, initiator