import logging
import time
from time import sleep

from broker_quickfix_client.application import ClientApplication
from broker_quickfix_client.handlers.execution_report import ExecutionReportHandler
from broker_quickfix_client.wrappers.enums import SideEnum
from broker_quickfix_client.wrappers.new_order_single import NewOrderSingle

logger = logging.getLogger("client")


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
        order_filled_callback=on_filled_report,
        order_rejected_callback=on_rejected_report,
    )
    application.set_execution_report_handler(execution_handler)

    nb_orders = 20

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
