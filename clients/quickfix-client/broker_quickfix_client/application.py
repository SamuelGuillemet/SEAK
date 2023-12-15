# pylint: disable=unused-argument,invalid-name,super-init-not-called

import logging

from quickfix import (
    Application,
    Message,
    MsgType,
    MsgType_ExecutionReport,
    MsgType_Logon,
    MsgType_MarketDataSnapshotFullRefresh,
    Password,
    Session,
    SessionID,
    Username,
)

from broker_quickfix_client.handlers.execution_report import ExecutionReportHandler
from broker_quickfix_client.utils.quickfix import log_quick_fix_message

logger = logging.getLogger("client.application")


class ClientApplication(Application):
    session_id: SessionID | None = None

    execution_report_handler = ExecutionReportHandler()

    username: str | None = "user1"
    password: str | None = "password"

    def set_execution_report_handler(
        self, execution_report_handler: ExecutionReportHandler
    ):
        self.execution_report_handler = execution_report_handler

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
