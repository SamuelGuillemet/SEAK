import argparse
import faulthandler
import logging
import time
from time import sleep

from quickfix import FileStoreFactory, SocketInitiator

from broker_quickfix_client.application import ClientApplication
from broker_quickfix_client.handlers.execution_report import ExecutionReportHandler
from broker_quickfix_client.utils.logger import setup_logs
from broker_quickfix_client.utils.quickfix import set_settings
from broker_quickfix_client.wrappers.enums import SideEnum
from broker_quickfix_client.wrappers.new_order_single import NewOrderSingle

logger = logging.getLogger("client")

faulthandler.enable()


def build_initiator(username: str, application: ClientApplication) -> SocketInitiator:
    settings = set_settings(username)
    store_factory = FileStoreFactory(settings)
    initiator = SocketInitiator(application, store_factory, settings)
    return initiator


def start_initiator(initiator: SocketInitiator, application: ClientApplication):
    initiator.start()
    # Wait for the session to logon before sending messages.
    while not application.get_session_id():
        sleep(0.1)


def setup(username: str):
    setup_logs("client")
    setup_logs("quickfix")
    application = ClientApplication()
    initiator = build_initiator(username, application)
    start_initiator(initiator, application)
    return application, initiator


def latency_test(username: str, application: ClientApplication):
    order_time_map: dict[int, list[float]] = {}
    cl_ord_id = 0
    nb_rejected = 0

    def on_filled_report(report):
        order_time_map[report.client_order_id].append(time.time())
        logger.info(f"Filled: {report}")

    def on_rejected_report(report):
        logger.warning(f"Rejected: {report}")
        nonlocal nb_rejected
        nb_rejected += 1

    execution_handler = ExecutionReportHandler(
        on_filled_report=on_filled_report,
        on_rejected_report=on_rejected_report,
    )
    application.set_execution_report_handler(execution_handler)

    nb_orders = 100

    for _ in range(nb_orders):
        order = NewOrderSingle.new_market_order(cl_ord_id, SideEnum.BUY, 1, "ACGL")
        application.send(order)
        order_time_map[cl_ord_id] = [time.time()]
        cl_ord_id += 1
        sleep(0.1)
    for _ in range(nb_orders):
        order = NewOrderSingle.new_market_order(cl_ord_id, SideEnum.SELL, 1, "ACGL")
        application.send(order)
        order_time_map[cl_ord_id] = [time.time()]
        cl_ord_id += 1
        sleep(0.1)

    sleep(5)

    # Print avg latency for each order
    sum_latency = 0.0
    latency_list: list[float] = []
    try:
        for _, order_time_array in order_time_map.items():
            if len(order_time_array) != 2:
                continue
            latency = order_time_array[1] - order_time_array[0]
            sum_latency += latency
            latency_list.append(latency)
    except Exception as e:
        print(e)
        print(order_time_map)
    # Save latency list to file
    with open(f"latency-{username}.txt", "w", encoding="utf-8") as f:
        for item in latency_list:
            f.write(f"{item}\n")
    print(f"Average latency: {sum_latency/len(latency_list)} seconds")


def limit_order_test(application: ClientApplication):
    order_time_map: dict[int, list[float]] = {}
    cl_ord_id = 0
    nb_rejected = 0

    def on_filled_report(report):
        order_time_map[report.client_order_id].append(time.time())
        logger.info(f"Filled: {report}")

    def on_rejected_report(report):
        logger.warning(f"Rejected: {report}")
        nonlocal nb_rejected
        nb_rejected += 1

    execution_handler = ExecutionReportHandler(
        on_filled_report=on_filled_report,
        on_rejected_report=on_rejected_report,
    )
    application.set_execution_report_handler(execution_handler)

    nb_orders = 10

    for _ in range(nb_orders):
        order = NewOrderSingle.new_limit_order(cl_ord_id, SideEnum.BUY, 1, "ACGL", 47.4)
        application.send(order)
        order_time_map[cl_ord_id] = [time.time()]
        cl_ord_id += 1
        sleep(3)

    sleep(15)
    for _ in range(nb_orders):
        order = NewOrderSingle.new_limit_order(
            cl_ord_id, SideEnum.SELL, 1, "ACGL", 47.5
        )
        application.send(order)
        order_time_map[cl_ord_id] = [time.time()]
        cl_ord_id += 1
        sleep(3)

    sleep(150)


def main(username: str):
    application, initiator = setup(username)
    latency_test(username, application)
    # limit_order_test(application)
    initiator.stop()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Customize parameters")

    parser.add_argument(
        "--username",
        type=str,
        default="user1",
        help="The username to use to connect to the server",
    )

    args = parser.parse_args()

    main(args.username)
