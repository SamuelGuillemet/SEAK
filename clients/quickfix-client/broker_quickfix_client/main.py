import argparse
import faulthandler
import logging
from time import sleep

from quickfix import FileStoreFactory, SessionSettings, SocketInitiator

from broker_quickfix_client.application import ClientApplication
from broker_quickfix_client.handlers.execution_report import ExecutionReportHandler
from broker_quickfix_client.utils.loader import get_config_dir
from broker_quickfix_client.utils.logger import setup_logs
from broker_quickfix_client.wrappers.enums import SideEnum
from broker_quickfix_client.wrappers.new_order_single import NewOrderSingle

logger = logging.getLogger("client")

faulthandler.enable()


def build_initiator(
    config_path: str, application: ClientApplication
) -> SocketInitiator:
    settings = SessionSettings(config_path)
    store_factory = FileStoreFactory(settings)
    initiator = SocketInitiator(application, store_factory, settings)
    return initiator


def start_initiator(initiator: SocketInitiator, application: ClientApplication):
    initiator.start()
    # Wait for the session to logon before sending messages.
    while not application.get_session_id():
        sleep(0.1)


def setup():
    setup_logs("client")
    setup_logs("quickfix")
    application = ClientApplication()
    initiator = build_initiator(str(get_config_dir() / "client.cfg"), application)
    start_initiator(initiator, application)
    return application, initiator


def main():
    execution_handler = ExecutionReportHandler(
        on_filled_report=lambda report: logger.info(f"Filled: {report}"),
        on_rejected_report=lambda report: logger.info(f"Rejected: {report}"),
    )
    application, initiator = setup()
    application.set_execution_report_handler(execution_handler)

    order = NewOrderSingle.new_market_order(0, SideEnum.SELL, 1, "ACGL")
    order.get_order()
    application.send(order)
    sleep(1000)

    initiator.stop()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Customize parameters")

    args = parser.parse_args()

    main()
